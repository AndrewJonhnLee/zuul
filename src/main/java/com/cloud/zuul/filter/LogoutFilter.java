package com.cloud.zuul.filter;

import com.cloud.zuul.constant.constant;
import com.cloud.zuul.utils.MD5;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

@Component
public class LogoutFilter extends ZuulFilter {

    private static Logger log = LoggerFactory.getLogger(LoginFilter.class);
    private final String LOGOUT_URI="/gate/logout";

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
        return true;
    }

    @Override
    public Object run() {
        RequestContext ctx = RequestContext.getCurrentContext();
        HttpServletRequest request = ctx.getRequest();
        String uri=request.getRequestURI();
        log.info("uri======"+uri);
        if(LOGOUT_URI.equals(uri)&&request.getHeader(constant.USER_ID_HEADER)!=null){

            String auth_key=request.getHeader(constant.AUTH_HEADER);
            auth_key=auth_key.replace(constant.AUTH_TYPE,"").trim();
            String md5Key= constant.TOKEN_KEY_PREFIX+ MD5.getMD5(auth_key);
            log.info("退出登录md5key========"+md5Key);
            redisTemplate.delete(md5Key);
            ctx.setSendZuulResponse(false); //不进行路由
            ctx.set(constant.FILTER_FLAG_KEY,false);
            ctx.setResponseStatusCode(200);

            try {
                ctx.getResponse().getWriter().write(constant.RESULT_OK);
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        return null;
    }
}
