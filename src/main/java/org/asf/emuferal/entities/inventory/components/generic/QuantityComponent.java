package org.asf.emuferal.entities.inventory.components.generic;

import org.asf.emuferal.entities.inventory.components.Component;
import org.asf.emuferal.entities.inventory.components.InventoryItemComponent;

import com.google.gson.JsonObject;

/**
 * Generic component for declaring the amount of a stackable item.
 * @author Owenvii
 *
 */
@Component
public class QuantityComponent extends InventoryItemComponent {

	public static String componentName = "Quantity";
	
	private static String quantityPropertyName = "quantity";
	
	public int quantity;
	
	@Override
	public String getComponentName() {
		return componentName;
	}

	@Override
	public JsonObject toJson() {
		
		JsonObject object = new JsonObject();
		object.addProperty(quantityPropertyName, quantity);
		
		return object;
	}

	@Override
	protected void getPropertiesFromJson(JsonObject object) {
		this.quantity = object.get(quantityPropertyName).getAsInt();
	}
	
	public QuantityComponent()
	{
		super();
	}

	public QuantityComponent(int quantity)
	{
		this.quantity = quantity;
	}
	
}
