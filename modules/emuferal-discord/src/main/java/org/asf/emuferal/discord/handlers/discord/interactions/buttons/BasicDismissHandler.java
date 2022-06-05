package org.asf.emuferal.discord.handlers.discord.interactions.buttons;

import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import reactor.core.publisher.Mono;

public class BasicDismissHandler {

	/**
	 * Handles the dismiss button event
	 * 
	 * @param event   Button event
	 * @param gateway Discord client
	 * @return Result Mono object
	 */
	public static Mono<?> handle(String id, ButtonInteractionEvent event, GatewayDiscordClient gateway) {
		// Handle button
		event.deferEdit().subscribe();

		// Delete buttons from message
		event.getMessage().get().edit().withComponents().subscribe();

		// Default response
		return Mono.empty();
	}
}
