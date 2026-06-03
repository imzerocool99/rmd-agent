
package com.rmd.integration;

import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import java.util.*;

public class AlpacaClient {

  private final String API="https://paper-api.alpaca.markets/v2/orders";

  public String placeOrder(String symbol,int qty,String type){
    RestTemplate rt=new RestTemplate();

    HttpHeaders h=new HttpHeaders();
    h.set("APCA-API-KEY-ID","PKFPWZM5RPDH76OOD2P5JXQ7BI");
    h.set("APCA-API-SECRET-KEY","9qutFqhrKtEuA2m94FYYANgPakoGoks2xwiktWHjhdyK");

    Map<String,Object> body=new HashMap<>();
    body.put("symbol",symbol);
    body.put("qty",qty);
    body.put("side","sell");
    body.put("type",type.toLowerCase());
    body.put("time_in_force","gtc");

    HttpEntity<Map<String,Object>> e=new HttpEntity<>(body,h);
    return rt.postForObject(API,e,String.class);
  }
}
