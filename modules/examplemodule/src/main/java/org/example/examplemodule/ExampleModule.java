package org.example.examplemodule;

import org.asf.emuferal.modules.IEmuFeralModule;
import org.asf.emuferal.modules.eventbus.EventListener;
import org.asf.emuferal.modules.events.servers.GameServerStartupEvent;

public class ExampleModule implements IEmuFeralModule {

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
