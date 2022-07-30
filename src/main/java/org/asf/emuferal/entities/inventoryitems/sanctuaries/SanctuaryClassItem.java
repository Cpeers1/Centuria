package org.asf.emuferal.entities.inventoryitems.sanctuaries;

import java.lang.reflect.InvocationTargetException;

import org.asf.emuferal.entities.components.generic.NameComponent;
import org.asf.emuferal.entities.components.generic.TimeStampComponent;
import org.asf.emuferal.entities.components.sanctuaries.PrimaryLookComponent;
import org.asf.emuferal.entities.components.sanctuaries.SanctuaryClassComponent;
import org.asf.emuferal.entities.components.sanctuaries.SanctuaryLookComponent;
import org.asf.emuferal.entities.inventoryitems.InventoryItem;
import org.asf.emuferal.entities.inventoryitems.Item;
import org.asf.emuferal.enums.inventory.InventoryType;

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

	public final static InventoryType INV_TYPE = InventoryType.SanctuaryClasses;

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
