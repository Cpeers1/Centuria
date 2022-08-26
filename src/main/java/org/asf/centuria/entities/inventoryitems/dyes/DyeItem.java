package org.asf.centuria.entities.inventoryitems.dyes;

import java.lang.reflect.InvocationTargetException;

import org.asf.centuria.entities.components.generic.QuantityComponent;
import org.asf.centuria.entities.components.generic.TimeStampComponent;
import org.asf.centuria.entities.components.generic.TradeableComponent;
import org.asf.centuria.entities.inventoryitems.InventoryItem;
import org.asf.centuria.entities.inventoryitems.Item;
import org.asf.centuria.enums.inventory.InventoryType;

import com.google.gson.JsonObject;

@Item
public class DyeItem extends InventoryItem {

	public final static InventoryType INV_TYPE = InventoryType.Dye;

	public DyeItem(int defId, String uuid) {
		super(defId, uuid, INV_TYPE);
	}

	public DyeItem() {
		super(0, "", INV_TYPE);
	}

	@Override
	public InventoryType getInventoryType() {
		return INV_TYPE;
	}

	/**
	 * Converts this item to a jsonObject with the correct format to be in an
	 * inventory.
	 */
	@Override
	public JsonObject toJsonObject() {
		return super.toJsonObject();
	}

	/**
	 * Populates this item with properties from an inventory jsonObject of the same
	 * type.
	 */
	@Override
	public void fromJsonObject(JsonObject object) throws InstantiationException, IllegalAccessException,
			IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
		super.fromJsonObject(object);
	}

	public TradeableComponent getTradeableComponent() {
		return (TradeableComponent) this.getComponent(TradeableComponent.COMPONENT_NAME);
	}

	public QuantityComponent getQuantityComponent() {
		return (QuantityComponent) this.getComponent(QuantityComponent.COMPONENT_NAME);
	}

	public TimeStampComponent getTimeStampComponent() {
		return (TimeStampComponent) this.getComponent(TimeStampComponent.COMPONENT_NAME);
	}

	public void setTradeableComponent(TradeableComponent component) {
		this.SetComponent(component);
	}

	public void setQuantityComponent(QuantityComponent component) {
		this.SetComponent(component);
	}

	public void setTimeStampComponent(TimeStampComponent component) {
		this.SetComponent(component);
	}

}
