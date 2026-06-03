package com.rmd.mcp;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.*;
import org.springframework.beans.factory.annotation.Value;
import java.util.*;

@Component
public class AlpacaMCP {

    @Value("${alpaca.api.key}")   private String apiKey;
    @Value("${alpaca.secret.key}") private String secretKey;
    @Value("${alpaca.base.url}")  private String baseUrl;

    public Map<String, Object> execute(Map<String, Object> ctx) {
        String symbol = String.valueOf(ctx.get("symbol"));
        Object qty    = ctx.get("qty");
        double price  = Double.parseDouble(ctx.getOrDefault("price", "0").toString());
        double value  = Double.parseDouble(ctx.getOrDefault("value", "0").toString());
        // POC mode: simulate instantly — no external HTTP call
        return simulateTrade(symbol, qty, price, value);
    }

    private Map<String, Object> enrichResult(Map raw, String symbol, Object qty, double price, double value) {
        Map<String, Object> result = new LinkedHashMap<>(raw != null ? raw : Map.of());
        result.put("status", "executed");
        result.put("symbol", symbol);
        result.put("qty", qty);
        result.put("price", price);
        result.put("value", value);
        return result;
    }

    private Map<String, Object> simulateTrade(String symbol, Object qty, double price, double value) {
        return Map.of(
            "status",    "simulated",
            "symbol",    symbol,
            "qty",       qty,
            "price",     price,
            "value",     value,
            "side",      "sell",
            "type",      "market",
            "note",      "Paper trade simulated locally (Alpaca timeout)"
        );
    }

    private RestTemplate buildRestTemplate(int timeoutMs) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeoutMs);
        factory.setReadTimeout(timeoutMs);
        return new RestTemplate(factory);
    }
}
