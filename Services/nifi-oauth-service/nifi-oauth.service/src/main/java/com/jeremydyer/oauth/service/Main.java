package com.jeremydyer.oauth.service;

import com.google.api.client.auth.oauth2.AuthorizationCodeFlow;
import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.auth.oauth2.ClientParametersAuthentication;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;

import java.io.IOException;
import java.util.Collections;

/**
 * Created by jdyer on 4/18/16.
 */
public class Main {

    public static void main(String[] args) {
        System.out.println("Running");

        JsonFactory jsonFactory = new JacksonFactory();
        HttpTransport httpTransport = new NetHttpTransport();

        AuthorizationCodeFlow flow = new AuthorizationCodeFlow.Builder(
                BearerToken.authorizationHeaderAccessMethod(),
                httpTransport, jsonFactory,
                new GenericUrl("https://github.com/login/oauth/access_token"),
                new ClientParametersAuthentication( "", "" ),
                "https://github.com/login/oauth/authorize").build();

        TokenResponse tokenResponse = flow
                .newTokenRequest("authorization_code")
                .setScopes(Collections.singletonList("user:email"))
                .setRequestInitializer(new HttpRequestInitializer() {
                    @Override
                    public void initialize(HttpRequest request) throws IOException {
                        request.getHeaders().setAccept("application/json");
                    }
                }).execute();

        System.out.println("done");
    }
}
