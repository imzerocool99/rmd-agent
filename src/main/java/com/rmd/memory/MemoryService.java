package com.rmd.memory;

import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class MemoryService {
    Map<String, List<Map>> m = new HashMap<>();

    public void save(String id, Map ctx) {
        m.computeIfAbsent(id, k -> new ArrayList<>()).add(ctx);
    }

    public List<Map> get(String id) {
        return m.getOrDefault(id, new ArrayList<>());
    }
}