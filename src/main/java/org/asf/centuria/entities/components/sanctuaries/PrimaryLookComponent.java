package org.asf.centuria.entities.components.sanctuaries;

import org.asf.centuria.entities.components.Component;
import org.asf.centuria.entities.components.InventoryItemComponent;

import com.google.gson.JsonObject;

/**
 * Primary look component. Seems to mostly be a dud, but needs to be present on
 * sanctuary invs.
 * 
 * @author Owenvii
 *
 */
@Component
public class PrimaryLookComponent extends InventoryItemComponent {

	public static final String COMPONENT_NAME = "PrimaryLook";

	@Override
	public String getComponentName() {
		return COMPONENT_NAME;
	}

	@Override
	public JsonObject toJson() {
		return new JsonObject();
	}

	@Override
	protected void getPropertiesFromJson(JsonObject object) {
		// Absolutely nothing to do here...
	}

}
