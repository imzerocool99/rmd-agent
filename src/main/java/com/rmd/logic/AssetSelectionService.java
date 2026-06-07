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

		// Score assets — losses get high score (preferred), gains get low score
		List<Map<String, Object>> scoredPortfolio = new ArrayList<>();
		for (Map<String, Object> asset : portfolio) {
			double gain = Double.parseDouble(asset.get("gain").toString());
			Map<String, Object> assetCopy = new HashMap<>(asset);
			assetCopy.put("score", -gain);
			scoredPortfolio.add(assetCopy);
		}

		// Split into loss assets and gain assets
		List<Map<String, Object>> lossAssets = new ArrayList<>();
		List<Map<String, Object>> gainAssets = new ArrayList<>();
		for (Map<String, Object> a : scoredPortfolio) {
			double gain = Double.parseDouble(a.get("gain").toString());
			if (gain <= 0) lossAssets.add(a);
			else           gainAssets.add(a);
		}
		// Loss assets: biggest loss first (tax-loss harvesting)
		lossAssets.sort((a, b) -> Double.compare((double) b.get("score"), (double) a.get("score")));
		// Gain assets: highest gain first (best QCD/IK candidates)
		gainAssets.sort((a, b) -> Double.compare(
			Double.parseDouble(b.get("gain").toString()),
			Double.parseDouble(a.get("gain").toString())));

		// Phase 1: loss assets cover 70% of RMD (tax-loss harvesting floor)
		// Phase 2: gain assets cover the remaining 30% (QCD / In-Kind candidates)
		double lossQuota = rmdAmount * 0.70;
		double gainQuota = rmdAmount * 0.30;

		List<Map<String, Object>> selected = new ArrayList<>();
		double lossRemaining = lossQuota;
		double gainRemaining = rmdAmount; // full remaining after losses settle

		// Phase 1 — loss assets
		for (Map<String, Object> asset : lossAssets) {
			if (lossRemaining <= 0) break;
			double price      = Double.parseDouble(asset.get("price").toString());
			int availableQty  = Integer.parseInt(asset.get("qty").toString());
			int qtyToSell     = (int) Math.min(availableQty, Math.floor(lossRemaining / price));
			if (qtyToSell > 0) {
				Map<String, Object> trade = buildTrade(asset, qtyToSell, price);
				selected.add(trade);
				lossRemaining  -= qtyToSell * price;
				gainRemaining  -= qtyToSell * price;
			}
		}

		// Phase 2 — gain assets fill the rest (target gainQuota, but use full remaining)
		double gainFill = Math.max(gainQuota, gainRemaining);
		for (Map<String, Object> asset : gainAssets) {
			if (gainFill <= 0) break;
			double price     = Double.parseDouble(asset.get("price").toString());
			int availableQty = Integer.parseInt(asset.get("qty").toString());
			int qtyToSell    = (int) Math.min(availableQty, Math.floor(gainFill / price));
			if (qtyToSell <= 0) qtyToSell = 1; // always include at least 1 share of top gain assets
			qtyToSell = Math.min(qtyToSell, availableQty);
			if (qtyToSell > 0) {
				Map<String, Object> trade = buildTrade(asset, qtyToSell, price);
				selected.add(trade);
				gainFill -= qtyToSell * price;
			}
		}

		return selected;
	}

	private Map<String, Object> buildTrade(Map<String, Object> asset, int qty, double price) {
		Map<String, Object> trade = new HashMap<>();
		trade.put("symbol",     asset.get("symbol"));
		trade.put("qty",        qty);
		trade.put("price",      price);
		trade.put("value",      qty * price);
		trade.put("gain",       asset.get("gain"));
		trade.put("assetClass", asset.get("assetClass"));
		trade.put("account",    asset.get("account"));
		return trade;
	}
}