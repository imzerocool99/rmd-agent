package com.rmd.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import com.rmd.agent.MasterAgent;
import com.rmd.mcp.AlpacaPortfolioService;
import com.rmd.monitor.MonitorService;

import java.util.*;

@RestController
@RequestMapping("/agent")
@CrossOrigin
public class AgentController {
    private final MasterAgent a;

    @Autowired
    AlpacaPortfolioService portfolio;

    @Value("${trade.max.qty}")
    private int maxQty;

    @Autowired
    MonitorService monitor;

    @GetMapping("/logs")
    public List<String> getLogs() {
        return monitor.getLogs();
    }

    @PostMapping("/config/limit")
    public Map<String, Object> updateLimit(@RequestBody Map<String, Integer> req) {
        this.maxQty = req.get("maxQty");
        return Map.of("status", "updated", "maxQty", maxQty);
    }

    @GetMapping("/portfolio")
    public Map<String, Object> portfolio() {
        return portfolio.getAccount();
    }

    @GetMapping("/positions")
    public List<Map<String, Object>> positions() {
        return portfolio.getPositions();
    }

    public AgentController(MasterAgent a) {
        this.a = a;
    }

    @PostMapping("/run")
    public Map run(@RequestBody Map r) {
        return a.run(r);
    }

}