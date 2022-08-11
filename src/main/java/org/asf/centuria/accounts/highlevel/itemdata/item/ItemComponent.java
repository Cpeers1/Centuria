package org.asf.centuria.accounts.highlevel.itemdata.item;

import com.google.gson.JsonObject;

public class ItemComponent {

	public String componentName;
	public JsonObject componentData;

	public ItemComponent(String name, JsonObject data) {
		componentName = name;
		componentData = data;
	}

}
