package com.cloud.zuul.filter;

import com.cloud.zuul.constant.constant;
import com.cloud.zuul.model.TokenModel;
import com.cloud.zuul.utils.MD5;
import com.google.gson.Gson;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Base64;
import java.util.concurrent.TimeUnit;
@Component
public class AuthFilter extends ZuulFilter {


    private static Logger log = LoggerFactory.getLogger(AuthFilter.class);

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
        if(ctx.containsKey(constant.FILTER_FLAG_KEY)){
            return (boolean)ctx.get(constant.FILTER_FLAG_KEY);
        }
        return true;
    }

    @Override
    public Object run() {

        RequestContext ctx = RequestContext.getCurrentContext();
        HttpServletRequest request = ctx.getRequest();

        String token = request.getHeader(constant.AUTH_HEADER);
        final String SECRET = Base64.getEncoder().encodeToString("!@#$QWER1234qwer".getBytes());
        if (token != null) {
            // 解析 Token
            Claims claims = null;
            String userId=null;

            try {
                Jwts.parser()
                        // 验签
                        .setSigningKey(SECRET)
                        // 去掉 Bearer
                        .parseClaimsJws(token.replace("bearer", "").trim())
                        .getBody();
                userId = (String) claims.get("user_name");
            }catch (Exception e){
                log.info("token验证异常========" + e.getMessage());
            }

            //刷新token
//            System . currentTimeMillis()
//            Instant.now().

            if (userId == null) {

                String auth_key = request.getHeader(constant.AUTH_HEADER);
                auth_key = auth_key.replace("bearer", "").trim();
                String md5Key = constant.TOKEN_KEY_PREFIX + MD5.getMD5(auth_key);
                if (redisTemplate.hasKey(md5Key)) {

                    String basic = Base64.getEncoder().encodeToString(constant.AUTH_BASIC.getBytes());

                    String refresh_token = (String) redisTemplate.opsForValue().get(md5Key);


                    RequestBody formBody = new FormBody.Builder()
                            .add("grant_type", "refresh_token")
                            .add("refresh_token", refresh_token)
                            .build();
                    Request clinetRequest = new Request.Builder()
                            .url(constant.AUTH_URL)
                            .addHeader("Authorization", "Basic " + basic)
                            .post(formBody).build();

                    try {
                        Response response = okHttpClient.newCall(clinetRequest).execute();
                        String json = response.body().string();

                        TokenModel tokenModel = new Gson().fromJson(json, TokenModel.class);
                        String key = constant.TOKEN_KEY_PREFIX + MD5.getMD5(tokenModel.getAccess_token().trim());
                        String value = tokenModel.getRefresh_token();
                        int expire = constant.TOKEN_EXPIRE;
                        log.info("登录md5key========" + key);
                        redisTemplate.delete(md5Key);
                        redisTemplate.opsForValue().set(key, value, expire, TimeUnit.SECONDS);
                        ctx.getResponse().addHeader(constant.AUTH_HEADER, tokenModel.getToken_type() + " " + tokenModel.getAccess_token());
                        Claims refresh_claims = Jwts.parser()
                                // 验签
                                .setSigningKey(SECRET)
                                // 去掉 Bearer
                                .parseClaimsJws(tokenModel.getAccess_token())
                                .getBody();
                        ctx.addZuulRequestHeader(constant.USER_ID_HEADER, (String) refresh_claims.get("user_name"));
                        return null;
                    } catch (IOException e) {
                        ctx.setSendZuulResponse(false);
                        ctx.setResponseStatusCode(constant.FORBID_CODE);
                        e.printStackTrace();
                    }


                } else {
                    ctx.setSendZuulResponse(false);
                    ctx.setResponseStatusCode(constant.FORBID_CODE);
                    return null;
                }


            }

            ctx.addZuulRequestHeader(constant.USER_ID_HEADER, userId);


        } else {
            ctx.setSendZuulResponse(false);
            ctx.setResponseStatusCode(constant.FORBID_CODE);
        }
        return null;

    }
}
