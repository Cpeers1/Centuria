package org.asf.emuferal.modules.events.maintenance;

import java.util.Map;

import org.asf.emuferal.modules.eventbus.EventObject;
import org.asf.emuferal.modules.eventbus.EventPath;

/**
 * 
 * Maintenance Start Event - called when server maintenance is started.
 * 
 * @author Sky Swimmer - AerialWorks Software Foundation
 *
 */
@EventPath("maintenance.start")
public class MaintenanceStartEvent extends EventObject {

	@Override
	public String eventPath() {
		return "maintenance.start";
	}

	@Override
	public Map<String, String> eventProperties() {
		return Map.of();
	}

}
