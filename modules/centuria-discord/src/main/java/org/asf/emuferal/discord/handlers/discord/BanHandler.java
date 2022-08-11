package org.asf.centuria.discord.handlers.discord;

import org.asf.centuria.accounts.CenturiaAccount;
import org.asf.centuria.discord.LinkUtils;

import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.guild.BanEvent;
import reactor.core.publisher.Mono;

public class BanHandler {

	/**
	 * Handles bans
	 * 
	 * @param event   Ban event
	 * @param gateway Discord client
	 * @return Result Mono object
	 */
	public static Mono<?> handle(BanEvent event, GatewayDiscordClient gateway) {
		// Find account
		CenturiaAccount acc = LinkUtils.getAccountByDiscordID(event.getUser().getId().asString());

		// Ban it if found
		if (acc != null)
			acc.ban();

		// Return empty mono
		return Mono.empty();
	}

}
