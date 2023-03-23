package org.asf.centuria;

import org.asf.centuria.accounts.SaveMode;
import org.asf.centuria.modules.ICenturiaModule;
import org.asf.centuria.modules.eventbus.EventListener;
import org.asf.centuria.modules.events.accounts.AccountPreloginEvent;
import org.asf.centuria.modules.events.chatcommands.ChatCommandEvent;
import org.asf.centuria.modules.events.chatcommands.ModuleCommandSyntaxListEvent;
import org.asf.centuria.modules.events.interactions.InteractionSuccessEvent;
import org.asf.centuria.modules.events.updates.ServerUpdateEvent;

public class TestModule implements ICenturiaModule {

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
	}

	@EventListener
	public void interactionSuccess(InteractionSuccessEvent event) {
	}

	@EventListener
	public void prelogin(AccountPreloginEvent event) {
	}

	@EventListener
	public void registerCommands(ModuleCommandSyntaxListEvent event) {
		event.addCommandSyntaxMessage("test");
		event.addCommandSyntaxMessage("migrate");
	}

	@EventListener
	public void runCommand(ChatCommandEvent event) {
		if (event.getCommandID().equals("test")) {
			event.respond("Test 123");
			Centuria.systemMessage(event.getAccount().getOnlinePlayerInstance(), "test", true);
		} else if (event.getCommandID().equals("migrate")) {
			if (event.getAccount().getSaveMode() == SaveMode.MANAGED) {
				event.respond("Already using managed data");
				return;
			}

			event.getAccount().getOnlinePlayerInstance().client
					.sendPacket("%xt%mod:ft%-1%disconnect%Disconnected%Account data migration in progress%Log out%");
			event.getAccount().migrateSaveDataToManagedMode();
		}
	}

}
