package com.cloud.zuul.filter;

import com.cloud.zuul.constant.constant;
import com.cloud.zuul.model.TokenModel;
import com.cloud.zuul.okhttp.OkHttpClientManager;
import com.cloud.zuul.utils.MD5;
import com.google.common.base.Splitter;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
public class AuthFilter extends ZuulFilter {


    private static Logger log = LoggerFactory.getLogger(AuthFilter.class);

    private final String COMMON = "common";
    private final String ADMIN = "admin";
    private final String USER = "user";

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    @Qualifier("loginOkClient")
    private OkHttpClient okHttpClient;


    @Override
    public String filterType() {
        return "pre";
    }

    @Override
    public int filterOrder() {
        return 1;
    }

    @Override
    public boolean shouldFilter() {
        RequestContext ctx = RequestContext.getCurrentContext();
        HttpServletRequest request = ctx.getRequest();
        String uri = request.getRequestURI();
        log.info("uri======" + uri);
        //不拦截登录url
        if (ctx.containsKey(constant.FILTER_FLAG_KEY)) {
            return (boolean) ctx.get(constant.FILTER_FLAG_KEY);
        }
        List<String> list = Splitter.on("/").splitToList(uri);

        if (list.size() > 3) {

            String start = list.get(1);
            String flag = list.get(3);
            if ("zuul".endsWith(start) && COMMON.equals(flag)) {
                return false;
            }

        }
        if (list.size() > 2) {
            String flag = list.get(2);
            switch (flag) {

                case COMMON:
                    return false;
                case USER:
                    return true;
                case ADMIN:
                    return true;

                default:
                    return true;
            }
        }


        return true;
    }

    @Override
    public Object run() {

        RequestContext ctx = RequestContext.getCurrentContext();
        HttpServletRequest request = ctx.getRequest();

        String token = request.getHeader(constant.AUTH_HEADER);
        log.info("获取token====" + token);
        if (token != null) {
            // 解析 Token
            Claims claims;
            String userId = null;

            try {
                claims = parseToken(token, constant.AUTH_TYPE);
                if (claims != null) {//获取userID
                    userId = (String) claims.get(constant.USER_ID_HEADER);
                }
            } catch (Exception e) {
                log.info("token验证异常========" + e.getMessage());
            }

            if (userId == null) {
                return refreshToken(token, ctx);
            }

            ctx.addZuulRequestHeader(constant.USER_ID_HEADER, userId);


        } else {
            forbidResult(ctx);
        }
        return null;

    }


    /**
     * token刷新
     *
     * @param token
     * @param ctx
     * @return
     */

    private Object refreshToken(String token, RequestContext ctx) {
        String auth_key = "";
        auth_key = token.replace(constant.AUTH_TYPE, "").trim();
        String md5Key = constant.TOKEN_KEY_PREFIX + MD5.getMD5(auth_key);
        if (redisTemplate.hasKey(md5Key)) {

            String refresh_token = (String) redisTemplate.opsForValue().get(md5Key);
            Map<String, String> map = new HashMap<String, String>();
            map.put("grant_type", "refresh_token");
            map.put("refresh_token", refresh_token);
            TokenModel tokenModel = null;
            try {

                tokenModel = OkHttpClientManager.getInstance().postRequestMapper(okHttpClient, constant.AUTH_URL, map);

            } catch (IOException e) {
                forbidResult(ctx);
                e.printStackTrace();
            }

            if (tokenModel.getAccess_token() != null) {
                String key = constant.TOKEN_KEY_PREFIX + MD5.getMD5(tokenModel.getAccess_token().trim());
                String value = tokenModel.getRefresh_token();
                int expire = constant.TOKEN_EXPIRE;
                log.info("authFilter登录md5key========" + key);
                redisTemplate.delete(md5Key);
                redisTemplate.opsForValue().set(key, value, expire, TimeUnit.SECONDS);
                ctx.getResponse().addHeader(constant.AUTH_HEADER, constant.AUTH_TYPE + tokenModel.getAccess_token());
                Claims refresh_claims = parseToken(tokenModel.getAccess_token(), null);
                ctx.addZuulRequestHeader(constant.USER_ID_HEADER, (String) refresh_claims.get(constant.USER_ID_HEADER));
            } else {
                forbidResult(ctx);
            }


        } else {
            forbidResult(ctx);
        }
        return null;
    }


    private void forbidResult(RequestContext ctx) {

        ctx.setSendZuulResponse(false);
        ctx.setResponseStatusCode(constant.FORBID_CODE);

    }

    /**
     * jwttoken的解析
     *
     * @param token
     * @param replace
     * @return
     */
    private Claims parseToken(String token, String replace) {

        final String SECRET = Base64.getEncoder().encodeToString("!@#$QWER1234qwer".getBytes());
        return Jwts.parser()
                // 验签
                .setSigningKey(SECRET)
                // 去掉 Bearer
                .parseClaimsJws(replace == null ? token : token.replace(replace, "").trim())
                .getBody();


    }


}
