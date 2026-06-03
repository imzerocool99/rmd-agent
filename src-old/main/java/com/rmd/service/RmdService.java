
package com.rmd.service;

import com.rmd.model.RequestModel;
import com.rmd.integration.AlpacaClient;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class RmdService {

  private final AlpacaClient client = new AlpacaClient();

  public Map<String,Object> process(RequestModel req) {
    Map<String,Object> res = new HashMap<>();

    String strategy = "recommend";
    if("Cash".equals(req.getPreference())) strategy = "sell";
    if("In-Kind".equals(req.getPreference())) strategy = "transfer";

    res.put("strategy", strategy);

    if(strategy.equals("sell")) {
      res.put("trade", client.placeOrder(req.getSymbol(), req.getQty(), req.getOrderType()));
    }

    res.put("portfolioSize", req.getPortfolio().size());
    return res;
  }
}
