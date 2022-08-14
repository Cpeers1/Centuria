package org.asf.centuria.discord.handlers.discord.interactions.buttons.auth2fa;

import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.spec.InteractionApplicationCommandCallbackSpec;
import reactor.core.publisher.Mono;

public class ConfirmWhitelistHandler2fa {

	/**
	 * Confirm login button event (2fa)
	 * 
	 * @param event   Button event
	 * @param gateway Discord client
	 * @return Result Mono object
	 */
	public static Mono<?> handle(String id, ButtonInteractionEvent event, GatewayDiscordClient gateway) {
		// Parse request
		String uid = id.split("/")[1];
		String action = id.split("/")[2];

		// Verify interaction owner
		String str = event.getInteraction().getUser().getId().asString();
		if (uid.equals(str)) {

			// Build response (the 'Are you sure' prompt)
			InteractionApplicationCommandCallbackSpec.Builder msg = InteractionApplicationCommandCallbackSpec.builder();
			msg.content("Confirm this login attempt?\n"
					+ "\nYou have selected the `Always allow from this IP` option, this will mean that any further login attempts from this location will be allowed without verifying, are you sure you wish to continue?");

			// Add buttons
			msg.addComponent(ActionRow.of(Button.success("dowhitelistip2fa/" + uid + "/" + action, "Proceed"),
					Button.primary("dismissDelete", "Cancel")));

			// Reply
			return event.reply(msg.build());
		}

		// Default response
		return Mono.empty();
	}
}
