package org.asf.centuria.discord.handlers.discord.interactions.buttons.password;

import org.asf.centuria.accounts.AccountManager;
import org.asf.centuria.accounts.CenturiaAccount;

import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.spec.InteractionApplicationCommandCallbackSpec;
import reactor.core.publisher.Mono;

public class ConfirmBlockIpHandler {

	/**
	 * Confirm block IP button event
	 * 
	 * @param event   Button event
	 * @param gateway Discord client
	 * @return Result Mono object
	 */
	public static Mono<?> handle(String id, ButtonInteractionEvent event, GatewayDiscordClient gateway) {
		// Parse request
		String uid = id.split("/")[1];
		String gid = id.split("/")[2];
		String ip = id.split("/")[3];

		// Verify interaction owner
		String str = event.getInteraction().getUser().getId().asString();
		if (uid.equals(str)) {
			// Locate CenturiaAccount
			CenturiaAccount acc = AccountManager.getInstance().getAccount(gid);
			if (acc != null) {
				// Build response (the 'Are you sure' prompt)
				InteractionApplicationCommandCallbackSpec.Builder msg = InteractionApplicationCommandCallbackSpec
						.builder();
				msg.content("Are you sure you wish to block this IP address?");

				// Add buttons
				msg.addComponent(ActionRow.of(Button.danger("doblockip/" + uid + "/" + gid + "/" + ip, "Confirm"),
						Button.primary("dismissDelete", "Cancel")));

				// Reply
				return event.reply(msg.build());
			} else {
				// Reply error
				event.getMessage().get().edit().withComponents().subscribe();
				return event.reply("The account you are attempting to block an IP for does not exist anymore.");
			}
		}

		// Default response
		return Mono.empty();
	}
}
