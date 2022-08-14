package org.asf.centuria.shops.info;

import java.util.ArrayList;
import java.util.HashMap;

public class ShopInfo {

	public String object;
	public int restockTime;
	public HashMap<String, ShopItem> contents = new HashMap<String, ShopItem>();
	public ArrayList<String> enigmas = new ArrayList<String>();

}
