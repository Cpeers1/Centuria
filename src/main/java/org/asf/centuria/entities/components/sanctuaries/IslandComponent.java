package org.asf.centuria.entities.components.sanctuaries;

import org.asf.centuria.entities.components.Component;
import org.asf.centuria.entities.components.InventoryItemComponent;

import com.google.gson.JsonObject;

@Component
public class IslandComponent extends InventoryItemComponent {

	public static final String COMPONENT_NAME = "Island";

	public static final String GRID_ID_PROPERTY_NAME = "gridId";
	public static final String THEME_DEF_ID_PROPERTY_NAME = "themeDefId";	
	
	public int gridId = 0;
	public int themeDefId = 0;

	
	@Override
	public String getComponentName() {
		return COMPONENT_NAME;
	}

	@Override
	public JsonObject toJson() {
		JsonObject jsonObject = new JsonObject();
		jsonObject.addProperty(GRID_ID_PROPERTY_NAME, gridId);
		jsonObject.addProperty(THEME_DEF_ID_PROPERTY_NAME, themeDefId);
		
		return jsonObject;
	}

	@Override
	protected void getPropertiesFromJson(JsonObject object) {
		gridId = object.get(GRID_ID_PROPERTY_NAME).getAsInt();
		themeDefId = object.get(THEME_DEF_ID_PROPERTY_NAME).getAsInt();
	}

}