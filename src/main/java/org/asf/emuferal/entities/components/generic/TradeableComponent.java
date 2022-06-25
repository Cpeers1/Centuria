package org.asf.emuferal.entities.components.generic;

import org.asf.emuferal.entities.components.Component;
import org.asf.emuferal.entities.components.InventoryItemComponent;

import com.google.gson.JsonObject;

/**
 * Generic component for declaring an item as tradeable, and its tradeable status.
 * @author Owenvii
 *
 */
@Component
public class TradeableComponent extends InventoryItemComponent {
	
	public static final String COMPONENT_NAME = "Tradable";

	private static final String IS_IN_TRADE_LIST_PROPERTY_NAME = "isInTradeList";

	public boolean isInTradeList;

	@Override
	public String getComponentName() {
		return COMPONENT_NAME;
	}

	@Override
	public JsonObject toJson() {

		JsonObject object = new JsonObject();
		object.addProperty(IS_IN_TRADE_LIST_PROPERTY_NAME, isInTradeList);

		return object;
	}

	@Override
	protected void getPropertiesFromJson(JsonObject object) {
		this.isInTradeList = object.get(IS_IN_TRADE_LIST_PROPERTY_NAME).getAsBoolean();
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