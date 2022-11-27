package org.asf.centuria.discord.handlers.discord.interactions.buttons;

import org.asf.centuria.accounts.AccountManager;
import org.asf.centuria.accounts.CenturiaAccount;
import org.asf.centuria.discord.TimedActions;

import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.TextInput;
import discord4j.core.spec.InteractionPresentModalSpec;
import reactor.core.publisher.Mono;

public class FeedbackReplyButtonHandler {

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

		// Check if the ID matches a action in memory
		if (TimedActions.runAction(uid)) {
			return event.getMessage().get().delete();
		}

		// Locate CenturiaAccount
		CenturiaAccount acc = AccountManager.getInstance().getAccount(uid);
		if (acc != null) {
			// Show appeal form
			InteractionPresentModalSpec.Builder modal = InteractionPresentModalSpec.builder();
			modal.title("Reply to Feedback Report");
			modal.customId("feedbackreply/" + acc.getAccountID());
			modal.addComponent(ActionRow.of(TextInput.paragraph("reply", "Feedback reply message")));
			return event.presentModal(modal.build());
		}

		// Default response
		return Mono.empty();
	}
}
