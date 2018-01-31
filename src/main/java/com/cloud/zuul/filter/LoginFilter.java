package com.cloud.zuul.filter;

import com.cloud.zuul.constant.constant;
import com.cloud.zuul.model.TokenModel;
import com.cloud.zuul.utils.MD5;
import com.google.gson.Gson;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.netflix.zuul.filters.ProxyRequestHelper;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
public class LoginFilter extends ZuulFilter {



    private static Logger log = LoggerFactory.getLogger(LoginFilter.class);
    private final String LOGIN_URI="/gate/login";

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
//        ValueOperations<String, String> stringOperations = redisTemplate.opsForValue();
//                redisTemplate.getExpire()
//        stringOperations.
        return true;
    }

    @Override
    public Object run() {
        RequestContext ctx = RequestContext.getCurrentContext();
        HttpServletRequest request = ctx.getRequest();
        String uri=request.getRequestURI();
        log.info("uri======"+uri);
        if("POST".equals(request.getMethod())&&LOGIN_URI.equals(uri)){
            // TODO: 18-1-26 请求token,返回accesstoken,加入redis
            Map<String, String[]> map = request.getParameterMap();
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


            String username=map.get("username")[0];
            String password=map.get("password")[0];
            String basic= Base64.getEncoder().encodeToString(constant.AUTH_BASIC.getBytes());

            RequestBody formBody = new FormBody.Builder()
                    .add("username", username)
                    .add("password", password)
                    .add("grant_type", "password")
                    .build();
            Request clinetRequest = new Request.Builder()
                    .url(constant.AUTH_URL)
                    .addHeader("Authorization","Basic "+basic)
                    .post(formBody).build();
            try {


                Response response = okHttpClient.newCall(clinetRequest).execute();
                String json=response.body().string();
                log.info("json返回======"+json);
                TokenModel tokenModel=new Gson().fromJson(json,TokenModel.class);
                if(tokenModel.getAccess_token()==null){
                    ctx.setSendZuulResponse(false);
                    ctx.setResponseStatusCode(constant.FORBID_CODE);
                    ctx.set(constant.FILTER_FLAG_KEY,false);
                }
                String key= constant.TOKEN_KEY_PREFIX+MD5.getMD5(tokenModel.getAccess_token().trim());
                String value=tokenModel.getRefresh_token();
                int expire=constant.TOKEN_EXPIRE;
                log.info("登录md5key========"+key);
                redisTemplate.opsForValue().set(key,value,expire,TimeUnit.SECONDS);
                ctx.getResponse().addHeader(constant.AUTH_HEADER,tokenModel.getToken_type()+" "+tokenModel.getAccess_token());
                ctx.setSendZuulResponse(false); //不进行路由
                ctx.set(constant.FILTER_FLAG_KEY,false);
                ctx.setResponseStatusCode(constant.SUCCESS_CODE);

                ctx.getResponse().getWriter().write(constant.RESULT_OK);

            } catch (IOException e) {
                //登录失败返回
                ctx.setSendZuulResponse(false);//true,会进行路由，也就是会调用api服务提供者
                ctx.setResponseStatusCode(constant.FORBID_CODE);
                ctx.set(constant.FILTER_FLAG_KEY,false);
                try {
                    ctx.getResponse().getWriter().write(constant.RESULT_FAIL);
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                e.printStackTrace();
            }


        }
        return null;
    }



    protected HttpUrl transformUrl(String serverIdUrl){

        Request originRequest = new Request.Builder()
                .url(serverIdUrl)
                .build();

        HttpUrl originalUrl = originRequest.url();
        String serviceId = originalUrl.host();
        ServiceInstance service = client.choose(serviceId);
        if (service == null) {
            throw new IllegalStateException("No instances available for service_auth");
        }

        HttpUrl url = originalUrl.newBuilder()
                .scheme(service.isSecure()? "https" : "http")
                .host(service.getHost())
                .port(service.getPort())
                .build();

        return url;

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