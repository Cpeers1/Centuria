package org.asf.centuria.entities.inventoryitems.sanctuaries;

import java.lang.reflect.InvocationTargetException;

import org.asf.centuria.entities.components.generic.NameComponent;
import org.asf.centuria.entities.components.generic.TimeStampComponent;
import org.asf.centuria.entities.components.sanctuaries.PrimaryLookComponent;
import org.asf.centuria.entities.components.sanctuaries.SanctuaryLookComponent;
import org.asf.centuria.entities.inventoryitems.InventoryItem;
import org.asf.centuria.entities.inventoryitems.Item;
import org.asf.centuria.enums.inventory.InventoryType;

import com.google.gson.JsonObject;

/**
 * A sanctuary look item. Used by the game to store data about sanctuary looks.
 * 
 * @author Owenvii
 *
 */
@Item
public class SanctuaryLookItem extends InventoryItem {

	public final static InventoryType INV_TYPE = InventoryType.SanctuaryLooks;

	public SanctuaryLookItem(int defId, String uuid) {
		super(defId, uuid, INV_TYPE);
	}

	public SanctuaryLookItem() {
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

	public PrimaryLookComponent getPrimaryLookComponent() {
		return (PrimaryLookComponent) this.getComponent(PrimaryLookComponent.COMPONENT_NAME);
	}

	public SanctuaryLookComponent getSanctuaryLookComponent() {
		return (SanctuaryLookComponent) this.getComponent(SanctuaryLookComponent.COMPONENT_NAME);
	}

	public TimeStampComponent getTimeStampComponent() {
		return (TimeStampComponent) this.getComponent(TimeStampComponent.COMPONENT_NAME);
	}

	public NameComponent getNameComponent() {
		return (NameComponent) this.getComponent(NameComponent.COMPONENT_NAME);
	}

	public void setPrimaryLookComponent(PrimaryLookComponent component) {
		this.SetComponent(component);
	}

	public void setSanctuaryLookComponent(SanctuaryLookComponent component) {
		this.SetComponent(component);
	}

	public void setTimeStampComponent(TimeStampComponent component) {
		this.SetComponent(component);
	}

	public void setNameComponent(NameComponent component) {
		this.SetComponent(component);
	}

}
