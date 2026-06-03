
package com.rmd.model;
import java.util.*;

public class RequestModel {
  private List<Map<String,Object>> portfolio;
  private String preference;
  private String symbol;
  private int qty;
  private String orderType;

  public List<Map<String,Object>> getPortfolio(){return portfolio;}
  public String getPreference(){return preference;}
  public String getSymbol(){return symbol;}
  public int getQty(){return qty;}
  public String getOrderType(){return orderType;}
}
