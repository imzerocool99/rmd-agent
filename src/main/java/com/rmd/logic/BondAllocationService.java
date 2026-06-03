package com.rmd.logic;

import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class BondAllocationService {

    public String selectBondETF(Map<String,Object> ctx){

        // ✅ Simple safe logic (no hallucination)
        // Choose based on prediction or risk

        String prediction = String.valueOf(ctx.getOrDefault("prediction","stable"));

        if ("volatile".equalsIgnoreCase(prediction)) {
            return "SHY";  // safer short-term bonds
        }

        return "BND"; // general bond ETF
    }
}