
package com.rmd.llm;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class AzureLLM {
 public String decide(Map<String,Object> ctx){
  // structured prompt placeholder
  String pref=(String)ctx.get("preference");
  if("Cash".equals(pref)) return "sell";
  if("In-Kind".equals(pref)) return "transfer";
  return "recommend";
 }
}
