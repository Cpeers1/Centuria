package org.asf.emuferal.discord.handlers.discord.interactions.buttons.panel;

import org.asf.emuferal.discord.LinkUtils;

import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.TextInput;
import discord4j.core.spec.InteractionApplicationCommandCallbackSpec;
import discord4j.core.spec.InteractionPresentModalSpec;
import reactor.core.publisher.Mono;

public class PairAccountHandler {

	/**
	 * Handles the 'pair account' button event
	 * 
	 * @param event   Button event
	 * @param gateway Discord client
	 * @return Result Mono object
	 */
	public static Mono<?> handle(String id, ButtonInteractionEvent event, GatewayDiscordClient gateway) {
		// Find owner UserID
		String userID = event.getInteraction().getUser().getId().asString();

		// Check link
		if (LinkUtils.isPairedWithEmuFeral(userID)) {
			// Return error
			InteractionApplicationCommandCallbackSpec.Builder msg = InteractionApplicationCommandCallbackSpec.builder();
			msg.content("You can only have one EmuFeral account per Discord account.");
			msg.ephemeral(true);
			return event.reply(msg.build());
		} else {
			// Generate pair code
			String code = LinkUtils.generatePairCode(userID);

			// Build form
			InteractionPresentModalSpec.Builder modal = InteractionPresentModalSpec.builder();
			modal.title("Pair existing EmuFeral account");
			modal.addComponent(ActionRow.of(TextInput.small("codeinput", "Your account pairing code").prefilled(code)));
			modal.customId("dummy");

			// Show form
			return event.presentModal(modal.build());
		}
	}
}
