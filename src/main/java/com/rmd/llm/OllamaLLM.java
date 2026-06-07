package com.rmd.llm;

import com.rmd.admin.LlmSettings;
import com.rmd.rag.RagService;
import java.util.List;
import java.util.Map;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class OllamaLLM {

    private final LlmSettings settings;
    private final RagService ragService;
    private final RestTemplate restTemplate = buildRestTemplate(5_000); // 5s — fall back fast if Ollama is slow

    public OllamaLLM(LlmSettings settings, RagService ragService) {
        this.settings = settings;
        this.ragService = ragService;
    }

    // ── STRATEGY DECISION ─────────────────────────────────────────────────────

    public Map<String, Object> decide(Map<String, Object> ctx) {
        String preference = String.valueOf(ctx.getOrDefault("preference", "sell"));
        if ("in_kind".equalsIgnoreCase(preference)) {
            return Map.of("decision", "in_kind",
                "reason", "Client preference: In-Kind Transfer — shares moved directly to taxable brokerage, preserving full market exposure without liquidation.");
        }
        return fallback(String.valueOf(ctx.get("prediction")));
    }

    // ── RAG-AUGMENTED CHAT ────────────────────────────────────────────────────

    public String chat(String userMessage, String context) {
        String ragContext = ragService.retrieveAsContext(userMessage);

        String prompt = String.format(
            "You are a helpful IRA RMD financial assistant.\n\n" +
            "%s\n\n" +
            "Client context: %s\n\n" +
            "Answer clearly and concisely in 2-3 sentences: %s",
            ragContext.isEmpty() ? "" : ragContext + "\n",
            context, userMessage
        );

        try {
            Map<String, Object> body = buildOllamaBody(prompt);
            Map response = restTemplate.postForObject(settings.getOllamaUrl(), body, Map.class);
            return String.valueOf(response.get("response")).trim();
        } catch (Exception e) {
            return smartFallbackChat(userMessage);
        }
    }

    // ── RAG-AUGMENTED REINVESTMENT ADVICE ─────────────────────────────────────

    public String recommendReinvestment(double rmdAmount, int age, List<Map<String, Object>> suggestions) {
        StringBuilder products = new StringBuilder();
        for (Map<String, Object> s : suggestions) {
            products.append(String.format("%s %.0f%% $%.0f yield %.1f%%, ",
                s.get("name"), s.get("allocationPct"), s.get("allocationAmount"), s.get("yieldPct")));
        }

        String ragContext = ragService.retrieveAsContext(
            "reinvestment strategy age " + age + " RMD distribution products yield income");

        String prompt = String.format(
            "You are a financial advisor assistant.\n\n" +
            "%s\n\n" +
            "Client age %d, RMD amount $%.0f reinvested across: %s\n\n" +
            "In 2-3 sentences: why this product mix suits the client and the projected income benefit.",
            ragContext.isEmpty() ? "" : ragContext,
            age, rmdAmount, products
        );

        try {
            Map<String, Object> body = buildOllamaBody(prompt);
            Map response = restTemplate.postForObject(settings.getOllamaUrl(), body, Map.class);
            String text = String.valueOf(response.get("response")).trim();
            return text.isEmpty() ? fallbackReinvestmentAdvice(rmdAmount, age, suggestions) : text;
        } catch (Exception e) {
            return fallbackReinvestmentAdvice(rmdAmount, age, suggestions);
        }
    }

    public String getFallbackReinvestmentAdvice(double rmdAmount, int age, List<Map<String, Object>> suggestions) {
        return fallbackReinvestmentAdvice(rmdAmount, age, suggestions);
    }

    // ── INTERNAL HELPERS ──────────────────────────────────────────────────────

    private Map<String, Object> buildOllamaBody(String prompt) {
        return Map.of(
            "model",   settings.getModel(),
            "prompt",  prompt,
            "stream",  false,
            "options", Map.of(
                "temperature", settings.getTemperature(),
                "top_p",       settings.getTopP(),
                "num_predict", settings.getNumPredict()
            )
        );
    }

    private RestTemplate buildRestTemplate(int timeoutMs) {
        SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(timeoutMs);
        f.setReadTimeout(timeoutMs);
        return new RestTemplate(f);
    }

    private Map<String, Object> fallback(String prediction) {
        if ("volatile".equalsIgnoreCase(prediction)) {
            return Map.of("decision", "in_kind",
                "reason", "Market volatility detected — preserving investments via in-kind transfer to avoid selling at a loss.");
        }
        return Map.of("decision", "sell",
            "reason", "Stable market conditions — generating cash by liquidating underperforming and over-concentrated assets.");
    }

    private String extractReason(String text) {
        int dash = text.indexOf('-');
        if (dash >= 0 && dash < text.length() - 1) return text.substring(dash + 1).trim();
        return text.length() > 80 ? text.substring(0, 80) + "..." : text;
    }

    private String fallbackReinvestmentAdvice(double rmdAmount, int age, List<Map<String, Object>> suggestions) {
        double totalIncome = suggestions.stream().mapToDouble(s -> (double) s.get("annualIncome")).sum();
        String topProduct = suggestions.isEmpty() ? "High-Yield Savings" : String.valueOf(suggestions.get(0).get("name"));
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
        if (q.contains("why") && q.contains("asset"))
            return "Assets are selected based on a tax-efficiency scoring model: underperformers and over-concentrated positions are liquidated first to minimize your tax burden while satisfying the RMD requirement.";
        if (q.contains("tax"))
            return "Since IRA withdrawals are treated as ordinary income, the agent selects assets with the highest cost basis first to minimize realized gains. Losses are harvested when available.";
        if (q.contains("in-kind") || q.contains("in_kind") || q.contains("kind"))
            return "In-Kind transfer moves your shares directly to a taxable brokerage account. You satisfy the RMD without selling, retain market exposure, and only pay tax when you eventually sell those shares.";
        if (q.contains("calculat") || (q.contains("how") && q.contains("rmd")))
            return "Your RMD = Prior year-end IRA balance ÷ IRS Life Expectancy Factor. At age 75 the factor is 24.6, so a $500,000 balance results in an RMD of $20,325.";
        if (q.contains("cash"))
            return "Cash distribution sells selected assets, transfers the proceeds out of your IRA to your bank account. This is the default method and works best when you need the funds for living expenses.";
        return "I'm your RMD assistant. I can explain asset selection logic, tax impact, distribution options (cash vs in-kind), and how your RMD amount is calculated. What would you like to know?";
    }
}
