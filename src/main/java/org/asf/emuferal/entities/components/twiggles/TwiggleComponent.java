package org.asf.emuferal.entities.components.twiggles;

import org.asf.emuferal.entities.components.Component;
import org.asf.emuferal.entities.components.InventoryItemComponent;
import org.asf.emuferal.entities.twiggles.TwiggleWorkParameters;
import org.asf.emuferal.enums.twiggles.TwiggleState;

import com.google.gson.JsonObject;

/**
 * A component that represents a player's helper, a 'twiggle'.
 * @author Owenvii
 *
 */
@Component
public class TwiggleComponent extends InventoryItemComponent {

	public static final String COMPONENT_NAME = "Twiggle";
	
	public static final String WORK_TYPE_PROPERTY_NAME = "workType";
	public static final String END_WORK_TIME_PROPERTY_NAME = "endWorkTime";
	public static final String WORK_PARAMS_PROPERTY_NAME = "workParams";
	
	public TwiggleState workType = TwiggleState.None; //TODO: Make a enum for this (I don't think even WW did)
	public long workEndTime = 0;
	public TwiggleWorkParameters twiggleWorkParams = null;
	
	@Override
	public String getComponentName() {
		return COMPONENT_NAME;
	}

	@Override
	public JsonObject toJson() {
		JsonObject jsonObject = new JsonObject();
		jsonObject.addProperty(WORK_TYPE_PROPERTY_NAME, workType.value);
		jsonObject.addProperty(END_WORK_TIME_PROPERTY_NAME, workEndTime);
		
		if(twiggleWorkParams != null)
			jsonObject.add(WORK_PARAMS_PROPERTY_NAME, twiggleWorkParams.toJson());

		return jsonObject;
	}

	@Override
	protected void getPropertiesFromJson(JsonObject object) {
		this.workType = TwiggleState.get(object.get(WORK_TYPE_PROPERTY_NAME).getAsInt());
		this.workEndTime = object.get(END_WORK_TIME_PROPERTY_NAME).getAsLong();
		
		if(object.has(WORK_PARAMS_PROPERTY_NAME))
		{
			try 
			{
				this.twiggleWorkParams = (TwiggleWorkParameters)new TwiggleWorkParameters().CreateObjectFromJson(object.get(WORK_PARAMS_PROPERTY_NAME).getAsJsonObject());
			}
			catch(Exception e)
			{
				throw new RuntimeException(e);
			}
		}

	}

}
