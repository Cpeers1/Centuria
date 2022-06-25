package org.asf.emuferal.entities.inventory.components.generic;

import org.asf.emuferal.entities.inventory.components.Component;
import org.asf.emuferal.entities.inventory.components.InventoryItemComponent;

import com.google.gson.JsonObject;

@Component
public class TradeableComponent extends InventoryItemComponent {
	
	public static String componentName = "Tradable";

	private static String isInTradeListPropertyName = "isInTradeList";

	public boolean isInTradeList;

	@Override
	public String getComponentName() {
		return componentName;
	}

	@Override
	public JsonObject toJson() {

		JsonObject object = new JsonObject();
		object.addProperty(isInTradeListPropertyName, isInTradeList);

		return object;
	}

	@Override
	protected void getPropertiesFromJson(JsonObject object) {
		this.isInTradeList = object.get(isInTradeListPropertyName).getAsBoolean();
	}

	public TradeableComponent()
	{
		super();
	}

	public TradeableComponent(boolean isInTradeList)
	{
		this.isInTradeList = isInTradeList;
	}
}