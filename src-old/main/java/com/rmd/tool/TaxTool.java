
package com.rmd.tool;
import java.util.*;
import org.springframework.stereotype.Component;
@Component
public class TaxTool implements AgentTool{
 public String name(){return "tax";}
 public Object run(Map<String,Object> ctx){ return "sell"; }
}
