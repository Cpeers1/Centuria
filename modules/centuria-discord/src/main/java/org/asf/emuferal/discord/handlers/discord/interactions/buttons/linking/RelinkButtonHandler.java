package org.asf.centuria.discord.handlers.discord.interactions.buttons.linking;

import org.asf.centuria.accounts.AccountManager;
import org.asf.centuria.accounts.CenturiaAccount;
import org.asf.centuria.discord.LinkUtils;

import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import reactor.core.publisher.Mono;

public class RelinkButtonHandler {

	/**
	 * Handles the re-link button event
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
		// unlinking of their account.
		String str = event.getInteraction().getUser().getId().asString();
		if (uid.equals(str)) {
			// Locate CenturiaAccount
			CenturiaAccount acc = AccountManager.getInstance().getAccount(gid);
			if (acc != null) {
				// Unlink existing account connection
				if (acc.getPlayerInventory().containsItem("pairedaccount")) {
					// Check if currently linked to this account
					String userID = acc.getPlayerInventory().getItem("pairedaccount").getAsJsonObject().get("userId")
							.getAsString();
					if (userID.equals(uid)) {
						// Return error
						event.getMessage().get().getChannel().block()
								.createMessage("The account has already been re-linked with your current account.")
								.subscribe();
						return event.getMessage().get().delete();
					}

					// Unlink account
					LinkUtils.unpairAccount(acc, null, false);
				}

				// Re-link the account
				LinkUtils.pairAccount(acc, uid, null, false, false);

				event.getMessage().get().getChannel().block()
						.createMessage("Your account has now been re-linked with your current discord account.")
						.subscribe();
				return event.getMessage().get().delete();
			} else {
				// Reply error
				event.getMessage().get().getChannel().block()
						.createMessage("The account you are attempting to re-connect does not exist anymore.")
						.subscribe();
				return event.getMessage().get().delete();
			}
		}

		// Default response
		return Mono.empty();
	}
}
