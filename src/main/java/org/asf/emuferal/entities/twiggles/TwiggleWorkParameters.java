package org.asf.emuferal.entities.twiggles;

import org.asf.emuferal.entities.JsonableObject;

import com.google.gson.JsonObject;

public class TwiggleWorkParameters extends JsonableObject {
	
	public static final String CLASS_ITEM_INV_ID_PROPERTY_KEY = "classItemInvId";
	public String classItemInvId = "";

	@Override
	public JsonObject toJson() {
		JsonObject jsonObject = new JsonObject();
		jsonObject.addProperty(CLASS_ITEM_INV_ID_PROPERTY_KEY, classItemInvId);
		return jsonObject;
	}

	@Override
	protected void propagatePropertiesFromJson(JsonObject jsonObject) {
		this.classItemInvId = jsonObject.get(CLASS_ITEM_INV_ID_PROPERTY_KEY).getAsString();
	}
}
