package org.asf.centuria.discord.handlers.discord.interactions.buttons.registration;

import org.asf.centuria.discord.LinkUtils;
import org.asf.centuria.discord.TimedActions;
import org.asf.centuria.discord.handlers.registration.DiscordRegistrationHelper;

import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import reactor.core.publisher.Mono;

public class CreateAccountHandler {

	/**
	 * Registration button event
	 * 
	 * @param event   Button event
	 * @param gateway Discord client
	 * @return Result Mono object
	 */
	public static Mono<?> handle(String id, ButtonInteractionEvent event, GatewayDiscordClient gateway) {
		// Parse request
		String uid = id.split("/")[1];
		String action = id.split("/")[2];
		boolean isAPI = false;
		if (id.split("/").length == 4)
			isAPI = id.split("/")[3].equals("api");

		// Verify interaction owner
		String str = event.getInteraction().getUser().getId().asString();
		if (uid.equals(str)) {
			// Disable the buttons
			event.getMessage().get().edit().withComponents().subscribe();

			// Run the action (or attempt to)
			if (TimedActions.runAction(action)) {
				if (!isAPI) {
					// Check linked account
					if (!LinkUtils.isPairedWithCenturia(uid)) {
						// Send failure
						return event.reply(
								"Registration request has been invalidated as someone else registered an account with your login name.");
					}

					// Send success
					return event.reply("Registration completed, please log into your account to update its password.");
				} else {
					return event.reply("**Registration confirmed**\nVerification code: `"
							+ DiscordRegistrationHelper.getCode("apiregistration-" + uid)
							+ "`\nThis code is valid for 15 minutes.");
				}
			} else {
				// Send failure
				return event.reply("Registration request has expired.");
			}
		}

		// Default response
		return Mono.empty();
	}
}
