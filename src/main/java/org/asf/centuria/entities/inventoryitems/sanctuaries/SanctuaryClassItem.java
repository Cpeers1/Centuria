package org.asf.centuria.entities.inventoryitems.sanctuaries;

import java.lang.reflect.InvocationTargetException;

import org.asf.centuria.entities.components.generic.NameComponent;
import org.asf.centuria.entities.components.generic.TimeStampComponent;
import org.asf.centuria.entities.components.sanctuaries.PrimaryLookComponent;
import org.asf.centuria.entities.components.sanctuaries.SanctuaryClassComponent;
import org.asf.centuria.entities.components.sanctuaries.SanctuaryLookComponent;
import org.asf.centuria.entities.inventoryitems.InventoryItem;
import org.asf.centuria.entities.inventoryitems.Item;
import org.asf.centuria.enums.inventory.InventoryType;

import com.google.gson.JsonObject;

/**
 * A sanctuary class item. Used by the game to store data about sanctuary
 * classes.
 * 
 * @author Owenvii
 *
 */
@Item
public class SanctuaryClassItem extends InventoryItem {

	public final static InventoryType INV_TYPE = InventoryType.SanctuaryClass;

	public SanctuaryClassItem(int defId, String uuid) {
		super(defId, uuid, INV_TYPE);
	}

	public SanctuaryClassItem() {
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
	public JsonObject toJsonObject() {
		return super.toJsonObject();
	}

	/**
	 * Populates this item with properties from an inventory jsonObject of the same
	 * type.
	 */
	public void fromJsonObject(JsonObject object) throws InstantiationException, IllegalAccessException,
			IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
		super.fromJsonObject(object);
	}

	public SanctuaryClassComponent getSanctuaryClassComponent() {
		return (SanctuaryClassComponent) this.getComponent(SanctuaryClassComponent.COMPONENT_NAME);
	}

	public TimeStampComponent getTimeStampComponent() {
		return (TimeStampComponent) this.getComponent(TimeStampComponent.COMPONENT_NAME);
	}

	public void setSanctuaryClassComponent(SanctuaryClassComponent component) {
		this.SetComponent(component);
	}

	public void setTimeStampComponent(TimeStampComponent component) {
		this.SetComponent(component);
	}

}
