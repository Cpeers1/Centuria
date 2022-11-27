package org.asf.centuria.discord.handlers.discord.interactions.buttons;

import org.asf.centuria.accounts.AccountManager;
import org.asf.centuria.accounts.CenturiaAccount;

import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.TextInput;
import discord4j.core.spec.InteractionPresentModalSpec;
import reactor.core.publisher.Mono;

public class AppealButtonHandler {

	/**
	 * Handles the appeal button event
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
				// Show appeal form
				InteractionPresentModalSpec.Builder modal = InteractionPresentModalSpec.builder();
				modal.title("Centuria Online Ban Appeal");
				modal.customId("appealform");
				modal.addComponent(ActionRow.of(TextInput.small("short_why", "What was the reason for your ban?")));
				modal.addComponent(
						ActionRow.of(TextInput.paragraph("appeal", "Why do you believe you should be pardoned?")));
				return event.presentModal(modal.build());
			} else {
				// Reply error
				event.getMessage().get().edit().withComponents().subscribe();
				return event.reply("The account you are attempting appeal for does not exist anymore.");
			}
		}

		// Default response
		return Mono.empty();
	}
}
