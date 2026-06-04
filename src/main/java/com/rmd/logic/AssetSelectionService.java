package com.rmd.logic;

import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class AssetSelectionService {

	/*
	 * public List<Map<String,Object>> selectAssetsForRMD(Map<String,Object> ctx){
	 * 
	 * List<Map<String,Object>> portfolio = (List<Map<String,Object>>)
	 * ctx.get("portfolio");
	 * 
	 * double rmdAmount = Double.parseDouble(ctx.get("rmdAmount").toString());
	 * 
	 * // ✅ Score assets based on tax + performance for(Map<String,Object> asset :
	 * portfolio){
	 * 
	 * double gain = Double.parseDouble(asset.get("gain").toString());
	 * 
	 * // ✅ scoring logic: // prefer: // - losses (negative gain) // - low gains
	 * double score = -gain;
	 * 
	 * asset.put("score", score); }
	 * 
	 * // ✅ Sort by best candidates (lowest gains first)
	 * portfolio.sort(Comparator.comparingDouble(a -> (double)a.get("score") ));
	 * 
	 * List<Map<String,Object>> selected = new ArrayList<>();
	 * 
	 * double accumulated = 0;
	 * 
	 * for(Map<String,Object> asset : portfolio){
	 * 
	 * selected.add(asset);
	 * 
	 * // ✅ assume simple valuation (qty as proxy) accumulated += Math.abs((double)
	 * asset.get("gain"));
	 * 
	 * if(accumulated >= rmdAmount){ break; } }
	 * 
	 * return selected; }
	 */

	public List<Map<String, Object>> selectAssetsForRMD(Map<String, Object> ctx) {

		List<Map<String, Object>> portfolio = (List<Map<String, Object>>) ctx.get("portfolio");

		double rmdAmount = Double.parseDouble(ctx.get("rmdAmount").toString());

		// ✅ Step 1: score assets without mutating immutable maps
		List<Map<String, Object>> scoredPortfolio = new ArrayList<>();
		for (Map<String, Object> asset : portfolio) {

			double gain = Double.parseDouble(asset.get("gain").toString());

			double score = -gain; // prefer losses
			Map<String, Object> assetCopy = new HashMap<>(asset);
			assetCopy.put("score", score);
			scoredPortfolio.add(assetCopy);
		}

		// Descending — highest score (biggest loss) sold first
		scoredPortfolio.sort((a, b) -> Double.compare((double) b.get("score"), (double) a.get("score")));

		List<Map<String, Object>> selected = new ArrayList<>();

		double remaining = rmdAmount;

		// ✅ Step 2: allocate exact quantities
		for (Map<String, Object> asset : scoredPortfolio) {

			double price = Double.parseDouble(asset.get("price").toString());
			int availableQty = Integer.parseInt(asset.get("qty").toString());

			// ✅ how many shares needed
			int qtyToSell = (int) Math.min(availableQty, Math.floor(remaining / price));

			if (qtyToSell > 0) {

				Map<String, Object> trade = new HashMap<>();
				trade.put("symbol",     asset.get("symbol"));
				trade.put("qty",        qtyToSell);
				trade.put("price",      price);
				trade.put("value",      qtyToSell * price);
				trade.put("gain",       asset.get("gain"));
				trade.put("assetClass", asset.get("assetClass"));
				trade.put("account",    asset.get("account"));

				selected.add(trade);

				remaining -= qtyToSell * price;
			}

			// ✅ stop when RMD met
			if (remaining <= 0) {
				break;
			}
		}

		// ✅ Optional: small remainder handling
		if (remaining > 0) {
			Map<String, Object> fallback = scoredPortfolio.get(0);
			Map<String, Object> fb = new HashMap<>();
			fb.put("symbol",     fallback.get("symbol"));
			fb.put("qty",        1);
			fb.put("price",      fallback.get("price"));
			fb.put("value",      fallback.get("price"));
			fb.put("gain",       fallback.get("gain"));
			fb.put("assetClass", fallback.get("assetClass"));
			fb.put("account",    fallback.get("account"));
			selected.add(fb);
		}

		return selected;
	}
}