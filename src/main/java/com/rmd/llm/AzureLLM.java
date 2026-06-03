package com.rmd.llm;

import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class AzureLLM {

    @Value("${ollama.url}")
    private String ollamaUrl;

    @Value("${ollama.model}")
    private String ollamaModel;

    private final RestTemplate restTemplate = new RestTemplate();

    public Map<String, Object> decide(Map<String, Object> ctx) {
        String prediction = String.valueOf(ctx.get("prediction"));
        double rmd = Double.parseDouble(ctx.getOrDefault("rmdAmount", "0").toString());
        Object assets = ctx.get("selectedAssets");

        String prompt = String.format(
            "You are an IRA RMD advisor. Client data: market prediction=%s, RMD amount=$%.2f. " +
            "Decide whether to execute a CASH sale (sell assets) or IN-KIND transfer (move assets to brokerage). " +
            "Reply with exactly one word: 'sell' or 'in_kind', then a dash, then one sentence reason.",
            prediction, rmd
        );

        try {
            Map<String, Object> body = Map.of(
                "model", ollamaModel,
                "prompt", prompt,
                "stream", false
            );
            Map response = restTemplate.postForObject(ollamaUrl, body, Map.class);
            String text = String.valueOf(response.get("response")).trim();
            return parseOllamaResponse(text);
        } catch (Exception e) {
            return fallback(prediction);
        }
    }

    public String chat(String userMessage, String context) {
        String prompt = String.format(
            "You are a helpful IRA RMD financial assistant. Context about the client: %s\n\n" +
            "Answer this question clearly and concisely in 2-3 sentences: %s",
            context, userMessage
        );

        try {
            Map<String, Object> body = Map.of(
                "model", ollamaModel,
                "prompt", prompt,
                "stream", false
            );
            Map response = restTemplate.postForObject(ollamaUrl, body, Map.class);
            return String.valueOf(response.get("response")).trim();
        } catch (Exception e) {
            return smartFallbackChat(userMessage);
        }
    }

    private Map<String, Object> parseOllamaResponse(String text) {
        String lower = text.toLowerCase();
        if (lower.startsWith("in_kind") || lower.contains("in-kind") || lower.contains("in_kind")) {
            return Map.of("decision", "in_kind", "reason", extractReason(text));
        }
        return Map.of("decision", "sell", "reason", extractReason(text));
    }

    private String extractReason(String text) {
        int dash = text.indexOf('-');
        if (dash >= 0 && dash < text.length() - 1) {
            return text.substring(dash + 1).trim();
        }
        return text.length() > 80 ? text.substring(0, 80) + "..." : text;
    }

    private Map<String, Object> fallback(String prediction) {
        if ("volatile".equalsIgnoreCase(prediction)) {
            return Map.of("decision", "in_kind",
                "reason", "Market volatility detected — preserving investments via in-kind transfer to avoid selling at a loss.");
        }
        return Map.of("decision", "sell",
            "reason", "Stable market conditions — generating cash by liquidating underperforming and over-concentrated assets.");
    }

    public String recommendReinvestment(double rmdAmount, int age, List<Map<String, Object>> suggestions) {
        StringBuilder productList = new StringBuilder();
        for (Map<String, Object> s : suggestions) {
            productList.append(String.format("- %s (%.1f%% allocation = $%.2f, yield %.2f%%)\n",
                s.get("name"), s.get("allocationPct"), s.get("allocationAmount"), s.get("yieldPct")));
        }

        String prompt = String.format(
            "You are an intelligent financial planning agent helping a %d-year-old IRA client reinvest their $%.2f RMD distribution.\n\n" +
            "The following reinvestment products have been pre-selected by the allocation engine:\n%s\n" +
            "As an Agentic AI, explain in 3-4 sentences:\n" +
            "1. WHY this allocation mix makes sense for this client's age and situation\n" +
            "2. Which product is the most important and why\n" +
            "3. What the estimated annual income from this reinvestment will be\n" +
            "Be specific, use the actual numbers, and sound like a knowledgeable financial advisor.",
            age, rmdAmount, productList
        );

        try {
            Map<String, Object> body = Map.of("model", ollamaModel, "prompt", prompt, "stream", false);
            Map response = restTemplate.postForObject(ollamaUrl, body, Map.class);
            return String.valueOf(response.get("response")).trim();
        } catch (Exception e) {
            return fallbackReinvestmentAdvice(rmdAmount, age, suggestions);
        }
    }

    private String fallbackReinvestmentAdvice(double rmdAmount, int age, List<Map<String, Object>> suggestions) {
        double totalIncome = suggestions.stream()
            .mapToDouble(s -> (double) s.get("annualIncome")).sum();
        String topProduct = suggestions.isEmpty() ? "High-Yield Savings" :
            String.valueOf(suggestions.get(0).get("name"));

        return String.format(
            "Based on your age (%d) and RMD amount of $%.2f, the agent has allocated your distribution across %d products " +
            "prioritizing capital preservation, tax efficiency, and income generation. " +
            "The top recommendation is %s, which offers the best balance of safety and yield for your profile. " +
            "This reinvestment plan is projected to generate approximately $%.2f in annual income, " +
            "ensuring your RMD continues working for you rather than sitting idle in a checking account.",
            age, rmdAmount, suggestions.size(), topProduct, totalIncome
        );
    }

    private String smartFallbackChat(String question) {
        String q = question.toLowerCase();
        if (q.contains("why") && q.contains("asset")) {
            return "Assets are selected based on a tax-efficiency scoring model: underperformers and over-concentrated positions are liquidated first to minimize your tax burden while satisfying the RMD requirement.";
        }
        if (q.contains("tax")) {
            return "Since IRA withdrawals are treated as ordinary income, the agent selects assets with the highest cost basis first to minimize realized gains. Losses are harvested when available.";
        }
        if (q.contains("in-kind") || q.contains("in_kind") || q.contains("kind")) {
            return "In-Kind transfer moves your shares directly to a taxable brokerage account. You satisfy the RMD without selling, retain market exposure, and only pay tax when you eventually sell those shares.";
        }
        if (q.contains("calculat") || q.contains("how") && q.contains("rmd")) {
            return "Your RMD = Prior year-end IRA balance ÷ IRS Life Expectancy Factor. At age 75 the factor is 24.6, so a $500,000 balance results in an RMD of $20,325.";
        }
        if (q.contains("cash")) {
            return "Cash distribution sells selected assets, transfers the proceeds out of your IRA to your bank account. This is the default method and works best when you need the funds for living expenses.";
        }
        return "I'm your RMD assistant. I can explain asset selection logic, tax impact, distribution options (cash vs in-kind), and how your RMD amount is calculated. What would you like to know?";
    }
}
