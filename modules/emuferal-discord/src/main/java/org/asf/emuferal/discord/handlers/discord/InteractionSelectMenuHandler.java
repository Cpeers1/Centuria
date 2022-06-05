package org.asf.emuferal.discord.handlers.discord;

import org.asf.emuferal.discord.handlers.discord.interactions.selectmenu.AccountOptionsMenuHandler;
import org.asf.emuferal.discord.handlers.discord.interactions.selectmenu.ServerConfigMenuHandler;
import org.asf.emuferal.discord.handlers.discord.interactions.selectmenu.ServerOptionsMenuHandler;

import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.SelectMenuInteractionEvent;
import reactor.core.publisher.Mono;

public class InteractionSelectMenuHandler {

	/**
	 * Handles select menu interactions
	 * 
	 * @param event   Select menu event
	 * @param gateway Discord client
	 * @return Result Mono object
	 */
	public static Mono<?> handle(SelectMenuInteractionEvent event, GatewayDiscordClient gateway) {
		String id = event.getCustomId();

		if (id.equals("serverconfig")) {
			return ServerConfigMenuHandler.handle(id, event, gateway);
		} else if (id.startsWith("serveroptionselection/")) {
			return ServerOptionsMenuHandler.handle(id, event, gateway);
		} else if (id.equals("accountoption")) {
			return AccountOptionsMenuHandler.handle(id, event, gateway);
		}

		// Default handler
		return Mono.empty();
	}

}
