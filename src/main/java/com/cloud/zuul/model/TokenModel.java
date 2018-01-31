package com.cloud.zuul.model;


import lombok.Data;

@Data
public class TokenModel {

    private String access_token;
    private String token_type;
    private String refresh_token;
    private long expires_in;
    private String scope;
    private String jti;
}
