package org.asf.emuferal.modules.events.updates;

import java.util.Map;

import org.asf.emuferal.modules.eventbus.EventObject;
import org.asf.emuferal.modules.eventbus.EventPath;

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

	@Override
	public Map<String, String> eventProperties() {
		return Map.of();
	}
}
