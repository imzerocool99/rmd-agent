
package com.rmd.agent;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import com.rmd.tool.AgentTool;
import com.rmd.llm.AzureLLM;

@Service
public class Agent{
 @Autowired List<AgentTool> tools;
 @Autowired AzureLLM llm;
 public Map<String,Object> run(Map<String,Object> in){
  Map<String,Object> ctx=new HashMap<>(in);
  ctx.put("strategy", llm.decide(ctx));
  ctx.put("result", call("execute", ctx));
  return ctx;
 }
 Object call(String n,Map<String,Object> ctx){
  return tools.stream().filter(t->t.name().equals(n)).findFirst().map(t->t.run(ctx)).orElse(null);
 }
}
