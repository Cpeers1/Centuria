package org.asf.centuria.modules.events.maintenance;

import org.asf.centuria.modules.eventbus.EventObject;
import org.asf.centuria.modules.eventbus.EventPath;

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

}
