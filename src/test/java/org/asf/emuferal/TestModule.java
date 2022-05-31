package org.asf.emuferal;

import org.asf.emuferal.modules.IEmuFeralModule;
import org.asf.emuferal.modules.eventbus.EventListener;
import org.asf.emuferal.modules.events.servers.APIServerStartupEvent;

public class TestModule implements IEmuFeralModule {

	@Override
	public String id() {
		return "test";
	}

	@Override
	public String version() {
		return "1.0.0.A1";
	}

	@Override
	public void init() {
	}

	@EventListener
	public void startServer(APIServerStartupEvent event) {
		event = event;
	}

}
