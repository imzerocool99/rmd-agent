package com.rmd.logic;

import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class AssetRankingService {
    public List<String> rank(Map ctx) {
        return List.of("AAPL", "MSFT");
    }
}