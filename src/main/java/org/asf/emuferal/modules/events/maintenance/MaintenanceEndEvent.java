package org.asf.emuferal.modules.events.maintenance;

import java.util.Map;

import org.asf.emuferal.modules.eventbus.EventObject;
import org.asf.emuferal.modules.eventbus.EventPath;

/**
 * 
 * Maintenance End Event - called when server maintenance is ended.
 * 
 * @author Sky Swimmer - AerialWorks Software Foundation
 *
 */
@EventPath("maintenance.end")
public class MaintenanceEndEvent extends EventObject {

	@Override
	public String eventPath() {
		return "maintenance.end";
	}

	@Override
	public Map<String, String> eventProperties() {
		return Map.of();
	}
}
