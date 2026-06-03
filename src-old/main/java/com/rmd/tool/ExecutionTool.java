
package com.rmd.tool;
import java.util.*;
import org.springframework.stereotype.Component;
@Component
public class ExecutionTool implements AgentTool{
 public String name(){return "execute";}
 public Object run(Map<String,Object> ctx){ return "executed"; }
}
