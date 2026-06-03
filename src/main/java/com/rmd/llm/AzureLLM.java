package com.rmd.llm;

import java.util.Map;

import org.springframework.stereotype.Service;

@Service
public class AzureLLM {

    public Map<String, Object> decide(Map<String, Object> ctx) {

        // ✅ simple rule-based placeholder (still safe)
        String prediction = String.valueOf(ctx.get("prediction"));

        if ("volatile".equalsIgnoreCase(prediction)) {
            return Map.of(
                    "decision", "in_kind",
                    "reason", "Market volatility detected, preserving investments via in-kind transfer");
        }

        return Map.of(
                "decision", "sell",
                "reason", "Stable conditions, generating cash for RMD");

    }

}