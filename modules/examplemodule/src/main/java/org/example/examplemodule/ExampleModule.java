package org.example.examplemodule;

import org.asf.centuria.modules.ICenturiaModule;
import org.asf.centuria.modules.eventbus.EventListener;
import org.asf.centuria.modules.events.servers.GameServerStartupEvent;

public class ExampleModule implements ICenturiaModule {

	@Override
	public String id() {
		return "example";
	}

	@Override
	public String version() {
		return "1.0.0.A1";
	}

	@Override
	public void init() {
		// Main init method
	}
	
	@EventListener
	public void handleGameServerStart(GameServerStartupEvent event) {
		// Example event handler, called on gameserver startup
	}

}
