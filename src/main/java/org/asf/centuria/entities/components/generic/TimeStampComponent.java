package org.asf.centuria.entities.components.generic;

import org.asf.centuria.entities.components.Component;
import org.asf.centuria.entities.components.InventoryItemComponent;

import com.google.gson.JsonObject;

/**
 * Generic component for time stamping items.
 * 
 * @author Owenvii
 *
 */
@Component
public class TimeStampComponent extends InventoryItemComponent {

	public static final String COMPONENT_NAME = "Timestamp";

	private static final String TIME_STAMP_PROPERTY_NAME = "ts";

	public long timeStamp;

	@Override
	public String getComponentName() {
		return COMPONENT_NAME;
	}

	@Override
	public JsonObject toJson() {

		JsonObject object = new JsonObject();
		object.addProperty(TIME_STAMP_PROPERTY_NAME, timeStamp);

		return object;
	}

	@Override
	protected void getPropertiesFromJson(JsonObject object) {
		// TODO Auto-generated method stub
		this.timeStamp = object.get(TIME_STAMP_PROPERTY_NAME).getAsLong();
	}

	public TimeStampComponent() {
		super();
	}

	public TimeStampComponent(long timeStamp) {
		this.timeStamp = timeStamp;
	}
	
	public void stamp()
	{
		this.timeStamp = System.currentTimeMillis();
	}

}
