package org.asf.centuria.discord.handlers.discord.interactions.buttons.linking;

import org.asf.centuria.accounts.AccountManager;
import org.asf.centuria.accounts.CenturiaAccount;
import org.asf.centuria.discord.LinkUtils;

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
			// Locate CenturiaAccount
			CenturiaAccount acc = AccountManager.getInstance().getAccount(gid);
			if (acc != null) {
				// Unlink account connection
				if (acc.getPlayerInventory().containsItem("pairedaccount")) {
					// Unlink account
					LinkUtils.unpairAccount(acc, null, false);
					event.getMessage().get().getChannel().block().createMessage("Account has been unlinked.")
							.subscribe();
					return event.getMessage().get().delete();
				}

				// Error
				event.getMessage().get().getChannel().block()
						.createMessage(
								"The account you are attempting to unlink is not connected to a Discord account.")
						.subscribe();
				return event.getMessage().get().delete();
			} else {
				// Reply error
				event.getMessage().get().getChannel().block()
						.createMessage("The account you are attempting unlink does not exist anymore.").subscribe();
				return event.getMessage().get().delete();
			}
		}

		// Default response
		return Mono.empty();
	}
}
