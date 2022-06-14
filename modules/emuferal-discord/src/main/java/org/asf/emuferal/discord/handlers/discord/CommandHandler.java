package org.asf.emuferal.discord.handlers.discord;

import java.util.ArrayList;
import java.util.Optional;

import org.asf.emuferal.accounts.EmuFeralAccount;
import org.asf.emuferal.discord.DiscordBotModule;
import org.asf.emuferal.discord.LinkUtils;
import org.asf.emuferal.discord.ServerConfigUtils;

import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.component.SelectMenu;
import discord4j.core.object.component.SelectMenu.Option;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.retriever.EntityRetrievalStrategy;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.rest.util.Color;
import discord4j.rest.util.Permission;

public class CommandHandler {

	// Command parser
	private static ArrayList<String> parseCommand(String args) {
		ArrayList<String> args3 = new ArrayList<String>();
		char[] argarray = args.toCharArray();
		boolean ignorespaces = false;
		String last = "";
		int i = 0;
		for (char c : args.toCharArray()) {
			if (c == '"' && (i == 0 || argarray[i - 1] != '\\')) {
				if (ignorespaces)
					ignorespaces = false;
				else
					ignorespaces = true;
			} else if (c == ' ' && !ignorespaces && (i == 0 || argarray[i - 1] != '\\')) {
				args3.add(last);
				last = "";
			} else if (c != '\\' || (i + 1 < argarray.length && argarray[i + 1] != '"'
					&& (argarray[i + 1] != ' ' || ignorespaces))) {
				last += c;
			}

			i++;
		}

		if (last == "" == false)
			args3.add(last);

		return args3;
	}

	/**
	 * Handles the command message
	 *
	 * @param command Command string
	 * @param channel Channel where the command was run
	 * @param member  Command invoker
	 * @param gateway Gateway client
	 */
	public static void handle(String command, Guild guild, MessageChannel channel, Member owner,
			GatewayDiscordClient gateway) {
		String argsStr = command;
		if (argsStr.contains(" "))
			argsStr = argsStr.substring(argsStr.indexOf(" ") + 1);
		else
			argsStr = "";
		ArrayList<String> args = parseCommand(command);
		command = args.remove(0).toLowerCase();

		// Find the command
		switch (command) {
		case "createaccountpanel": {
			// Verify admin
			if (owner.getBasePermissions().block().contains(Permission.ADMINISTRATOR)) {
				// Verify arguments
				if (argsStr.isBlank()) {
					channel.createMessage(
							"Unable to create a empty account panel, please run this command followed by a description.")
							.subscribe();
					return;
				}

				// Build message
				argsStr = argsStr.trim()
						+ "\n\nPlease enable DMs for this server, many interactions with the bot are done via DM.";
				if (argsStr.length() > 4000) {
					// Warn about the length
					channel.createMessage("Embed description is too long, please make a shorter message.").subscribe();
					return;
				}
				MessageCreateSpec.Builder msg = MessageCreateSpec.builder();

				// Build embed
				EmbedCreateSpec.Builder embed = EmbedCreateSpec.builder();
				embed.color(Color.BLUE);
				embed.title(DiscordBotModule.getServerName());
				embed.description(argsStr);
				embed.footer(DiscordBotModule.getServerName(), gateway.getSelf().block().getAvatarUrl());
				msg.addEmbed(embed.build());

				// Buttons
				msg.addComponent(ActionRow.of(Button.success("accountpanel", "Open account panel"),
						Button.primary("register", "Register new account"),
						Button.primary("pair", "Pair existing account")));

				// Send message
				channel.createMessage(msg.build()).subscribe();
				break;
			}
		}
		case "serverconfig": {
			// Verify admin
			if (owner.getBasePermissions().block().contains(Permission.ADMINISTRATOR)) {
				// Create message
				MessageCreateSpec.Builder msg = MessageCreateSpec.builder();

				// Message content
				msg.content(
						"**EmuFeral server configuration.**\nPlease select below which setting you wish to change.");

				// Dropdown
				msg.addComponent(ActionRow.of(SelectMenu.of("serverconfig", Option.of("Moderator role", "modrole"),
						Option.of("Developer role", "devrole"), Option.of("Announcement ping role", "announcementrole"),
						Option.of("Announcement channel", "announcementchannel"),
						Option.of("Member report review channel", "reportchannel"),
						Option.of("Moderation log channel", "moderationlogchannel"),
						Option.of("Feedback review channel", "feedbackchannel"))));

				// Send message
				channel.createMessage(msg.build()).subscribe();
			}
			break;
		}
		case "getaccountinfo": {
			final String modRole;

			// Find moderator role
			if (ServerConfigUtils.getServerConfig(guild.getId().asString()).has("moderatorRole"))
				modRole = ServerConfigUtils.getServerConfig(guild.getId().asString()).get("moderatorRole")
						.getAsString();
			else
				modRole = "";

			// Verify admin or moderator
			if (owner.getRoleIds().stream().anyMatch(t -> t.asString().equals(modRole))
					|| owner.getBasePermissions().block().contains(Permission.ADMINISTRATOR)) {
				// Verify arguments
				if (args.size() < 1) {
					channel.createMessage("Missing argument: user-mention").subscribe();
					return;
				}

				// Find member
				String user = args.get(0);
				if (user.matches("^\\<\\@\\![0-9]+\\>$")) {
					user = user.substring(3);
					user = user.substring(0, user.lastIndexOf(">"));
				} else if (user.matches("^\\<\\@[0-9]+\\>$")) {
					user = user.substring(2);
					user = user.substring(0, user.lastIndexOf(">"));
				}
				final String userD = user;
				Optional<Member> mem = guild.getMembers(EntityRetrievalStrategy.STORE_FALLBACK_REST).toStream()
						.filter(t -> t.getId().asString().equals(userD)
								|| (t.getNickname().isPresent() && t.getNickname().get().equalsIgnoreCase(userD))
								|| t.getUsername().equalsIgnoreCase(userD))
						.findFirst();
				if (!mem.isPresent()) {
					channel.createMessage("Invalid argument: user-mention").subscribe();
					return;
				}

				// Get data
				String userID = mem.get().getId().asString();
				EmuFeralAccount account = LinkUtils.getAccountByDiscordID(userID);
				if (account != null) {
					// Build message
					String msg = "EmuFeral account details:\n";
					msg += "**Ingame display name**: `" + account.getDisplayName() + "`\n";
					msg += "**Last login**: " + (account.getLastLoginTime() == -1 ? "`Unknown`"
							: "<t:" + account.getLastLoginTime() + ">") + "\n";
					msg += "**Status:** " + (account.isBanned() ? "banned"
							: (account.isMuted() ? "muted"
									: (account.getOnlinePlayerInstance() != null ? "online" : "offline")));
					channel.createMessage(msg).subscribe();
				} else {
					// Return error
					channel.createMessage("The given member has no EmuFeral account linked to their Discord account.")
							.subscribe();
				}
			}
			break;
		}
		}
	}

}
