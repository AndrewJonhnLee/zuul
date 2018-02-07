package com.cloud.zuul.okhttp;

import com.cloud.zuul.constant.constant;
import com.cloud.zuul.model.TokenModel;
import com.google.gson.Gson;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.FileNameMap;
import java.net.URLConnection;
import java.util.Base64;
import java.util.Map;
import java.util.Set;

public class OkHttpClientManager {

    private static Logger log = LoggerFactory.getLogger(OkHttpClientManager.class);
    private Gson mGson;


    private OkHttpClientManager() {
        mGson = new Gson();
    }

    public static OkHttpClientManager getInstance() {
        return InnerClass.INSTANCE;
    }


    private static class InnerClass {
        private static final OkHttpClientManager INSTANCE = new OkHttpClientManager();
    }

    public TokenModel postRequestMapper(OkHttpClient okHttpClient,String url,Map<String, String> params) throws IOException {

        String json=postRequest(okHttpClient,url,params);
        TokenModel tokenModel = mGson.fromJson(json, TokenModel.class);
        return tokenModel;

    }


    public String postRequest(OkHttpClient okHttpClient,String url,Map<String, String> params) throws IOException {

        Request request=buildPostRequest(url,map2Params(params));
        Response response = okHttpClient.newCall(request).execute();
        String json = response.body().string();
        log.info("json返回======" + json);
        return json;
    }


    private Request buildPostRequest(String url, Param[] params) {
        if (params == null) {
            params = new Param[0];
        }
        FormBody.Builder builder = new FormBody.Builder();
        for (Param param : params) {
            builder.add(param.key, param.value);
        }
        RequestBody requestBody = builder.build();
        String basic = Base64.getEncoder().encodeToString(constant.AUTH_BASIC.getBytes());

        return new Request.Builder()
                .url(url)
                .post(requestBody)
                .addHeader("Authorization", "Basic " + basic)
                .build();
    }

    private String getFileName(String path) {
        int separatorIndex = path.lastIndexOf("/");
        return (separatorIndex < 0) ? path : path.substring(separatorIndex + 1, path.length());
    }



    private String guessMimeType(String path) {
        FileNameMap fileNameMap = URLConnection.getFileNameMap();
        String contentTypeFor = fileNameMap.getContentTypeFor(path);
        if (contentTypeFor == null) {
            contentTypeFor = "application/octet-stream";
        }
        return contentTypeFor;
    }


    private Param[] validateParam(Param[] params) {
        if (params == null)
            return new Param[0];
        else return params;
    }

    private Param[] map2Params(Map<String, String> params) {
        if (params == null) return new Param[0];
        int size = params.size();
        Param[] res = new Param[size];
        Set<Map.Entry<String, String>> entries = params.entrySet();
        int i = 0;
        for (Map.Entry<String, String> entry : entries) {
            res[i++] = new Param(entry.getKey(), entry.getValue());
        }
        return res;
    }



    public static class Param {
        public Param() {
        }

        public Param(String key, String value) {
            this.key = key;
            this.value = value;
        }

        String key;
        String value;
    }


}