package com.cloud.zuul.filter;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletResponse;


@Component
@Slf4j
public class ErrorFilter extends ZuulFilter {
    @Override
    public String filterType() {
        return "error";
    }

    @Override
    public int filterOrder() {
        return 10;
    }

    @Override
    public boolean shouldFilter() {
        return true;
    }

    @Override
    public Object run() {
//        SendErrorFilter
        RequestContext ctx = RequestContext.getCurrentContext();
        Throwable throwable = ctx.getThrowable();
        log.error("this is a ErrorFilter : {}", throwable.getCause().getMessage());
        throwable.printStackTrace();
        ctx.set("error.status_code", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        ctx.set("error.exception", throwable.getCause());
        return null;
    }

}


//  if (ctx.getResponseBody() == null) {
//          ctx.setResponseBody(body);
//          ctx.setSendZuulResponse(false);
//          }

//ctx.setSendZuulResponse(false);,没有走route,没有response,会报npn
//一般来讲，正常的流程是pre-->route-->post
//在pre过滤器阶段抛出异常，pre--> error -->post
//在route过滤器阶段抛出异常，pre-->route-->error -->post
//在post过滤器阶段抛出异常，pre-->route-->post--> error
