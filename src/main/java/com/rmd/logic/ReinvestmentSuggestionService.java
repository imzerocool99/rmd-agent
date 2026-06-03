package com.rmd.logic;

import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class ReinvestmentSuggestionService {

    public List<Map<String, Object>> suggest(double rmdAmount, int age, String riskProfile) {

        List<Map<String, Object>> products = new ArrayList<>();

        // ── Allocations based on age & risk ──────────────────────────
        boolean conservative = age >= 78 || "conservative".equalsIgnoreCase(riskProfile);
        boolean moderate     = age >= 73 && age < 78 || "moderate".equalsIgnoreCase(riskProfile);

        // 1. High-Yield Savings Account
        products.add(product(
            "High-Yield Savings Account",
            "Cash / Savings",
            conservative ? 0.30 : 0.20,
            4.50, "Low", "Immediate",
            "FDIC insured up to $250K. Best for emergency liquidity. No lock-in period.",
            "Same Bank", rmdAmount
        ));

        // 2. Certificates of Deposit (CDs)
        products.add(product(
            "Certificate of Deposit (CD)",
            "Fixed Income",
            conservative ? 0.25 : 0.15,
            4.85, "Low", "Low (locked term)",
            "12-month CD. FDIC insured. Fixed rate locked in for the term. Early withdrawal penalty applies.",
            "Same Bank", rmdAmount
        ));

        // 3. Treasury Bills
        products.add(product(
            "US Treasury Bills (T-Bills)",
            "Government Securities",
            conservative ? 0.20 : 0.15,
            4.20, "Very Low", "High (3-12 months)",
            "Backed by the US government. Exempt from state and local tax. Ideal for capital preservation.",
            "Brokerage Account", rmdAmount
        ));

        // 4. Municipal Bonds
        products.add(product(
            "Municipal Bond Fund (MUB)",
            "Tax-Advantaged Fixed Income",
            moderate || conservative ? 0.15 : 0.10,
            3.80, "Low-Medium", "Medium",
            "Interest is federally tax-free. Excellent for clients in higher tax brackets. State tax-free if in-state bonds.",
            "Brokerage Account", rmdAmount
        ));

        // 5. Dividend ETF
        products.add(product(
            "Dividend ETF (SCHD / VYM)",
            "Equity Income",
            conservative ? 0.05 : 0.20,
            3.50, "Medium", "High",
            "Diversified dividend-paying stocks. Combines income yield with long-term growth potential. Taxable dividends.",
            "Brokerage Account", rmdAmount
        ));

        // 6. Fixed Annuity
        if (age >= 72) {
            products.add(product(
                "Fixed Annuity",
                "Insurance / Guaranteed Income",
                conservative ? 0.05 : 0.10,
                5.20, "Very Low", "Low (surrender period)",
                "Guaranteed interest rate. Tax-deferred growth. Option to convert to lifetime income stream. Ideal for longevity protection.",
                "Insurance Product", rmdAmount
            ));
        }

        // 7. Money Market Fund
        products.add(product(
            "Money Market Fund (VMFXX)",
            "Cash Equivalent",
            conservative ? 0.05 : 0.10,
            4.10, "Very Low", "Immediate",
            "Near-cash stability with better yield than checking accounts. Invests in short-term government and corporate debt.",
            "Brokerage Account", rmdAmount
        ));

        // Normalize allocations to exactly 100%
        double total = products.stream().mapToDouble(p -> (double) p.get("allocationPct")).sum();
        for (Map<String, Object> p : products) {
            double pct = (double) p.get("allocationPct") / total;
            double amt = Math.round(rmdAmount * pct * 100.0) / 100.0;
            p.put("allocationPct", Math.round(pct * 1000.0) / 10.0);
            p.put("allocationAmount", amt);
            p.put("annualIncome", Math.round(amt * (double) p.get("yieldPct") / 100.0 * 100.0) / 100.0);
        }

        return products;
    }

    private Map<String, Object> product(String name, String type, double allocationPct,
                                         double yieldPct, String risk, String liquidity,
                                         String description, String account, double rmdAmount) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("name", name);
        p.put("type", type);
        p.put("allocationPct", allocationPct);
        p.put("yieldPct", yieldPct);
        p.put("risk", risk);
        p.put("liquidity", liquidity);
        p.put("description", description);
        p.put("account", account);
        p.put("allocationAmount", 0.0);
        p.put("annualIncome", 0.0);
        return p;
    }
}
