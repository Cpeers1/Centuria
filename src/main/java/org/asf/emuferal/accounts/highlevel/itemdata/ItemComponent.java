package org.asf.emuferal.accounts.highlevel.itemdata;

import com.google.gson.JsonObject;

public class ItemComponent {

	public String componentName;
	public JsonObject componentData;

	public ItemComponent(String name, JsonObject data) {
		componentName = name;
		componentData = data;
	}

}
