package org.asf.emuferal.entities.components.sanctuaries;

import org.asf.emuferal.entities.components.Component;
import org.asf.emuferal.entities.components.InventoryItemComponent;
import org.asf.emuferal.entities.sanctuaries.SanctuaryInfo;

import com.google.gson.JsonObject;

/**
 * Sanctuary Look Component. Mostly a wrapper for the {@link SanctuaryInfo}
 * object.
 * 
 * @author owen9
 *
 */
@Component
public class SanctuaryLookComponent extends InventoryItemComponent {

	public static final String COMPONENT_NAME = "SanctuaryLook";

	@Override
	public String getComponentName() {
		return COMPONENT_NAME;
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
