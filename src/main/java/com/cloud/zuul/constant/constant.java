package com.cloud.zuul.constant;

import okhttp3.MediaType;

public class constant {

    public static final String FILTER_FLAG_KEY="is_continue";
    public static final String LOGOUT_FLAG_KEY="is_logout";

    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    public static final MediaType MEDIA_TYPE_FORMURL = MediaType.parse("application/x-www-form-urlencoded; charset=utf-8");//mdiatype 这个需要和服务端保持一致

    public static final String TOKEN_KEY_PREFIX="token:";
    public static final String AUTH_HEADER="Authorization";
    public static final String AUTH_TYPE="bearer ";
    public static final String AUTH_TYPE_TRIM="Bearer";
    public static final String RESULT_OK="ok";
    public static final String RESULT_FAIL="fail";
    public static final String AUTH_BASIC="zull-client:zull-secret";
    public static final String AUTH_URL="http://SERVICE_AUTH/oauth/token";
    public static final String USER_ID_HEADER="userId";
    public static final int FORBID_CODE=401;
    public static final int SUCCESS_CODE=200;
    public static final int TOKEN_EXPIRE=7 * 24 * 60 * 60;

}
