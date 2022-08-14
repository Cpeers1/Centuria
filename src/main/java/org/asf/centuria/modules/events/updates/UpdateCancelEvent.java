package org.asf.centuria.modules.events.updates;

import org.asf.centuria.modules.eventbus.EventObject;
import org.asf.centuria.modules.eventbus.EventPath;

/**
 * 
 * Update Cancel Event - called when a (automated) update is cancelled by a
 * admin
 * 
 * @author Sky Swimmer - AerialWorks Software Foundation
 *
 */
@EventPath("update.cancel")
public class UpdateCancelEvent extends EventObject {

	@Override
	public String eventPath() {
		return "update.cancel";
	}

}
