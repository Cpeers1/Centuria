package org.asf.emuferal;

import org.asf.emuferal.modules.IEmuFeralModule;
import org.asf.emuferal.modules.eventbus.EventListener;
import org.asf.emuferal.modules.events.chatcommands.ChatCommandEvent;
import org.asf.emuferal.modules.events.chatcommands.ModuleCommandSyntaxListEvent;
import org.asf.emuferal.modules.events.servers.APIServerStartupEvent;
import org.asf.emuferal.modules.events.updates.ServerUpdateEvent;

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
	public void update(ServerUpdateEvent event) {
		event = event;
	}

	@EventListener
	public void registerCommands(ModuleCommandSyntaxListEvent event) {
		event.addCommandSyntaxMessage("test");
	}

	@EventListener
	public void runCommand(ChatCommandEvent event) {
		if (event.getCommandID().equals("test"))
			event.respond("Test 123");
	}

}
