package org.asf.emuferal.entities.inventory.components.generic;

import org.asf.emuferal.entities.inventory.components.Component;
import org.asf.emuferal.entities.inventory.components.InventoryItemComponent;

import com.google.gson.JsonObject;

@Component
public class TimeStampComponent extends InventoryItemComponent {

	public static String componentName = "Timestamp";
	
	private static String timeStampPropertyName = "ts";
	
	public String timeStamp;
	
	@Override
	public String getComponentName() {
		return componentName;
	}

	@Override
	public JsonObject toJson() {
		
		JsonObject object = new JsonObject();
		object.addProperty(timeStampPropertyName, timeStamp);
		
		return object;
	}

	@Override
	protected void getPropertiesFromJson(JsonObject object) {
		// TODO Auto-generated method stub
		this.timeStamp = object.get(timeStampPropertyName).getAsString();
	}
	
	public TimeStampComponent()
	{
		super();
	}

	public TimeStampComponent(String timeStamp)
	{
		this.timeStamp = timeStamp;
	}
	
}
