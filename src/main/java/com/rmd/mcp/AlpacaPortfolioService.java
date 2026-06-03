package com.rmd.mcp;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import org.springframework.beans.factory.annotation.Value;

import java.util.*;

@Service
public class AlpacaPortfolioService {

    @Value("${alpaca.api.key}")
    private String apiKey;

    @Value("${alpaca.secret.key}")
    private String secretKey;

    @Value("${alpaca.base.url}")
    private String baseUrl;

    public Map<String,Object> getAccount() {

        RestTemplate rest = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.set("APCA-API-KEY-ID", apiKey);
        headers.set("APCA-API-SECRET-KEY", secretKey);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        return rest.exchange(
                baseUrl + "/account",
                HttpMethod.GET,
                entity,
                Map.class
        ).getBody();
    }

    public List<Map<String,Object>> getPositions() {

        RestTemplate rest = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.set("APCA-API-KEY-ID", apiKey);
        headers.set("APCA-API-SECRET-KEY", secretKey);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        return rest.exchange(
                baseUrl + "/positions",
                HttpMethod.GET,
                entity,
                List.class
        ).getBody();
    }
}
