package com.github.zeekoe.bluebird.heatpump;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.zeekoe.bluebird.heatpump.model.Token;
import com.github.zeekoe.bluebird.infrastructure.MyHttpClient;
import com.github.zeekoe.bluebird.Const;

import java.io.FileInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Properties;

public class Auth {
    static {
        Properties properties = new Properties();
        try {
            properties.load(new FileInputStream(Const.CONFIG_FILE_PATH));
            USERNAME = properties.getProperty("bluebird.username");
            PASSWORD = properties.getProperty("bluebird.password");
            API_KEY = properties.getProperty("bluebird.apikey");
            LOG_URL = properties.getProperty("bluebird.logurl");
            TOKEN_URL = "https://auth.weheat.nl/auth/realms/Weheat/protocol/openid-connect/token";

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static final MyHttpClient httpClient = new MyHttpClient();
    private Token token = null;

    private static String USERNAME;
    private static String PASSWORD;
    private static String API_KEY;
    private static String LOG_URL;
    private static String TOKEN_URL;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public String getApikey() {
        return API_KEY;
    }

    public String getLogurl() {
        return LOG_URL;
    }

    public String getToken() throws IOException, InterruptedException {
        if (token == null) {
            System.out.println("Retrieving token");
            token = OBJECT_MAPPER.readValue(doLogin(), Token.class);
        }
        if (token.getExpiryDateTime().minusMinutes(1).isBefore(LocalDateTime.now())) {
            System.out.println("Refreshing token");
            token = OBJECT_MAPPER.readValue(refreshToken(), Token.class);
        }
        return token.getAccess_token();
    }

    private String refreshToken() throws IOException, InterruptedException {
        return httpClient.post(TOKEN_URL,
                Map.of("Content-Type", "application/x-www-form-urlencoded",
                        "Accept", "application/json"),
                Map.of(
                        "grant_type", "refresh_token",
                        "scope", "openid",
                        "client_id", "WeheatCommunityAPI",
                        "refresh_token", token.getRefresh_token()
                ));
    }

    private String doLogin() throws IOException, InterruptedException {
        return httpClient.post(TOKEN_URL,
                Map.of("Content-Type", "application/x-www-form-urlencoded",
                        "Accept", "application/json"),
                Map.of(
                        "grant_type", "password",
                        "scope", "openid",
                        "client_id", "WeheatCommunityAPI",
                        "username", USERNAME,
                        "password", PASSWORD
                ));
    }
}
