package org.asf.centuria.discord.handlers.discord.interactions.buttons;

import org.asf.centuria.accounts.AccountManager;
import org.asf.centuria.accounts.CenturiaAccount;

import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.TextInput;
import discord4j.core.spec.InteractionPresentModalSpec;
import reactor.core.publisher.Mono;

public class ReportReplyButtonHandler {

	/**
	 * Handles the feedback reply button event
	 * 
	 * @param event   Button event
	 * @param gateway Discord client
	 * @return Result Mono object
	 */
	public static Mono<?> handle(String id, ButtonInteractionEvent event, GatewayDiscordClient gateway) {
		// Parse request
		String uid = id.split("/")[1];

		// Locate CenturiaAccount
		CenturiaAccount acc = AccountManager.getInstance().getAccount(uid);
		if (acc != null) {
			String mode = id.split("/")[2];

			// Show appeal form
			InteractionPresentModalSpec.Builder modal = InteractionPresentModalSpec.builder();
			modal.title("Send a message to Report " + (mode.equals("tosender") ? "Sender" : "Subject"));
			modal.customId("reportreply/" + acc.getAccountID() + "/" + mode);
			modal.addComponent(ActionRow.of(TextInput.paragraph("reply", "Message to send")));
			return event.presentModal(modal.build());
		}

		// Default response
		return Mono.empty();
	}
}
