package org.asf.centuria.discord.handlers.discord.interactions.buttons.panel;

import org.asf.centuria.discord.DiscordBotModule;
import org.asf.centuria.discord.LinkUtils;

import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.TextInput;
import discord4j.core.spec.InteractionApplicationCommandCallbackSpec;
import discord4j.core.spec.InteractionPresentModalSpec;
import reactor.core.publisher.Mono;

public class RegisterAccountHandler {

	/**
	 * Handles the 'register account' button event
	 * 
	 * @param event   Button event
	 * @param gateway Discord client
	 * @return Result Mono object
	 */
	public static Mono<?> handle(String id, ButtonInteractionEvent event, GatewayDiscordClient gateway) {
		// Find owner UserID
		String userID = event.getInteraction().getUser().getId().asString();

		// Check link
		if (LinkUtils.isPairedWithCenturia(userID)) {
			// Return error
			InteractionApplicationCommandCallbackSpec.Builder msg = InteractionApplicationCommandCallbackSpec.builder();
			msg.content("You can only have one Centuria account per Discord account.");
			msg.ephemeral(true);
			return event.reply(msg.build());
		} else {
			// Build form
			InteractionPresentModalSpec.Builder modal = InteractionPresentModalSpec.builder();
			modal.title("Register for " + DiscordBotModule.getServerName());
			modal.addComponent(ActionRow.of(TextInput.small("accountname", "Account login name", 3, 320).required()));
			modal.addComponent(ActionRow.of(TextInput.small("displayname", "Ingame display name", 2, 16).required()));
			modal.addComponent(
					ActionRow.of(TextInput.small("enable2fa", "Enable 2-factor authentication? (yes/no)", 2, 3)
							.required(false).prefilled("No")));
			modal.customId("accountregistration");

			// Show form
			return event.presentModal(modal.build());
		}
	}
}
