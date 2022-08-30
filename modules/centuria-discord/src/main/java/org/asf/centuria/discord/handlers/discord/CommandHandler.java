package org.asf.centuria.discord.handlers.discord;

import java.util.ArrayList;
import java.util.Optional;

import org.asf.centuria.accounts.CenturiaAccount;
import org.asf.centuria.discord.DiscordBotModule;
import org.asf.centuria.discord.LinkUtils;
import org.asf.centuria.discord.ServerConfigUtils;
import org.reactivestreams.Publisher;

import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ApplicationCommandInteractionEvent;
import discord4j.core.object.command.ApplicationCommandOption;
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
import discord4j.discordjson.json.ApplicationCommandInteractionData;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.rest.util.Color;
import discord4j.rest.util.Permission;
import reactor.core.publisher.Mono;

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
						"**Centuria server configuration.**\nPlease select below which setting you wish to change.");

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
				CenturiaAccount account = LinkUtils.getAccountByDiscordID(userID);
				if (account != null) {
					// Build message
					String msg = "Centuria account details:\n";
					msg += "**Ingame display name**: `" + account.getDisplayName() + "`\n";
					msg += "**Last login**: " + (account.getLastLoginTime() == -1 ? "`Unknown`"
							: "<t:" + account.getLastLoginTime() + ">") + "\n";
					msg += "**Status:** " + (account.isBanned() ? "banned"
							: (account.isMuted() ? "muted"
									: (account.getOnlinePlayerInstance() != null ? "online" : "offline")));
					channel.createMessage(msg).subscribe();
				} else {
					// Return error
					channel.createMessage("The given member has no Centuria account linked to their Discord account.")
							.subscribe();
				}
			}
			break;
		}
		}
	}

	/**
	 * The setup command
	 */
	public static ApplicationCommandOptionData setupCommand() {
		return ApplicationCommandOptionData.builder().name("setup").description("Server configuration command")
				.type(ApplicationCommandOption.Type.SUB_COMMAND.getValue()).build();
	}

	/**
	 * The account panel setup command
	 */
	public static ApplicationCommandOptionData createAccountPanel() {
		return ApplicationCommandOptionData.builder().name("createaccountpanel")
				.description("Account panel creation command")
				.type(ApplicationCommandOption.Type.SUB_COMMAND.getValue()).build();
	}

	/**
	 * The account info command
	 */
	public static ApplicationCommandOptionData getAccountInfo() {
		return ApplicationCommandOptionData.builder().name("getaccountinfo")
				.description("Account info retrieval command")
				.addOption(ApplicationCommandOptionData.builder().name("member")
						.type(ApplicationCommandOption.Type.USER.getValue())
						.description("User to retrieve the Centuria account details from").required(true).build())
				.type(ApplicationCommandOption.Type.SUB_COMMAND.getValue()).build();
	}

	/**
	 * Handles slash commands
	 * 
	 * @param event   Command event
	 * @param guild   Guild it was run in
	 * @param gateway Client
	 */
	public static Publisher<Object> handle(ApplicationCommandInteractionEvent event, Guild guild,
			GatewayDiscordClient gateway) {
		ApplicationCommandInteractionData data = (ApplicationCommandInteractionData) event.getInteraction().getData().data().get();
		String command = data.name().get();
		if (command.equalsIgnoreCase("centuria")) {
			// Right command, find the subcommand
			String subCmd = data.options().get().get(0).name();
			subCmd = subCmd;
		}
		return Mono.empty();
	}

}
