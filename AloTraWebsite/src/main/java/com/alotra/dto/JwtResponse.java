package com.alotra.dto;

public class JwtResponse {
    private String token;
    private String tokenType="Bearer";
    public JwtResponse(String token){this.token=token;}
    public String getToken(){return token;}
    public void setToken(String token){this.token=token;}
    public String getTokenType(){return tokenType;}
    public void setTokenType(String tokenType){this.tokenType=tokenType;}
}
