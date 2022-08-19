package org.asf.centuria.entities.inventoryitems.uservars;

import java.lang.reflect.InvocationTargetException;

import org.asf.centuria.entities.components.InventoryItemComponent;
import org.asf.centuria.entities.components.uservars.UserVarBitComponent;
import org.asf.centuria.entities.components.uservars.UserVarBitOnOnlyComponent;
import org.asf.centuria.entities.components.uservars.UserVarComponent;
import org.asf.centuria.entities.components.uservars.UserVarCounterComponent;
import org.asf.centuria.entities.components.uservars.UserVarCustomComponent;
import org.asf.centuria.entities.components.uservars.UserVarHighestComponent;
import org.asf.centuria.entities.components.uservars.UserVarLowestComponent;
import org.asf.centuria.entities.inventoryitems.InventoryItem;
import org.asf.centuria.entities.inventoryitems.Item;
import org.asf.centuria.enums.inventory.InventoryType;
import org.asf.centuria.enums.uservars.UserVarType;

import com.google.gson.JsonObject;

/**
 * Friendly interface for making a userVarItem.
 * 
 * @author Owenvii
 *
 */
@Item
public final class UserVarItem extends InventoryItem {

	public final static InventoryType INV_TYPE = InventoryType.UserVar;

	private UserVarType type;
		
	public UserVarItem()
	{
		super(0, "", INV_TYPE);
	}

	
	public UserVarItem(int defId, String uuid, InventoryType invType) {
		super(defId, uuid, invType);
	}

	public UserVarItem(UserVarType type) {
		super(0, "", INV_TYPE);
		this.type = type;
	}

	public UserVarItem(int defId, String uuid, UserVarType type) {
		super(defId, uuid, INV_TYPE);
		this.type = type;
	}
	
	@Override
	public InventoryType getInventoryType() {
		return INV_TYPE;
	}

	public JsonObject toJsonObject() {
		return super.toJsonObject();
	}

	public UserVarComponent getUserVarComponent() {
		return (UserVarComponent) this.getComponent(type.componentName);
	}

	public void setUserVarComponent(UserVarComponent component) {
		this.addComponent(component);
	}

	/**
	 * Populates this item with properties from a json object of the same type.
	 */
	public void fromJsonObject(JsonObject object) throws InstantiationException, IllegalAccessException,
			IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {

		// Wait! first get the userVarComponent...

		var item = object.get(COMPONENTS_PROPERTY_NAME).getAsJsonObject().get(type.componentName).getAsJsonObject();

		switch (type) {
		case Any:
			this.addComponent(InventoryItemComponent.fromJson(UserVarCustomComponent.class, item));
			break;
		case Bit:
			this.addComponent(InventoryItemComponent.fromJson(UserVarBitComponent.class, item));
			break;
		case BitOnOnly:
			this.addComponent(InventoryItemComponent.fromJson(UserVarBitOnOnlyComponent.class, item));
			break;
		case Counter:
			this.addComponent(InventoryItemComponent.fromJson(UserVarCounterComponent.class, item));
			break;
		case Highest:
			this.addComponent(InventoryItemComponent.fromJson(UserVarHighestComponent.class, item));
			break;
		case Lowest:
			this.addComponent(InventoryItemComponent.fromJson(UserVarLowestComponent.class, item));
			break;
		}

		super.fromJsonObject(object);
	}




}
