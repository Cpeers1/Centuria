package org.asf.emuferal.discord.handlers.discord.interactions.buttons.linking;

import org.asf.emuferal.accounts.AccountManager;
import org.asf.emuferal.accounts.EmuFeralAccount;
import org.asf.emuferal.discord.LinkUtils;

import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import reactor.core.publisher.Mono;

public class UnlinkButtonHandler {

	/**
	 * Handles the unlink button event
	 * 
	 * @param event   Button event
	 * @param gateway Discord client
	 * @return Result Mono object
	 */
	public static Mono<?> handle(String id, ButtonInteractionEvent event, GatewayDiscordClient gateway) {
		// Parse request
		String uid = id.split("/")[1];
		String gid = id.split("/")[2];

		// Verify that the one making the interaction is the one who was DM'd about the
		// linking of their account.
		String str = event.getInteraction().getUser().getId().asString();
		if (uid.equals(str)) {
			// Locate EmuFeralAccount
			EmuFeralAccount acc = AccountManager.getInstance().getAccount(gid);
			if (acc != null) {
				// Unlink account connection
				if (acc.getPlayerInventory().containsItem("pairedaccount")) {
					// Unlink account
					LinkUtils.unpairAccount(acc, null, false);
					return event.reply("Account has been unlinked.");
				}

				// Error
				return event.reply("The account you are attempting to unlink is not connected to a Discord account.");
			} else {
				// Reply error
				event.getMessage().get().edit().withComponents().subscribe();
				return event.reply("The account you are attempting unlink does not exist anymore.");
			}
		}

		// Default response
		return Mono.empty();
	}
}
