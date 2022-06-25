package org.asf.emuferal.entities.components.inventory.sanctuaries;

import org.asf.emuferal.entities.inventory.components.Component;
import org.asf.emuferal.entities.inventory.components.InventoryItemComponent;

import com.google.gson.JsonObject;

/**
 * Primary look component.
 * Seems to mostly be a dud, but needs to be present on sanctuary invs.
 * @author owen9
 *
 */
@Component
public class PrimaryLookComponent extends InventoryItemComponent {

	public static String ComponentName = "PrimaryLook";
	
	@Override
	public String getComponentName() {
		return ComponentName;
	}

	@Override
	public JsonObject toJson() {
		return new JsonObject();
	}

	@Override
	protected void getPropertiesFromJson(JsonObject object) {
		//Absolutely nothing to do here...
	}

}
