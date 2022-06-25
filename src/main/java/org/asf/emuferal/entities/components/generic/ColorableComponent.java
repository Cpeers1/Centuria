package org.asf.emuferal.entities.components.generic;

import java.util.ArrayList;
import java.util.List;

import org.asf.emuferal.entities.components.Component;
import org.asf.emuferal.entities.components.InventoryItemComponent;
import org.asf.emuferal.entities.generic.ColorChannel;

import com.google.gson.JsonObject;

/**
 * Generic component for declaring an item's colorable aspects and set color values in HSV.
 * @author Owenvii
 *
 */
@Component
public class ColorableComponent extends InventoryItemComponent {

	public static final String COMPONENT_NAME = "Colorable";
	
	public static final String COLOR_CHANNEL_PROPERTY_NAME_FORMAT = "color%sHSV";
	public static final String AVAILABLE_CHANNELS_PROPERTY_NAME = "availableChannels";
	
	public List<ColorChannel> colorChannels = new ArrayList<ColorChannel>();
			
	@Override
	public String getComponentName() {
		return COMPONENT_NAME;
	}

	@Override
	public JsonObject toJson() {
		JsonObject newColorableComponent = new JsonObject();
		newColorableComponent.addProperty(AVAILABLE_CHANNELS_PROPERTY_NAME, colorChannels.size());
		
		//insert color channels
		for(int index = 0; index < colorChannels.size(); index++)
		{
			newColorableComponent.add(String.format(COLOR_CHANNEL_PROPERTY_NAME_FORMAT, index + 1), colorChannels.get(index).ToJson());
		}
		
		return newColorableComponent;
	}

	@Override
	protected void getPropertiesFromJson(JsonObject object) {
		this.colorChannels = new ArrayList<ColorChannel>(object.get(AVAILABLE_CHANNELS_PROPERTY_NAME).getAsInt());
		
		var entrySet = object.entrySet();
		
		for (var entry : entrySet)
		{
			if(!entry.getKey().equals(AVAILABLE_CHANNELS_PROPERTY_NAME))
			{
				colorChannels.add(ColorChannel.fromJson(entry.getValue().getAsJsonObject()));
			}
		}
		
	}

}
