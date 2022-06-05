package org.asf.emuferal.discord.handlers.discord.interactions.buttons.accountoptions;

import org.asf.emuferal.accounts.AccountManager;
import org.asf.emuferal.accounts.EmuFeralAccount;

import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.spec.InteractionApplicationCommandCallbackSpec;
import reactor.core.publisher.Mono;

public class ConfirmDisable2fa {

	/**
	 * Confirm 'disable 2-factor authentication' handler
	 * 
	 * @param event   Button event
	 * @param gateway Discord client
	 * @return Result Mono object
	 */
	public static Mono<?> handle(String id, ButtonInteractionEvent event, GatewayDiscordClient gateway) {
		// Parse request
		String uid = id.split("/")[1];
		String gid = id.split("/")[2];

		// Verify interaction owner
		String str = event.getInteraction().getUser().getId().asString();
		if (uid.equals(str)) {
			// Locate EmuFeralAccount
			EmuFeralAccount acc = AccountManager.getInstance().getAccount(gid);
			if (acc != null) {

				// Build response (the 'Are you sure' prompt)
				InteractionApplicationCommandCallbackSpec.Builder msg = InteractionApplicationCommandCallbackSpec
						.builder();
				msg.content("Disable 2-factor authentication?");

				// Add buttons
				msg.addComponent(ActionRow.of(Button.danger("dodisable2fa/" + uid + "/" + gid, "Confirm"),
						Button.primary("dismissDelete", "Cancel")));

				// Reply
				return event.reply(msg.build());
			} else {
				// Reply error
				return event.reply("The account you are attempting to configure does not exist anymore.");
			}
		}

		// Default response
		return Mono.empty();
	}
}
