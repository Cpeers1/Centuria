package org.asf.centuria.discord.handlers.discord.interactions.buttons.password;

import org.asf.centuria.discord.TimedActions;

import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.object.entity.Message;
import discord4j.discordjson.json.MessageReferenceData;
import reactor.core.publisher.Mono;

public class ResetPasswordHandler {

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
			// Disable the message
			if (!event.getMessage().get().getData().messageReference().isAbsent()) {
				try {
					MessageReferenceData ref = event.getMessage().get().getData().messageReference().get();
					Message oMsg = gateway.getMessageById(Snowflake.of(ref.channelId().get().asString()),
							Snowflake.of(ref.messageId().get())).block();
					oMsg.delete().subscribe();
				} catch (Exception e) {
				}
			}
			event.getMessage().get().edit().withComponents().subscribe();

			// Run the action (or attempt to)
			if (TimedActions.runAction(action)) {
				// Send success
				return event.reply("Account password has been reset, please log into your account to update it.");
			} else {
				// Send failure
				return event.reply("Password reset request has expired.");
			}
		}

		// Default response
		return Mono.empty();
	}
}
