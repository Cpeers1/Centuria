package org.asf.centuria.entities.inventoryitems.sanctuaries;

import org.asf.centuria.entities.components.generic.TimeStampComponent;
import org.asf.centuria.entities.components.generic.TradeableComponent;
import org.asf.centuria.entities.components.sanctuaries.HouseComponent;
import org.asf.centuria.entities.components.sanctuaries.IslandComponent;
import org.asf.centuria.entities.inventoryitems.InventoryItem;
import org.asf.centuria.entities.inventoryitems.Item;
import org.asf.centuria.enums.inventory.InventoryType;

import com.google.gson.JsonObject;

@Item
public class IslandItem extends InventoryItem {

	public final static InventoryType INV_TYPE = InventoryType.Island;

	public IslandItem(int defId, String uuid) {
		super(defId, uuid, INV_TYPE);
	}

	public IslandItem() {
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


	public TimeStampComponent getTimeStampComponent() {
		return (TimeStampComponent) this.getComponent(TimeStampComponent.COMPONENT_NAME);
	}
	
	public IslandComponent getIslandComponent() {
		return (IslandComponent) this.getComponent(IslandComponent.COMPONENT_NAME);
	}

	public void setTimeStampComponent(TimeStampComponent component) {
		this.SetComponent(component);
	}
	
	public void setIslandComponent(IslandComponent component) {
		this.SetComponent(component);
	}

}
