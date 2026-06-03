
package com.rmd.agent;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.*;
@Component
public class Scheduler {
 @Autowired MasterAgent agent;
 @Scheduled(fixedRateString="${agent.interval}")
 public void run(){agent.run(new HashMap<>());} }
