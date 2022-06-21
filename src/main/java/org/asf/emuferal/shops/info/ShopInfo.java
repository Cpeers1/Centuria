package org.asf.emuferal.shops.info;

import java.util.HashMap;

public class ShopInfo {

	public String object;
	public int restockTime;
	public HashMap<String, ShopItem> contents = new HashMap<String, ShopItem>();

}
