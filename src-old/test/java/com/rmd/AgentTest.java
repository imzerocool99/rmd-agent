
package com.rmd;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import com.rmd.agent.Agent;
import java.util.*;

@SpringBootTest
public class AgentTest{
 @Autowired Agent agent;
 @Test
 void testAgentRun(){
  Map<String,Object> input=new HashMap<>();
  input.put("preference","Cash");
  Map<String,Object> res=agent.run(input);
  Assertions.assertNotNull(res.get("strategy"));
 }
}
