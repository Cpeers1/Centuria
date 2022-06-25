package org.asf.emuferal.entities.inventory.uservars;

import java.lang.reflect.InvocationTargetException;

import org.asf.emuferal.entities.inventory.InventoryItem;
import org.asf.emuferal.entities.inventory.components.InventoryItemComponent;
import org.asf.emuferal.entities.inventory.components.uservars.UserVarBitComponent;
import org.asf.emuferal.entities.inventory.components.uservars.UserVarBitOnOnlyComponent;
import org.asf.emuferal.entities.inventory.components.uservars.UserVarComponent;
import org.asf.emuferal.entities.inventory.components.uservars.UserVarCounterComponent;
import org.asf.emuferal.entities.inventory.components.uservars.UserVarCustomComponent;
import org.asf.emuferal.entities.inventory.components.uservars.UserVarHighestComponent;
import org.asf.emuferal.entities.inventory.components.uservars.UserVarLowestComponent;
import org.asf.emuferal.enums.inventory.uservars.UserVarType;

import com.google.gson.JsonObject;

/**
 * Friendly interface for making a userVarItem.
 * 
 * @author owen9
 *
 */
public final class UserVarItem extends InventoryItem {

	private UserVarType type;

	public UserVarItem(int defId, String uuid, int invType) {
		super(defId, uuid, invType);
	}

	public UserVarItem(UserVarType type) {
		super(0, "", 0);
		this.type = type;
	}

	public JsonObject toJsonObject() {
		return super.toJsonObject();
	}
	
	public UserVarComponent getUserVarComponent()
	{
		return (UserVarComponent)this.GetComponent(type.componentName);
	}
	
	public void setUserVarComponent(UserVarComponent component)
	{
		this.AddComponent(component);
	}

	public void fromJsonObject(JsonObject object) throws InstantiationException, IllegalAccessException,
			IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {

		// Wait! first get the userVarComponent...

		var item = object.get(componentsPropertyName).getAsJsonObject().get(type.componentName).getAsJsonObject();

		switch(type)
		{
			case Any:
				this.AddComponent(InventoryItemComponent.fromJson(UserVarCustomComponent.class, item));
				break;
			case Bit:
				this.AddComponent(InventoryItemComponent.fromJson(UserVarBitComponent.class, item));
				break;
			case BitOnOnly:
				this.AddComponent(InventoryItemComponent.fromJson(UserVarBitOnOnlyComponent.class, item));
				break;
			case Counter:
				this.AddComponent(InventoryItemComponent.fromJson(UserVarCounterComponent.class, item));
				break;
			case Highest:
				this.AddComponent(InventoryItemComponent.fromJson(UserVarHighestComponent.class, item));
				break;
			case Lowest:
				this.AddComponent(InventoryItemComponent.fromJson(UserVarLowestComponent.class, item));
				break;
		}

		super.fromJsonObject(object);
	}

}
