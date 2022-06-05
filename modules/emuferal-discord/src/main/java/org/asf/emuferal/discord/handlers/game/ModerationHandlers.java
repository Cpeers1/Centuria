package org.asf.emuferal.discord.handlers.game;

import org.asf.emuferal.discord.DiscordBotModule;
import org.asf.emuferal.discord.LinkUtils;
import org.asf.emuferal.discord.ServerConfigUtils;
import org.asf.emuferal.modules.eventbus.EventListener;
import org.asf.emuferal.modules.eventbus.IEventReceiver;
import org.asf.emuferal.modules.events.accounts.AccountBanEvent;
import org.asf.emuferal.modules.events.accounts.AccountMuteEvent;
import org.asf.emuferal.modules.events.accounts.AccountPardonEvent;

import com.google.gson.JsonObject;

import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Guild;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.rest.util.Color;

public class ModerationHandlers implements IEventReceiver {

	@EventListener
	public void handleBan(AccountBanEvent ev) {
		// Find discord client (if present)
		String userID = LinkUtils.getDiscordAccountFrom(ev.getAccount());

		// Log moderation
		moderationLog("Ban", userID, ev.getAccount().getDisplayName(), ev.getAccount().getAccountID(),
				ev.isPermanent() ? "Ban type: **Permanent**" : ("Ban type: **Temporary** (" + ev.getDays() + " days)"));

		if (userID != null) {
			// DM them
			try {
				EmbedCreateSpec.Builder embed = EmbedCreateSpec.builder();

				// Description content
				String message = "You have been banned from our servers for unacceptable behaviour.\n";
				message += "\n";
				if (ev.isPermanent()) {
//					message += "This is a permanent ban, you can attempt to appeal by pressing the button below.";
					message += "This is a permanent ban, you can attempt to appeal by contacting the server staff.";
				} else
					message += "This is a temporary ban, you cannot log on for " + ev.getDays() + " days.";

				// Embed
				embed.title((ev.isPermanent() ? "Permanently" : "Temporarily") + " banned from "
						+ DiscordBotModule.getServerName());
				embed.color(Color.RED);
				embed.description(message);
				embed.footer(DiscordBotModule.getServerName(),
						DiscordBotModule.getClient().getSelf().block().getAvatarUrl());

				// Message object
				MessageCreateSpec.Builder msg = MessageCreateSpec.builder();
				msg.addEmbed(embed.build());

//				// Appeal button (if permanent)
//				if (ev.isPermanent()) {
//					msg.addComponent(ActionRow.of(Button.danger(
//							"appeal/" + userID + "/" + ev.getAccount().getAccountID(), "Appeal for pardon")));
//				}

				// Send response
				DiscordBotModule.getClient().getUserById(Snowflake.of(userID)).block().getPrivateChannel().block()
						.createMessage(msg.build()).subscribe();
			} catch (Exception e) {
			}
		}
	}

	private void moderationLog(String type, String userID, String displayName, String accountID, String data) {
		// Log moderation action to the moderation log
		String message = "**EmuFeral Moderation Log**\n";
		message += "\n";
		message += "Action: **" + type + "**\n";
		message += "Affected player: " + displayName + (userID != null ? " (<@!" + userID + ">)" : "");
		if (data != null)
			message += "\n" + data;
		message += "\nAction was taken on: <t:" + (System.currentTimeMillis() / 1000) + ">";

		// Send to all guild log channels
		for (Guild g : DiscordBotModule.getClient().getGuilds().toIterable()) {
			String guildID = g.getId().asString();
			JsonObject config = ServerConfigUtils.getServerConfig(guildID);
			if (config.has("moderationLogChannel")) {
				// Find channel
				String ch = config.get("moderationLogChannel").getAsString();
				String srvMessage = message;
				if (config.has("moderatorRole")) {
					// Add ping
					srvMessage += "\n\n<@&" + config.get("moderatorRole").getAsString() + ">";
				}

				// Attempt to send message
				try {
					g.getChannelById(Snowflake.of(ch)).block().getRestChannel().createMessage(srvMessage).block();
				} catch (Exception e) {
				}
			}
		}
	}

	@EventListener
	public void handleMute(AccountMuteEvent ev) {
		// Find discord client (if present)
		String userID = LinkUtils.getDiscordAccountFrom(ev.getAccount());

		// Log moderation
		moderationLog("Mute", userID, ev.getAccount().getDisplayName(), ev.getAccount().getAccountID(),
				"Unmute timestamp: <t:" + (ev.getUnmuteTimestamp() / 1000) + ">");

		if (userID != null) {
			// DM them
			try {
				EmbedCreateSpec.Builder embed = EmbedCreateSpec.builder();

				// Description content
				String message = "You have been muted for violating the server rules.\n";
				message += "You are not allowed to use public chat until <t:" + (ev.getUnmuteTimestamp() / 1000) + ">";

				// Embed
				embed.title("Public chat mute in " + DiscordBotModule.getServerName());
				embed.color(Color.ORANGE);
				embed.description(message);
				embed.footer(DiscordBotModule.getServerName(),
						DiscordBotModule.getClient().getSelf().block().getAvatarUrl());

				// Message object
				MessageCreateSpec.Builder msg = MessageCreateSpec.builder();
				msg.addEmbed(embed.build());

				// Send response
				DiscordBotModule.getClient().getUserById(Snowflake.of(userID)).block().getPrivateChannel().block()
						.createMessage(msg.build()).subscribe();
			} catch (Exception e) {
			}
		}
	}

	@EventListener
	public void handlePardon(AccountPardonEvent ev) {
		// Find discord client (if present)
		String userID = LinkUtils.getDiscordAccountFrom(ev.getAccount());

		// Log moderation
		moderationLog("Pardon", userID, ev.getAccount().getDisplayName(), ev.getAccount().getAccountID(), null);

		if (userID != null) {
			// DM them
			try {
				EmbedCreateSpec.Builder embed = EmbedCreateSpec.builder();

				// Description content
				String message = "Your account has been pardoned and all penalties have been removed.\n";
				message += "We are sorry about the inconvenience.";

				// Embed
				embed.title("Pardoned in " + DiscordBotModule.getServerName());
				embed.color(Color.GREEN);
				embed.description(message);
				embed.footer(DiscordBotModule.getServerName(),
						DiscordBotModule.getClient().getSelf().block().getAvatarUrl());

				// Message object
				MessageCreateSpec.Builder msg = MessageCreateSpec.builder();
				msg.addEmbed(embed.build());

				// Send response
				DiscordBotModule.getClient().getUserById(Snowflake.of(userID)).block().getPrivateChannel().block()
						.createMessage(msg.build()).subscribe();
			} catch (Exception e) {
			}
		}
	}

}
