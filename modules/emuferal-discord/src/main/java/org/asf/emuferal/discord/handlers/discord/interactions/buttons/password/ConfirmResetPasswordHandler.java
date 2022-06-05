package org.asf.emuferal.discord.handlers.discord.interactions.buttons.password;

import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.spec.InteractionApplicationCommandCallbackSpec;
import reactor.core.publisher.Mono;

public class ConfirmResetPasswordHandler {

	/**
	 * Confirm reset password button event
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
			msg.content("**Are you sure you wish to reset your password?**\n" + "\n"
					+ "**IMPORTANT:** Resetting the password will release your account's password locks!\n"
					+ "Please make sure to have the game open as when you select 'Reset',\n"
					+ "the account will not have any form of security until you log in again!\n"
					+ "\n"
					+ "After resetting your password, log into your account with a different password,\n"
					+ "the server will update the password for verifying future login attempts.");

			// Add buttons
			msg.addComponent(ActionRow.of(Button.danger("doresetpassword/" + uid + "/" + action, "Reset password"),
					Button.primary("dismissDelete", "Cancel")));

			// Reply
			return event.reply(msg.build());
		}

		// Default response
		return Mono.empty();
	}
}
