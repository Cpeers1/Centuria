package org.asf.emuferal.entities.twiggles;

import org.asf.emuferal.entities.JsonableObject;

import com.google.gson.JsonObject;

public class TwiggleWorkParameters extends JsonableObject {
	
	public static final String CLASS_ITEM_INV_ID_PROPERTY_KEY = "classItemInvId";
	public static final String ENLARGED_AREA_INDEX_PROPERTY_KEY = "enlargedAreaIndex";
	public static final String STAGE_PROPERTY_KEY = "stage";
	public String classItemInvId = null;
	public Integer enlargedAreaIndex = null;
	public Integer stage = null;

	@Override
	public JsonObject toJson() {
		JsonObject jsonObject = new JsonObject();
		
		if(classItemInvId != null)
			jsonObject.addProperty(CLASS_ITEM_INV_ID_PROPERTY_KEY, classItemInvId);
		
		if(enlargedAreaIndex != null)
			jsonObject.addProperty(ENLARGED_AREA_INDEX_PROPERTY_KEY, enlargedAreaIndex);	
		
		if(stage != null)
			jsonObject.addProperty(STAGE_PROPERTY_KEY, stage);			

		return jsonObject;
	}

	@Override
	protected void propagatePropertiesFromJson(JsonObject jsonObject) {
		//will return null if the member doesn't exist anyway, no need for checks
		this.classItemInvId = jsonObject.get(CLASS_ITEM_INV_ID_PROPERTY_KEY).getAsString();
		this.enlargedAreaIndex = jsonObject.get(ENLARGED_AREA_INDEX_PROPERTY_KEY).getAsInt();
		this.stage = jsonObject.get(STAGE_PROPERTY_KEY).getAsInt();
	}
}
