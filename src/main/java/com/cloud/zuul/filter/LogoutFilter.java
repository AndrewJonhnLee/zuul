package com.cloud.zuul.filter;

import com.cloud.zuul.constant.constant;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;

@Component
public class LogoutFilter extends ZuulFilter {

    private static Logger log = LoggerFactory.getLogger(LogoutFilter.class);
    private final String LOGOUT_URI = "/gate/logout";

    @Autowired
    private RedisTemplate redisTemplate;


    @Override
    public String filterType() {
        return "pre";
    }

    @Override
    public int filterOrder() {
        return 3;
    }

    @Override
    public boolean shouldFilter() {
        RequestContext ctx = RequestContext.getCurrentContext();
        HttpServletRequest request = ctx.getRequest();
        String uri = request.getRequestURI();
        log.info("uri======" + uri);
        //必须通过身份验证才拦截
        if (LOGOUT_URI.equals(uri) && ctx.getZuulRequestHeaders().get(constant.USER_ID_HEADER.toLowerCase()) != null) {

            return true;
        }
        return false;
    }

    @Override
    public Object run() {
        RequestContext ctx = RequestContext.getCurrentContext();
        HttpServletRequest request = ctx.getRequest();
        String auth_key = request.getHeader(constant.AUTH_HEADER);
        String token_Key = constant.TOKEN_KEY_PREFIX + auth_key;
        String refresh_Key = constant.REFRESH_KEY_PREFIX + auth_key;
        log.info("退出登录md5key========" + auth_key);
        if (redisTemplate.hasKey(token_Key)){
            redisTemplate.delete(token_Key);
        }

        if (redisTemplate.hasKey(refresh_Key)){
            redisTemplate.delete(refresh_Key);
        }
        if (ctx.getResponseBody() == null) {
            ctx.setResponseBody("ok");
            ctx.setSendZuulResponse(false);//true,会进行路由，也就是会调用api服务提供者
            ctx.setResponseStatusCode(200);
        }
        ctx.set(constant.FILTER_FLAG_KEY, false);
//
//        try {
//            ctx.getResponse().getWriter().write(constant.RESULT_OK);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

        return null;
    }
}
