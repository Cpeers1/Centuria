package org.asf.centuria.discord.handlers.discord;

import org.asf.centuria.discord.handlers.discord.interactions.forms.AppealHandler;
import org.asf.centuria.discord.handlers.discord.interactions.forms.RegistrationHandler;
import org.asf.centuria.discord.handlers.discord.interactions.forms.UpdateDisplayNameHandler;

import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ModalSubmitInteractionEvent;
import reactor.core.publisher.Mono;

public class InteractionModalHandler {

	/**
	 * Handles modal interaction
	 * 
	 * @param event   Modal submission event
	 * @param gateway Discord client
	 * @return Result Mono object
	 */
	public static Mono<?> handle(ModalSubmitInteractionEvent event, GatewayDiscordClient gateway) {
		String id = event.getCustomId();

		if (id.equals("dummy")) {
			return event.deferEdit();
		} else if (id.equals("updatedisplayname")) {
			return UpdateDisplayNameHandler.handle(id, event, gateway);
		} else if (id.equals("accountregistration")) {
			return RegistrationHandler.handle(id, event, gateway);
		} else if (id.equals("appealform")) {
			return AppealHandler.handle(id, event, gateway);
		}

		// Default handler
		return Mono.empty();
	}

}
