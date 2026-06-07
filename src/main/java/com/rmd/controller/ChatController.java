package com.rmd.controller;

import com.rmd.llm.OllamaLLM;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/agent")
@CrossOrigin(origins = "*")
public class ChatController {

    @Autowired
    private OllamaLLM llm;

    @PostMapping("/chat")
    public Map<String, String> chat(@RequestBody Map<String, String> req) {
        String message = req.getOrDefault("message", "");
        String context = req.getOrDefault("context", "No prior agent run context available.");
        String reply = llm.chat(message, context);
        return Map.of("reply", reply);
    }
}
