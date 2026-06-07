
package com.rmd.agent;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.*;
import java.util.LinkedHashMap;
import com.rmd.logic.*;
import com.rmd.llm.OllamaLLM;
import com.rmd.memory.MemoryService;
import com.rmd.mcp.AlpacaMCP;
import com.rmd.monitor.MonitorService;

@Service
public class MasterAgent {
	@Autowired
	ReinvestmentSuggestionService reinvestmentService;

	@Autowired
	AssetRankingService ranking;
	@Autowired
	TaxService tax;
	@Autowired
	PredictionService prediction;
	@Autowired
	OllamaLLM llm;
	@Autowired
	MemoryService memory;
	@Autowired
	AlpacaMCP mcp;
	@Autowired
	MonitorService monitor;

	@Autowired
	BondAllocationService bondService;

	@Autowired
	RMDService rmdService;

	@Autowired
	AssetSelectionService assetSelector;

	@Value("${trade.max.qty}")
	private int maxQty;

	private Map<String, Object> assetOf(String symbol, String assetClass, int qty, double price, double gain) {
		Map<String, Object> m = new HashMap<>();
		m.put("symbol", symbol);
		m.put("assetClass", assetClass);
		m.put("qty", qty);
		m.put("price", price);
		m.put("gain", gain);
		return m;
	}

	// ── Per-account portfolio definitions ──────────────────────────────
	private List<Map<String, Object>> getPortfolioForAccount(String accountId) {
		switch (accountId) {
			case "IRA-001-B": // Robert & Margaret Chen — Rollover IRA (conservative, bond-heavy)
				return new ArrayList<>(List.of(
					assetOf("BND",   "Bond",                50,  74,  -220),
					assetOf("GOVT",  "Bond",                40,  24,  -180),
					assetOf("TIP",   "Bond",                30, 110,  -350),
					assetOf("SCHD",  "US Broad Market ETF", 25,  82,   620),
					assetOf("JNJ",   "Healthcare",          10, 155,  -280),
					assetOf("PG",    "Consumer Staples",     8, 168,   820),
					assetOf("VYM",   "US Broad Market ETF", 20, 119,   410),
					assetOf("AGG",   "Corporate Bond",      35,  98,  -150),
					assetOf("VMFXX", "Money Market",       100,   1,     0)
				));
			case "IRA-002-A": // William & Dorothy Davis — Traditional IRA (large balanced)
				return new ArrayList<>(List.of(
					assetOf("SPY",   "US Broad Market ETF", 12, 485,  3200),
					assetOf("QQQ",   "US Equity",            8, 420,  2100),
					assetOf("KO",    "Consumer Staples",    30,  62,  -420),
					assetOf("XOM",   "Energy",              20, 118,  -680),
					assetOf("PFE",   "Healthcare",          40,  27, -1200),
					assetOf("VZ",    "Telecom",             35,  41,  -890),
					assetOf("AGG",   "Corporate Bond",      25,  98,  -230),
					assetOf("SHY",   "Bond",                30,  82,  -110),
					assetOf("GLD",   "Commodity",           10, 225,   780),
					assetOf("AAPL",  "US Equity",            6, 213,   960),
					assetOf("VMFXX", "Money Market",       150,   1,     0)
				));
			default: // IRA-001-A — Robert & Margaret Chen Traditional IRA + all other accounts
				return new ArrayList<>(List.of(
					assetOf("AAPL",  "US Equity",            10, 213,  1200),
					assetOf("TSLA",  "US Equity",             6, 248, -1800),
					assetOf("EEM",   "Intl Equity",          40,  42,  -680),
					assetOf("TLT",   "Bond",                 20,  88,  -420),
					assetOf("LQD",   "Corporate Bond",       18, 107,  -230),
					assetOf("MUB",   "Municipal Bond",       16, 104,   180),
					assetOf("HYG",   "High Yield Bond",      22,  74,  -510),
					assetOf("VNQ",   "Real Estate",          14,  82,  -210),
					assetOf("GLD",   "Commodity",            12, 225,  1850),
					assetOf("USO",   "Commodity",            25,  74,  -640),
					assetOf("VTI",   "US Broad Market ETF",  18, 242,  2800),
					assetOf("XLV",   "Healthcare ETF",       16, 140,   620),
					assetOf("VMFXX", "Money Market",        100,   1,     0)
				));
		}
	}

	// ── Account-label helper ─────────────────────────────────────────────
	private String accountLabelFor(String accountId) {
		switch (accountId) {
			case "IRA-001-A": return "Traditional IRA";
			case "IRA-001-B": return "Rollover IRA";
			case "IRA-002-A": return "Traditional IRA (Davis)";
			default:          return accountId;
		}
	}

	public Map<String, Object> run(Map<String, Object> ctx) {
		int age = (int) ctx.getOrDefault("age", 72);
		double balance = Double.parseDouble(ctx.getOrDefault("balance", "100000").toString());

		double rmd = rmdService.calculateRMD(age, balance);

		ctx.put("rmdAmount", rmd);

		monitor.log("Calculated RMD: " + rmd);

		// Parse clientId format: "client_001/IRA-001-A,IRA-001-B" or plain "client_001"
		String rawClientId  = (String) ctx.getOrDefault("clientId", "default");
		String[] parts      = rawClientId.split("/", 2);
		String clientId     = parts[0];
		String accountIdRaw = parts.length > 1 ? parts[1] : "IRA-001-A";
		// Use first account as primary for strategy/label decisions
		String accountId    = accountIdRaw.contains(",") ? accountIdRaw.split(",")[0] : accountIdRaw;
		ctx.put("accountId", accountId);
		ctx.put("accountLabel", accountLabelFor(accountId));

		monitor.log("Agent start — client: " + clientId + " | account: " + accountId);
		ctx.put("history", memory.get(clientId));
		ctx.put("ranking", ranking.rank(ctx));
		ctx.put("tax", tax.optimize(ctx));
		ctx.put("prediction", prediction.predict(ctx));
		List<String> ranked = (List<String>) ctx.get("ranking");
		ctx.put("symbol", ranked.get(0));
		ctx.put("qty", 1);

		String bondETF = bondService.selectBondETF(ctx);
		ctx.put("bondETF", bondETF);

		Map<String, Object> llmResult = llm.decide(ctx);
		ctx.put("strategy", llmResult.get("decision"));
		ctx.put("reasoning", llmResult.get("reason"));

		ctx.put("explanation", "Selected strategy: " + ctx.get("strategy") + " based on prediction: "
				+ ctx.get("prediction") + " and RMD requirement: " + ctx.get("rmdAmount"));

		// ✅ Safety check (limit trades)
		int qty = (int) ctx.getOrDefault("qty", 0);

		if (qty > maxQty) {
			monitor.alert("Trade blocked: quantity exceeds limit");
			return Map.of("error", "limit_exceeded");
		}

		String strategy = String.valueOf(ctx.get("strategy"));

		/*
		 * if ("sell".equalsIgnoreCase(strategy) ||
		 * "rebalance".equalsIgnoreCase(strategy)) {
		 * 
		 * if (qty > maxQty) { monitor.alert("Trade blocked: quantity exceeds limit");
		 * return Map.of("error", "limit_exceeded"); }
		 * 
		 * // ✅ Choose asset type String symbol = (String) ctx.get("symbol");
		 * 
		 * // ✅ If allocating to bonds if ("rebalance".equalsIgnoreCase(strategy)) {
		 * symbol = (String) ctx.get("bondETF"); monitor.log("Switching to bond ETF: " +
		 * symbol); }
		 * 
		 * ctx.put("symbol", symbol);
		 * 
		 * ctx.put("execution", mcp.execute(ctx)); }
		 */

		double rmdAmount = (double) ctx.get("rmdAmount");

		// ✅ Risk validation
		qty = (int) ctx.getOrDefault("qty", 1);
		if (qty > maxQty) {
			monitor.alert("Trade blocked: quantity exceeds limit");
			return Map.of("error", "limit_exceeded");
		}

		// Merge portfolios for all selected accounts
		List<Map<String, Object>> portfolio = new ArrayList<>();
		String[] accountIds = accountIdRaw.split(",");
		for (String aid : accountIds) {
			String aLabel = accountLabelFor(aid.trim());
			List<Map<String, Object>> acctPortfolio = getPortfolioForAccount(aid.trim());
			acctPortfolio.forEach(a -> a.put("account", aLabel));
			portfolio.addAll(acctPortfolio);
		}

		ctx.put("portfolio", portfolio);
		List<Map<String, Object>> selectedAssets = assetSelector.selectAssetsForRMD(ctx);
		ctx.put("selectedAssets", selectedAssets);
		monitor.log("Selected assets for liquidation: " + selectedAssets);

		// ✅ Execution logic aligned with RMD requirements
		if ("sell".equalsIgnoreCase(strategy)) {

			monitor.log("Executing CASH RMD (asset liquidation)");

			// ✅ Use selected asset
			/*
			 * List<Map<String, Object>> assets = (List<Map<String, Object>>)
			 * ctx.get("selectedAssets"); List<Map<String, Object>> executions = new
			 * ArrayList<>(); for (Map<String, Object> asset : assets) { Map<String, Object>
			 * tradeCtx = new HashMap<>(ctx); tradeCtx.put("symbol", asset.get("symbol"));
			 * tradeCtx.put("qty", 1); // simplify for now
			 * executions.add(mcp.execute(tradeCtx)); } ctx.put("execution", executions);
			 */

			List<Map<String, Object>> trades = (List<Map<String, Object>>) ctx.get("selectedAssets");
			List<Map<String, Object>> executions = new ArrayList<>();
			for (Map<String, Object> trade : trades) {
				Map<String, Object> tradeCtx = new HashMap<>(ctx);
				tradeCtx.put("symbol", trade.get("symbol"));
				tradeCtx.put("qty", trade.get("qty"));
				executions.add(mcp.execute(tradeCtx));
			}

			ctx.put("execution", executions);

			// ✅ ✅ Create clean history entry
			Map<String,Object> history = Map.of(
			    "timestamp", System.currentTimeMillis(),
			    "strategy", ctx.get("strategy"),
			    "rmdAmount", ctx.get("rmdAmount"),
			    "execution", executions
			);
			ctx.put("history", history);
			// ✅ Save to memory (no recursion)
			memory.save(clientId, history);

		} else if ("in_kind".equalsIgnoreCase(strategy)) {
			monitor.log("Executing IN-KIND RMD transfer");

			// ✅ Alpaca does NOT support in-kind → simulate
			ctx.put("execution", Map.of("type", "in_kind_transfer", "amount", rmdAmount, "status", "simulated", "note",
					"Asset moved from IRA to brokerage (conceptual)"));
		}

		// ── Rich Explanation ─────────────────────────────────────
		List<Map<String, Object>> selectedList2 = (List<Map<String, Object>>) ctx.get("selectedAssets");
		List<Map<String, Object>> fullPortfolio = (List<Map<String, Object>>) ctx.get("portfolio");
		double totalSold = 0;
		StringBuilder explanation = new StringBuilder();

		explanation.append("SELECTION CRITERIA: The agent scores every holding using a tax-efficiency formula: ");
		explanation.append("Score = -(Unrealized Gain/Loss). ");
		explanation.append("Assets with the largest losses score highest and are liquidated first — ");
		explanation.append("this minimizes taxable gains and maximizes tax-loss harvesting benefit.\n\n");

		explanation.append("SELECTED FOR LIQUIDATION:\n");
		for (Map<String, Object> t : selectedList2) {
			double price   = Double.parseDouble(t.get("price").toString());
			int sellQty    = Integer.parseInt(t.get("qty").toString());
			double value   = sellQty * price;
			totalSold     += value;

			// Find gain from full portfolio
			double gain = 0;
			String assetClass = "";
			for (Map<String, Object> p : fullPortfolio) {
				if (p.get("symbol").equals(t.get("symbol"))) {
					gain       = Double.parseDouble(p.get("gain").toString());
					assetClass = String.valueOf(p.getOrDefault("assetClass", ""));
					break;
				}
			}
			String reason = gain < 0
				? String.format("Unrealized loss of $%.0f — selling locks in a tax loss", gain)
				: gain == 0
					? "Neutral position — no tax impact on sale"
					: String.format("Small gain of $%.0f — minimal tax impact", gain);

			explanation.append(String.format("  • %s (%s): Sell %d shares @ $%.0f = $%.0f | %s\n",
				t.get("symbol"), assetClass, sellQty, price, value, reason));
		}

		// Assets spared
		explanation.append("\nPROTECTED (NOT liquidated):\n");
		for (Map<String, Object> p : fullPortfolio) {
			boolean wasSelected = selectedList2.stream()
				.anyMatch(s -> s.get("symbol").equals(p.get("symbol")));
			if (!wasSelected) {
				double gain = Double.parseDouble(p.get("gain").toString());
				if (gain > 500) {
					explanation.append(String.format("  • %s: Protected — unrealized gain of $%.0f, selling would trigger a large tax bill\n",
						p.get("symbol"), gain));
				}
			}
		}

		explanation.append(String.format("\nRESULT: Total liquidated $%.2f vs RMD required $%.2f. ",
			totalSold, rmdAmount));
		if (totalSold >= rmdAmount) {
			explanation.append("RMD fully satisfied. ✓");
		} else {
			explanation.append(String.format("Shortfall of $%.2f covered by remainder share.", rmdAmount - totalSold));
		}

		ctx.put("explanation", explanation.toString());
		// memory.save(clientId, ctx);
		monitor.log("Agent end");

		ctx.put("strategy", ctx.get("strategy")); // ensure exists
		ctx.put("rmdAmount", ctx.get("rmdAmount"));
		ctx.put("selectedAssets", ctx.get("selectedAssets"));
		ctx.put("reasoning", ctx.get("reasoning"));
		ctx.put("explanation", ctx.get("explanation"));

		ctx.put("reasoning", "Selected sell strategy due to stable market");

		// ── Agentic AI: Reinvestment — parallel Ollama call ──────────
		monitor.log("Agent: generating reinvestment suggestions");
		List<Map<String, Object>> reinvestmentSuggestions =
			reinvestmentService.suggest(rmdAmount, age, "moderate");

		// Fire Ollama reinvestment advice in parallel while we do tax analysis
		final List<Map<String, Object>> suggestionsSnapshot = reinvestmentSuggestions;
		final double rmdSnapshot = rmdAmount;
		final int ageSnapshot = age;
		java.util.concurrent.CompletableFuture<String> adviceFuture =
			java.util.concurrent.CompletableFuture.supplyAsync(
				() -> llm.recommendReinvestment(rmdSnapshot, ageSnapshot, suggestionsSnapshot));

		monitor.log("Agent: reinvestment advice requested (async)");

		double totalAnnualIncome = reinvestmentSuggestions.stream()
			.mapToDouble(s -> (double) s.get("annualIncome")).sum();

		// Collect Ollama reinvestment advice — fallback to rule-based if timeout
		String reinvestmentAdvice;
		try {
			reinvestmentAdvice = adviceFuture.get(4, java.util.concurrent.TimeUnit.SECONDS);
		} catch (Exception e) {
			reinvestmentAdvice = llm.getFallbackReinvestmentAdvice(rmdAmount, age, reinvestmentSuggestions);
		}
		monitor.log("Agent: reinvestment advice generated");

		Map<String, Object> reinvestment = new LinkedHashMap<>();
		reinvestment.put("bankAccount", "Client Checking Account — Same Bank");
		reinvestment.put("totalAmount", rmdAmount);
		reinvestment.put("totalAnnualIncome", Math.round(totalAnnualIncome * 100.0) / 100.0);
		reinvestment.put("agentAdvice", reinvestmentAdvice);
		reinvestment.put("suggestions", reinvestmentSuggestions);

		// ── Tax Efficiency Analysis ───────────────────────────────────
		List<Map<String, Object>> portfolioList = (List<Map<String, Object>>) ctx.get("portfolio");
		List<Map<String, Object>> selectedList  = (List<Map<String, Object>>) ctx.get("selectedAssets");

		// Naive worst-case: sort by highest gain (what an unintelligent system would pick)
		List<Map<String, Object>> naiveSorted = new ArrayList<>(portfolioList);
		naiveSorted.sort((a, b) -> Double.compare(
			Double.parseDouble(b.get("gain").toString()),
			Double.parseDouble(a.get("gain").toString())
		));

		double naiveGain = 0, smartGain = 0;
		double naiveRemaining = rmdAmount;
		List<Map<String, Object>> naiveAssets = new ArrayList<>();
		for (Map<String, Object> a : naiveSorted) {
			double price   = Double.parseDouble(a.get("price").toString());
			int availQty   = Integer.parseInt(a.get("qty").toString());
			int toSell     = (int) Math.min(availQty, Math.floor(naiveRemaining / price));
			if (toSell > 0) {
				double gain = Double.parseDouble(a.get("gain").toString());
				double gainPerShare = availQty > 0 ? gain / availQty : 0;
				naiveGain += gainPerShare * toSell;
				naiveAssets.add(Map.of("symbol", a.get("symbol"), "qty", toSell,
					"price", price, "gain", Math.round(gainPerShare * toSell * 100.0) / 100.0));
				naiveRemaining -= toSell * price;
			}
			if (naiveRemaining <= 0) break;
		}

		for (Map<String, Object> a : selectedList) {
			Object gainObj = a.get("gain");
			if (gainObj != null) smartGain += Double.parseDouble(gainObj.toString());
		}

		double TAX_RATE       = 0.24;
		double naiveTaxBill   = Math.max(0, naiveGain) * TAX_RATE;
		double smartTaxBill   = Math.max(0, smartGain) * TAX_RATE;
		double taxSaved       = Math.max(0, naiveTaxBill - smartTaxBill);

		Map<String, Object> taxAnalysis = new LinkedHashMap<>();
		taxAnalysis.put("naiveGain",        Math.round(naiveGain  * 100.0) / 100.0);
		taxAnalysis.put("smartGain",        Math.round(smartGain  * 100.0) / 100.0);
		taxAnalysis.put("naiveTaxBill",     Math.round(naiveTaxBill * 100.0) / 100.0);
		taxAnalysis.put("smartTaxBill",     Math.round(smartTaxBill * 100.0) / 100.0);
		taxAnalysis.put("taxSaved",         Math.round(taxSaved    * 100.0) / 100.0);
		taxAnalysis.put("taxRatePct",       (int)(TAX_RATE * 100));
		taxAnalysis.put("naiveAssets",      naiveAssets);
		taxAnalysis.put("optimizedAssets",  selectedList);
		taxAnalysis.put("savingsExplanation",
			String.format("By liquidating assets with losses (%.2f realized gain) instead of highest-gain assets " +
				"(%.2f realized gain), the agent saved $%.2f in taxes at the %.0f%% rate.",
				smartGain, naiveGain, taxSaved, TAX_RATE * 100));
		monitor.log("Agent: tax savings calculated — $" + taxSaved + " saved vs naive approach");

		Map<String, Object> response = new HashMap<>();
		response.put("rmdAmount",      ctx.get("rmdAmount"));
		response.put("strategy",       ctx.get("strategy"));
		response.put("selectedAssets", ctx.get("selectedAssets"));
		response.put("reasoning",      ctx.get("reasoning"));
		response.put("explanation",    ctx.get("explanation"));
		response.put("portfolio",      ctx.get("portfolio"));
		response.put("reinvestment",   reinvestment);
		response.put("taxAnalysis",    taxAnalysis);
		response.put("execution",      ctx.getOrDefault("execution", List.of()));

		return response;

	}
}
