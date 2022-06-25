package org.asf.emuferal.entities.inventory.uservars;

import java.lang.reflect.InvocationTargetException;

import org.asf.emuferal.entities.components.InventoryItemComponent;
import org.asf.emuferal.entities.components.uservars.UserVarBitComponent;
import org.asf.emuferal.entities.components.uservars.UserVarBitOnOnlyComponent;
import org.asf.emuferal.entities.components.uservars.UserVarComponent;
import org.asf.emuferal.entities.components.uservars.UserVarCounterComponent;
import org.asf.emuferal.entities.components.uservars.UserVarCustomComponent;
import org.asf.emuferal.entities.components.uservars.UserVarHighestComponent;
import org.asf.emuferal.entities.components.uservars.UserVarLowestComponent;
import org.asf.emuferal.entities.inventory.InventoryItem;
import org.asf.emuferal.enums.uservars.UserVarType;

import com.google.gson.JsonObject;

/**
 * Friendly interface for making a userVarItem.
 * 
 * @author Owenvii
 *
 */
public final class UserVarItem extends InventoryItem {

	public final static int InvType = 303;
	
	private UserVarType type;

	public UserVarItem(int defId, String uuid, int invType) {
		super(defId, uuid, invType);
	}

	public UserVarItem(UserVarType type) {
		super(0, "", InvType);
		this.type = type;
	}
	
	public UserVarItem(int defId, String uuid, UserVarType type)
	{
		super(defId, uuid, InvType);
		this.type = type;
	}

	public JsonObject toJsonObject() {
		return super.toJsonObject();
	}
	
	public UserVarComponent getUserVarComponent()
	{
		return (UserVarComponent)this.getComponent(type.componentName);
	}
	
	public void setUserVarComponent(UserVarComponent component)
	{
		this.addComponent(component);
	}

	/**
	 * Populates this item with properties from a json object of the same type.
	 */
	public void fromJsonObject(JsonObject object) throws InstantiationException, IllegalAccessException,
			IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {

		// Wait! first get the userVarComponent...

		var item = object.get(componentsPropertyName).getAsJsonObject().get(type.componentName).getAsJsonObject();

		switch(type)
		{
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
