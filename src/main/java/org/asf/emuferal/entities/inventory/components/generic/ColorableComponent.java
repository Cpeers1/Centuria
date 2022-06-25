package org.asf.emuferal.entities.inventory.components.generic;

import java.util.ArrayList;
import java.util.List;

import org.asf.emuferal.entities.inventory.components.Component;
import org.asf.emuferal.entities.inventory.components.InventoryItemComponent;

import com.google.gson.JsonObject;

/**
 * Generic component for declaring an item's colorable aspects and set color values in HSV.
 * @author Owenvii
 *
 */
@Component
public class ColorableComponent extends InventoryItemComponent {

	public static String componentName = "Colorable";
	
	public static String colorChannelPropertyNameFormat = "color%sHSV";
	public static String availableChannelsPropertyName = "availableChannels";
	
	public List<ColorChannel> colorChannels = new ArrayList<ColorChannel>();
			
	@Override
	public String getComponentName() {
		return componentName;
	}

	@Override
	public JsonObject toJson() {
		JsonObject newColorableComponent = new JsonObject();
		newColorableComponent.addProperty(availableChannelsPropertyName, colorChannels.size());
		
		//insert color channels
		for(int index = 0; index < colorChannels.size(); index++)
		{
			newColorableComponent.add(String.format(colorChannelPropertyNameFormat, index + 1), colorChannels.get(index).ToJson());
		}
		
		return newColorableComponent;
	}

	@Override
	protected void getPropertiesFromJson(JsonObject object) {
		this.colorChannels = new ArrayList<ColorChannel>(object.get(availableChannelsPropertyName).getAsInt());
		
		var entrySet = object.entrySet();
		
		for (var entry : entrySet)
		{
			if(!entry.getKey().equals(availableChannelsPropertyName))
			{
				colorChannels.add(ColorChannel.fromJson(entry.getValue().getAsJsonObject()));
			}
		}
		
	}

}
