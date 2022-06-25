package org.asf.emuferal.entities.components.inventory.sanctuaries;

import org.asf.emuferal.entities.inventory.components.Component;
import org.asf.emuferal.entities.inventory.components.InventoryItemComponent;

import com.google.gson.JsonObject;

/**
 * Sanctuary Look Component.
 * Mostly a wrapper for the {@link SanctuaryInfo} object.
 * @author owen9
 *
 */
@Component
public class SanctuaryLookComponent extends InventoryItemComponent {

	private static String ComponentName = "SanctuaryLook";
			
	@Override
	public String getComponentName() {
		return ComponentName;
	}

	@Override
	public JsonObject toJson() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected void getPropertiesFromJson(JsonObject object) {
		// TODO Auto-generated method stub
		
	}

}
