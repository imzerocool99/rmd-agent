
package com.rmd.mcp;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import org.springframework.beans.factory.annotation.Value;
import java.util.*;
@Component
public class AlpacaMCP {
 @Value("${alpaca.api.key}") private String apiKey;
 @Value("${alpaca.secret.key}") private String secretKey;
 @Value("${alpaca.base.url}") private String baseUrl;
 public Map<String,Object> execute(Map<String,Object> ctx){
  try{
   RestTemplate r=new RestTemplate();
   HttpHeaders h=new HttpHeaders();
   h.set("APCA-API-KEY-ID",apiKey);
   h.set("APCA-API-SECRET-KEY",secretKey);
   h.setContentType(MediaType.APPLICATION_JSON);
   Map<String,Object> b=new HashMap<>();
   b.put("symbol",ctx.get("symbol"));
   b.put("qty",ctx.get("qty"));
   b.put("side","sell");
   b.put("type","market");
   b.put("time_in_force","gtc");
   return r.postForObject(baseUrl+"/orders",new HttpEntity<>(b,h),Map.class);
  }catch(Exception e){return Map.of("error",e.getMessage());}
 }
}
