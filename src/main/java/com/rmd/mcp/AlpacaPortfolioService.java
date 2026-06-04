package com.rmd.mcp;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
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

    @Value("${alpaca.news.base.url:https://data.alpaca.markets/v2}")
    private String newsBaseUrl;

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

    public List<Map<String,Object>> getNews(List<String> symbols) {
        if (symbols == null || symbols.isEmpty()) {
            return Collections.emptyList();
        }

        RestTemplate rest = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.set("APCA-API-KEY-ID", apiKey);
        headers.set("APCA-API-SECRET-KEY", secretKey);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        String symbolQuery = String.join(",", symbols);
        String url = UriComponentsBuilder.fromHttpUrl(newsBaseUrl + "/stocks/news")
                .queryParam("symbols", symbolQuery)
                .queryParam("limit", 6)
                .toUriString();

        List<Map<String, Object>> news = Collections.emptyList();
        try {
            news = rest.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    List.class
            ).getBody();
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            // upstream returned 4xx (e.g., Not Found or unauthorized) — treat as no news
            return Collections.emptyList();
        } catch (org.springframework.web.client.ResourceAccessException e) {
            // network or connection issue — treat as no news
            return Collections.emptyList();
        } catch (Exception e) {
            return Collections.emptyList();
        }

        return news == null ? Collections.emptyList() : news;
    }

    public List<Map<String,Object>> getCatalyst(List<String> symbols) {
        List<Map<String,Object>> news = getNews(symbols);
        List<Map<String,Object>> catalysts = new ArrayList<>();

        if (news.isEmpty()) {
            // No news returned from Alpaca — provide gentle monitoring placeholders per symbol
            if (symbols != null && !symbols.isEmpty()) {
                for (String s : symbols) {
                    Map<String, Object> c = new HashMap<>();
                    c.put("symbol", s);
                    c.put("title", "No recent headlines — monitoring " + s);
                    c.put("summary", "No recent news available for " + s + ". We will surface updates as they arrive.");
                    c.put("source", "Alpaca Catalyst");
                    c.put("url", "");
                    c.put("time", "—");
                    c.put("impact", "Low");
                    catalysts.add(c);
                }
            }
            return catalysts;
        }

        for (Map<String, Object> article : news) {
            Map<String, Object> catalyst = new HashMap<>();
            catalyst.put("symbol", article.getOrDefault("symbol", article.getOrDefault("ticker", "")));
            catalyst.put("title", article.getOrDefault("headline", article.getOrDefault("title", "Market catalyst detected")));
            catalyst.put("summary", article.getOrDefault("summary", article.getOrDefault("excerpt", "")));
            catalyst.put("source", article.getOrDefault("source", "Alpaca Catalyst"));
            catalyst.put("url", article.getOrDefault("url", ""));
            catalyst.put("time", article.getOrDefault("published_at", article.getOrDefault("publishedAt", "")));
            catalyst.put("impact", article.containsKey("category") && article.get("category") != null ? article.get("category") : "Medium");
            catalysts.add(catalyst);
        }
        return catalysts;
    }
}
