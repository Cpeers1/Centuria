package org.asf.emuferal.entities.inventoryitems.twiggles;

import java.lang.reflect.InvocationTargetException;

import org.asf.emuferal.entities.components.generic.NameComponent;
import org.asf.emuferal.entities.components.generic.TimeStampComponent;
import org.asf.emuferal.entities.components.sanctuaries.PrimaryLookComponent;
import org.asf.emuferal.entities.components.sanctuaries.SanctuaryLookComponent;
import org.asf.emuferal.entities.components.twiggles.TwiggleComponent;
import org.asf.emuferal.entities.inventoryitems.InventoryItem;
import org.asf.emuferal.entities.inventoryitems.Item;
import org.asf.emuferal.enums.inventory.InventoryType;

import com.google.gson.JsonObject;

@Item
public class TwiggleItem extends InventoryItem {

	public final static InventoryType INV_TYPE = InventoryType.Twiggles;
	
	public TwiggleItem(int defId, String uuid) {
		super(defId, uuid, INV_TYPE);
	}
	
	public TwiggleItem()
	{
		super(0, "", INV_TYPE);
	}

	@Override
	public InventoryType getInventoryType() {
		return INV_TYPE;
	}
	
	/**
	 * Converts this item to a jsonObject with the correct format to be in an inventory.
	 */
	public JsonObject toJsonObject() {
		return super.toJsonObject();
	}

	/**
	 * Populates this item with properties from an inventory jsonObject of the same type.
	 */
	public void fromJsonObject(JsonObject object) throws InstantiationException, IllegalAccessException,
			IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
		super.fromJsonObject(object);
	}
	
	public TwiggleComponent getTwiggleItem()
	{
		return (TwiggleComponent)this.getComponent(TwiggleComponent.COMPONENT_NAME);
	}
	
	public void setTwiggleComponent(TwiggleComponent component)
	{
		this.SetComponent(component);
	}
	
	public TimeStampComponent getTimeStampComponent()
	{
		return (TimeStampComponent)this.getComponent(TimeStampComponent.COMPONENT_NAME);
	}
	
	public void setTimeStampComponent(TimeStampComponent component)
	{
		this.SetComponent(component);
	}

}
