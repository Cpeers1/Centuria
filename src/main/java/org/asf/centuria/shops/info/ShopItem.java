package org.asf.centuria.shops.info;

import java.util.HashMap;

public class ShopItem {

	public String objectName;
	public int stock;
	public int requiredLevel;
	public HashMap<String, Integer> items = new HashMap<String, Integer>();
	public HashMap<String, Integer> cost = new HashMap<String, Integer>();
	public HashMap<String, Integer> eurekaItems = new HashMap<String, Integer>();

}
