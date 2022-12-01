package org.asf.centuria.discord.handlers.discord.interactions.forms;

import java.time.Duration;

import org.asf.centuria.Centuria;
import org.asf.centuria.accounts.AccountManager;
import org.asf.centuria.accounts.CenturiaAccount;
import org.asf.centuria.discord.DiscordBotModule;
import org.asf.centuria.discord.LinkUtils;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ModalSubmitInteractionEvent;
import discord4j.core.object.entity.User;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.rest.util.Color;
import reactor.core.publisher.Mono;

public class ReportReplyHandler {

	/**
	 * Handles the user report reply form submission event
	 * 
	 * @param event   Modal submission event
	 * @param gateway Discord client
	 * @return Result Mono object
	 */
	public static Mono<?> handle(String id, ModalSubmitInteractionEvent event, GatewayDiscordClient gateway) {
		// Load fields
		String reply = event.getInteraction().getData().data().get().components().get().get(0).components().get().get(0)
				.value().get();

		// Find account
		CenturiaAccount account = AccountManager.getInstance().getAccount(id.split("/")[1]);
		if (account == null)
			return Mono.empty();
		String mode = id.split("/")[2];

		boolean replySent = false;
		if (account.getOnlinePlayerInstance() != null) {
			if (mode.equals("tosender"))
				Centuria.systemMessage(account.getOnlinePlayerInstance(),
						"Reply to your user report received.\nRead the DM by the server for info.\n\n" + reply
								+ "\n\nSent by: " + event.getInteraction().getUser().getTag(),
						true);
			else if (mode.equals("tosubject"))
				Centuria.systemMessage(account.getOnlinePlayerInstance(),
						"Please read the private message.\nServer staff has contacted you via DMs, please read.\n\n"
								+ reply,
						true);
			replySent = true;
		}
		try {
			if (LinkUtils.isPairedWithDiscord(account)) {
				String discord = LinkUtils.getDiscordAccountFrom(account);
				User owner = DiscordBotModule.getClient().getUserById(Snowflake.of(discord)).block();

				EmbedCreateSpec.Builder embed = EmbedCreateSpec.builder();
				if (mode.equals("tosender")) {
					embed.title("Response to your User Report");
					embed.color(Color.DARK_GOLDENROD);
					embed.footer(event.getInteraction().getUser().getTag(),
							event.getInteraction().getUser().getAvatarUrl());
				} else if (mode.equals("tosubject")) {
					embed.title("Centuria Moderation Message");
					embed.color(Color.RED);
				}
				embed.description(reply);
				MessageCreateSpec.Builder replyMsg = MessageCreateSpec.builder();
				replyMsg.addEmbed(embed.build());

				owner.getPrivateChannel().block(Duration.ofSeconds(10)).createMessage(replyMsg.build())
						.block(Duration.ofSeconds(10));
				replySent = true;
			}
		} catch (Exception e) {
		}
		if (!replySent) {
			return event.reply(
					"Failed to send the reply to the player, they are not online and neither are they accepting server DMs (do they havea discord account paired? if they do they arent accepting dms from the bot)")
					.withEphemeral(true);
		}
		return event.deferEdit();
	}

}
