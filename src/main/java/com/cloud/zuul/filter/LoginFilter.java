package com.cloud.zuul.filter;

import com.cloud.zuul.constant.constant;
import com.cloud.zuul.model.TokenModel;
import com.cloud.zuul.okhttp.OkHttpClientManager;
import com.cloud.zuul.utils.MD5;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.netflix.zuul.filters.ProxyRequestHelper;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
public class LoginFilter extends ZuulFilter {


    private static Logger log = LoggerFactory.getLogger(LoginFilter.class);
    private final String LOGIN_URI = "/gate/login";
    private final String USERNAME = "username";
    private final String PASSWORD = "password";

    @Autowired
    private LoadBalancerClient client;


    @Autowired
    private ProxyRequestHelper helper;
    @Autowired
    private RedisTemplate redisTemplate;

//    @Autowired
//    private OkHttpRibbonInterceptor interceptor;


    @Autowired
    @Qualifier("loginOkClient")
    private OkHttpClient okHttpClient;

    @Override
    public String filterType() {
        return "pre";
    }

    @Override
    public int filterOrder() {
        return 0;
    }

    @Override
    public boolean shouldFilter() {
        RequestContext ctx = RequestContext.getCurrentContext();
        HttpServletRequest request = ctx.getRequest();
        String uri = request.getRequestURI();
        log.info("uri======" + uri);
        if ("POST".equals(request.getMethod()) && LOGIN_URI.equals(uri)) {

            return true;
        }
        return false;
    }

    @Override
    public Object run() {
        RequestContext ctx = RequestContext.getCurrentContext();
        HttpServletRequest request = ctx.getRequest();
        Map<String, String[]> map = request.getParameterMap();
        String username = map.get(USERNAME)[0];
        String password = map.get(PASSWORD)[0];

        Map<String, String> paraMap = new HashMap<String, String>();
        paraMap.put(USERNAME, username);
        paraMap.put(PASSWORD, password);
        paraMap.put("grant_type", PASSWORD);

        try {
            TokenModel tokenModel = OkHttpClientManager.getInstance().postRequestMapper(okHttpClient, constant.AUTH_URL, paraMap);
            if (tokenModel.getAccess_token() == null) {
                modifyResult(ctx, constant.FORBID_CODE, constant.RESULT_FAIL);
                return null;
            }
            String md5_key=MD5.getMD5(tokenModel.getAccess_token().trim());
            String token_key = constant.TOKEN_KEY_PREFIX + md5_key;
            String token_value = tokenModel.getAccess_token();
            String refresh_key=constant.REFRESH_KEY_PREFIX + md5_key;;
            String refresh_value=tokenModel.getRefresh_token();
            log.info("loginFilter登录md5key========" + md5_key);
            redisTemplate.opsForValue().set(token_key, token_value, constant.TOKEN_EXPIRE, TimeUnit.SECONDS);
            redisTemplate.opsForValue().set(refresh_key, refresh_value, constant.REFRESH_TOKEN_EXPIRE, TimeUnit.SECONDS);
//                返回头信息携带token
            ctx.getResponse().addHeader(constant.AUTH_HEADER, constant.AUTH_TYPE + md5_key);
            modifyResult(ctx, constant.SUCCESS_CODE, constant.RESULT_OK);


        } catch (IOException e) {
            //登录失败返回
            try {
                modifyResult(ctx, constant.FORBID_CODE, constant.RESULT_FAIL);
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            e.printStackTrace();
        }


        return null;
    }

    private void modifyResult(RequestContext ctx, int statusCode, String content) throws IOException {

        if (ctx.getResponseBody() == null) {
            ctx.setResponseBody(content);
            ctx.setSendZuulResponse(false);//true,会进行路由，也就是会调用api服务提供者
        }
        ctx.setResponseStatusCode(statusCode);
        ctx.set(constant.FILTER_FLAG_KEY, false);

    }

}
//    RequestBody body = RequestBody.create(JSON, json);
//    Request request = new Request.Builder()
//            .url(url)
//            .post(body)
//            .build();

//
//    RequestBody formBody = new FormEncodingBuilder()
//            .add("platform", "android")
//            .add("name", "bug")
//            .add("subject", "XXXXXXXXXXXXXXX")
//            .build();


//            InputStream in = (InputStream) ctx.get("requestEntity");
//            Map<String, String> paraMap=null;
//            Splitter.MapSplitter splitter = Splitter.on("&").withKeyValueSeparator("=");
//            if (in == null) {
//                try {
//                    in = ctx.getRequest().getInputStream();
//                    String body = StreamUtils.copyToString(in, Charset.forName("UTF-8"));
//                    log.info("请求类型======"+body);
//                    paraMap=splitter.split(body);
//
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }