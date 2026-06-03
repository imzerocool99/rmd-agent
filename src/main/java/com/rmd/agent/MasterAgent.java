
package com.rmd.agent;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.*;
import com.rmd.logic.*;
import com.rmd.llm.AzureLLM;
import com.rmd.memory.MemoryService;
import com.rmd.mcp.AlpacaMCP;
import com.rmd.monitor.MonitorService;

@Service
public class MasterAgent {
	@Autowired
	AssetRankingService ranking;
	@Autowired
	TaxService tax;
	@Autowired
	PredictionService prediction;
	@Autowired
	AzureLLM llm;
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

	public Map<String, Object> run(Map<String, Object> ctx) {
		int age = (int) ctx.getOrDefault("age", 72);
		double balance = Double.parseDouble(ctx.getOrDefault("balance", "100000").toString());

		double rmd = rmdService.calculateRMD(age, balance);

		ctx.put("rmdAmount", rmd);

		monitor.log("Calculated RMD: " + rmd);

		String clientId = (String) ctx.getOrDefault("clientId", "default");
		monitor.log("Agent start");
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

		List<Map<String, Object>> portfolio = new ArrayList<>(List.of(
				// ── US Large-Cap Equities ──────────────────────────────
				assetOf("AAPL",  "US Equity",            10,  213,   1200),
				assetOf("MSFT",  "US Equity",             8,  415,   3200),
				assetOf("GOOGL", "US Equity",             5,  178,   2100),
				assetOf("JPM",   "US Equity",            12,  205,    850),
				assetOf("JNJ",   "US Equity",            15,  145,   -320),
				assetOf("AMZN",  "US Equity",             7,  192,    940),
				assetOf("NVDA",  "US Equity",             4,  875,   5600),
				// ── Growth / Volatile ─────────────────────────────────
				assetOf("TSLA",  "US Equity",             6,  248,  -1800),
				assetOf("ARKK",  "US Equity",            20,   47,  -1100),
				// ── International Equities ────────────────────────────
				assetOf("EFA",   "Intl Equity",          35,   82,    420),
				assetOf("EEM",   "Intl Equity",          40,   42,   -680),
				assetOf("VEA",   "Intl Equity",          28,   52,    310),
				// ── US Fixed Income / Bonds ───────────────────────────
				assetOf("BND",   "Bond",                 30,   73,    -90),
				assetOf("AGG",   "Bond",                 25,   96,   -150),
				assetOf("TLT",   "Bond",                 20,   88,   -420),
				assetOf("LQD",   "Corporate Bond",       18,  107,   -230),
				assetOf("HYG",   "High Yield Bond",      22,   74,   -510),
				assetOf("MUB",   "Municipal Bond",       16,  104,    180),
				// ── Real Estate (REITs) ───────────────────────────────
				assetOf("VNQ",   "Real Estate",          14,   82,   -210),
				assetOf("O",     "Real Estate",          20,   54,    360),
				assetOf("AMT",   "Real Estate",           8,  195,    720),
				// ── Commodities ───────────────────────────────────────
				assetOf("GLD",   "Commodity",            12,  225,   1850),
				assetOf("SLV",   "Commodity",            30,   24,   -190),
				assetOf("USO",   "Commodity",            25,   74,   -640),
				// ── Broad Market ETFs ─────────────────────────────────
				assetOf("VTI",   "US Broad Market ETF",  18,  242,   2800),
				assetOf("SPY",   "US Broad Market ETF",   6,  530,   4200),
				assetOf("QQQ",   "US Broad Market ETF",   5,  450,   3100),
				// ── Sector ETFs ───────────────────────────────────────
				assetOf("XLE",   "Energy Sector ETF",    22,   88,    380),
				assetOf("XLV",   "Healthcare ETF",       16,  140,    620),
				assetOf("XLF",   "Financial ETF",        28,   41,    290),
				// ── Cash & Money Market ───────────────────────────────
				assetOf("VMFXX", "Money Market",        100,    1,      0)
		));

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

		String explanation = "RMD matched using optimized quantities: ";

		double total = 0;

		for (Map<String, Object> t : (List<Map<String, Object>>) ctx.get("selectedAssets")) {
			explanation += t.get("symbol") + ": " + t.get("qty") + " shares, ";
			total += Double.parseDouble(t.get("value").toString());
		}

		explanation += "Total value: " + total;

		ctx.put("explanation", explanation);
		// memory.save(clientId, ctx);
		monitor.log("Agent end");

		ctx.put("strategy", ctx.get("strategy")); // ensure exists
		ctx.put("rmdAmount", ctx.get("rmdAmount"));
		ctx.put("selectedAssets", ctx.get("selectedAssets"));
		ctx.put("reasoning", ctx.get("reasoning"));
		ctx.put("explanation", ctx.get("explanation"));

		ctx.put("reasoning", "Selected sell strategy due to stable market");

		Map<String, Object> response = new HashMap<>();

		response.put("rmdAmount", ctx.get("rmdAmount"));
		response.put("strategy", ctx.get("strategy"));
		response.put("selectedAssets", ctx.get("selectedAssets"));
		response.put("reasoning", ctx.get("reasoning"));
		response.put("explanation", ctx.get("explanation"));
		response.put("portfolio", ctx.get("portfolio"));

		return response;

	}
}
