package org.asf.emuferal.discord.handlers.discord.interactions.forms;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;

import org.asf.emuferal.accounts.EmuFeralAccount;
import org.asf.emuferal.discord.DiscordBotModule;
import org.asf.emuferal.discord.LinkUtils;
import org.asf.emuferal.discord.ServerConfigUtils;

import com.google.gson.JsonObject;

import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ModalSubmitInteractionEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.spec.MessageCreateSpec;
import reactor.core.publisher.Mono;

public class AppealHandler {

	/**
	 * Handles the appeal form submission event
	 * 
	 * @param event   Modal submission event
	 * @param gateway Discord client
	 * @return Result Mono object
	 */
	public static Mono<?> handle(String id, ModalSubmitInteractionEvent event, GatewayDiscordClient gateway) {
		// Load fields
		String shortWhy = event.getInteraction().getData().data().get().components().get().get(0).components().get()
				.get(0).value().get();
		String pardonReason = event.getInteraction().getData().data().get().components().get().get(1).components().get()
				.get(0).value().get();

		// Find owner UserID
		String userID = event.getInteraction().getUser().getId().asString();

		// Find account
		EmuFeralAccount account = LinkUtils.getAccountByDiscordID(userID);
		if (account == null)
			return Mono.empty();

		// Build report
		String report = "Appeal form:\n";
		report += "--------------------------------------------------------------------------------------------------\n";
		report += "\n";
		report += "What was the reason for your ban?\n";
		report += "--------------------------------------------------------------------------------------------------\n";
		report += shortWhy + "\n";
		report += "--------------------------------------------------------------------------------------------------\n";
		report += "\n";
		report += "Why do you believe you should be pardoned?\n";
		report += "--------------------------------------------------------------------------------------------------\n";
		report += pardonReason + "\n";
		report += "--------------------------------------------------------------------------------------------------\n";

		// Send to all guild log channels
		for (Guild g : DiscordBotModule.getClient().getGuilds().toIterable()) {
			String guildID = g.getId().asString();
			JsonObject config = ServerConfigUtils.getServerConfig(guildID);
			if (config.has("moderationLogChannel")) {
				// Find channel
				String ch = config.get("moderationLogChannel").getAsString();

				// Build message content
				String srvMessage = "**Ban Appeal Received**\n";
				srvMessage += "\n";
				srvMessage += "Appeal issuer: `" + account.getDisplayName() + "` (<@!" + userID + ">)\n";
				srvMessage += "Appeal issued at: <t:" + (System.currentTimeMillis() / 1000) + ">\n";
				if (config.has("moderatorRole")) {
					// Add ping
					srvMessage += "\n\n<@&" + config.get("moderatorRole").getAsString() + ">";
				}

				// Build message object
				MessageCreateSpec.Builder msg = MessageCreateSpec.builder();
				msg.content(srvMessage);

				// Add report
				try {
					msg.addFile("appeal.txt", new ByteArrayInputStream(report.getBytes("UTF-8")));
				} catch (UnsupportedEncodingException e1) {
				}

				// Attempt to send message
				try {
					g.getChannelById(Snowflake.of(ch)).block().getRestChannel().createMessage(msg.build().asRequest())
							.block();
				} catch (Exception e) {
				}
			}
		}

		// Reset and acknowledge
		event.getMessage().get().edit().withComponents().subscribe();
		return event.deferEdit();
	}
}
