
package com.rmd.controller;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import com.rmd.agent.Agent;
@RestController
@CrossOrigin
@RequestMapping("/agent")
public class Ctrl{
 private final Agent agent;
 public Ctrl(Agent a){this.agent=a;}
 @PostMapping("/run")
 public Map<String,Object> run(@RequestBody Map<String,Object> req){ return agent.run(req);} }
