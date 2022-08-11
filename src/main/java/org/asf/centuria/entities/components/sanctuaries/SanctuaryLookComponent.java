package org.asf.centuria.entities.components.sanctuaries;

import org.asf.centuria.entities.components.Component;
import org.asf.centuria.entities.components.InventoryItemComponent;
import org.asf.centuria.entities.sanctuaries.SanctuaryInfo;

import com.google.gson.JsonObject;

/**
 * Sanctuary Look Component. Mostly a wrapper for the {@link SanctuaryInfo}
 * object.
 * 
 * @author Owenvii
 *
 */
@Component
public class SanctuaryLookComponent extends InventoryItemComponent {

	public static final String COMPONENT_NAME = "SanctuaryLook";

	public static final String INFO_PROPERTY_NAME = "info";

	public SanctuaryInfo info;

	@Override
	public String getComponentName() {
		return COMPONENT_NAME;
	}

	@Override
	public JsonObject toJson() {
		JsonObject object = new JsonObject();
		object.add(INFO_PROPERTY_NAME, info.toJson());
		return object;
	}

	@Override
	protected void getPropertiesFromJson(JsonObject object) {
		try {
			this.info = (SanctuaryInfo) new SanctuaryInfo().CreateObjectFromJson(object.get("info").getAsJsonObject());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}
