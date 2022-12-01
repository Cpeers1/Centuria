package org.asf.centuria.discord.handlers.discord.interactions.forms;

import java.time.Duration;

import org.asf.centuria.Centuria;
import org.asf.centuria.accounts.AccountManager;
import org.asf.centuria.accounts.CenturiaAccount;
import org.asf.centuria.discord.DiscordBotModule;
import org.asf.centuria.discord.LinkUtils;
import org.asf.centuria.discord.TimedActions;

import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ModalSubmitInteractionEvent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.component.Button.Style;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.rest.util.Color;
import reactor.core.publisher.Mono;

public class FeedbackReplyHandler {

	/**
	 * Handles the feeedback reply form submission event
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

		// Find message
		Message msg = event.getMessage().get().getChannel().block().getMessageById(event.getMessageId()).block();
		ActionRow row = (ActionRow) msg.getComponents().get(0);
		Button btn = (Button) row.getChildren().get(0);
		if (btn.getStyle() == Style.DANGER) {
			String codeConfirm = TimedActions.addAction(account.getAccountID() + "-2fa-allow", () -> {
				boolean replySent = false;
				if (account.getOnlinePlayerInstance() != null) {
					Centuria.systemMessage(account.getOnlinePlayerInstance(),
							"Reply to your feedback report received.\nRead the DM by the server for info.\n\n" + reply
									+ "\n\nSent by: " + event.getInteraction().getUser().getTag(),
							true);
					replySent = true;
				}
				try {
					if (LinkUtils.isPairedWithDiscord(account)) {
						String discord = LinkUtils.getDiscordAccountFrom(account);
						User owner = DiscordBotModule.getClient().getUserById(Snowflake.of(discord)).block();

						EmbedCreateSpec.Builder embed = EmbedCreateSpec.builder();
						embed.title("Response to your Ingame Feedback");
						embed.color(Color.GREEN);
						embed.description(reply);
						embed.footer(event.getInteraction().getUser().getTag(),
								event.getInteraction().getUser().getAvatarUrl());
						MessageCreateSpec.Builder replyMsg = MessageCreateSpec.builder();
						replyMsg.addEmbed(embed.build());

						owner.getPrivateChannel().block(Duration.ofSeconds(10)).createMessage(replyMsg.build())
								.block(Duration.ofSeconds(10));
						replySent = true;
					}
				} catch (Exception e) {
				}
				if (!replySent) {
					event.reply(
							"Failed to send the reply to the player, they are not online and neither are they accepting server DMs (do they havea discord account paired? if they do they arent accepting dms from the bot)")
							.withEphemeral(true).block();
					return;
				}

				event.getMessage().get().edit()
						.withComponents(ActionRow.of(Button.danger("feedbackreply/" + account.getAccountID(),
								"Reply (done by " + event.getInteraction().getUser().getTag() + ")")))
						.subscribe();
			}, 15 * 60);
			return event
					.reply("Warning!\n\n" + event.getInteraction().getUser().getMention()
							+ ", someone already replied to this feedback report, do you wish to continue?")
					.withComponents(ActionRow.of(Button.danger("feedbackreply/" + codeConfirm, "Send anyways"),
							Button.primary("dismissDelete", "Cancel")));
		} else {
			boolean replySent = false;
			if (account.getOnlinePlayerInstance() != null) {
				Centuria.systemMessage(account.getOnlinePlayerInstance(),
						"Reply to your feedback report received.\nRead the DM by the server for info.\n\n" + reply
								+ "\n\nSent by: " + event.getInteraction().getUser().getTag(),
						true);
				replySent = true;
			}
			try {
				if (LinkUtils.isPairedWithDiscord(account)) {
					String discord = LinkUtils.getDiscordAccountFrom(account);
					User owner = DiscordBotModule.getClient().getUserById(Snowflake.of(discord)).block();

					EmbedCreateSpec.Builder embed = EmbedCreateSpec.builder();
					embed.title("Response to your Ingame Feedback");
					embed.color(Color.GREEN);
					embed.description(reply);
					embed.footer(event.getInteraction().getUser().getTag(),
							event.getInteraction().getUser().getAvatarUrl());
					MessageCreateSpec.Builder replyMsg = MessageCreateSpec.builder();
					replyMsg.addEmbed(embed.build());

					owner.getPrivateChannel().block(Duration.ofSeconds(10)).createMessage(replyMsg.build())
							.block(Duration.ofSeconds(10));
					replySent = true;
				}
			} catch (Exception e) {
			}
			if (!replySent)
				return event.reply(
						"Failed to send the reply to the player, they are not online and neither are they accepting server DMs (do they havea discord account paired? if they do they arent accepting dms from the bot)")
						.withEphemeral(true);

			event.getMessage().get().edit()
					.withComponents(ActionRow.of(Button.danger("feedbackreply/" + account.getAccountID(),
							"Reply (done by " + event.getInteraction().getUser().getTag() + ")")))
					.subscribe();
		}
		return event.deferEdit();
	}

}
