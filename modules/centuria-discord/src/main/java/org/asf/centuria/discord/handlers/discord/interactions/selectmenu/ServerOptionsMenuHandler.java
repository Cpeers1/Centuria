package org.asf.centuria.discord.handlers.discord.interactions.selectmenu;

import org.asf.centuria.discord.ServerConfigUtils;

import com.google.gson.JsonObject;

import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.SelectMenuInteractionEvent;
import discord4j.core.spec.InteractionApplicationCommandCallbackSpec;
import reactor.core.publisher.Mono;

public class ServerOptionsMenuHandler {

	/**
	 * Handles the 'server options' select menu event
	 * 
	 * @param event   Select menu event
	 * @param gateway Discord client
	 * @return Result Mono object
	 */
	public static Mono<?> handle(String id, SelectMenuInteractionEvent event, GatewayDiscordClient gateway) {
		// Load option info
		String option = event.getValues().get(0);
		String key = id.split("/")[1];
		String gid = event.getInteraction().getGuild().block().getId().asString();

		// Save to disk
		JsonObject config = ServerConfigUtils.getServerConfig(gid);
		if (config.has(key))
			config.remove(key);
		if (!option.equals("c/reset"))
			config.addProperty(key, option);
		ServerConfigUtils.saveServerConfig(gid, config);

		// Reply success
		InteractionApplicationCommandCallbackSpec.Builder msg = InteractionApplicationCommandCallbackSpec.builder();
		msg.content("Successfully saved the server configuration!");
		return event.reply(msg.ephemeral(true).build());
	}

}
