package org.asf.centuria.discord.handlers.discord;

import java.util.Arrays;

import org.asf.centuria.accounts.AccountManager;
import org.asf.centuria.accounts.CenturiaAccount;
import org.asf.centuria.discord.DiscordBotModule;
import org.asf.centuria.discord.LinkUtils;
import org.asf.centuria.networking.gameserver.GameServer;

import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ApplicationCommandInteractionEvent;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.SelectMenu;
import discord4j.core.object.component.SelectMenu.Option;
import discord4j.core.object.component.TextInput;
import discord4j.core.object.entity.Guild;
import discord4j.core.spec.InteractionApplicationCommandCallbackSpec;
import discord4j.discordjson.json.ApplicationCommandInteractionData;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.rest.util.Permission;
import reactor.core.publisher.Mono;

public class CommandHandler {

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
	 * The account info command (discord)
	 */
	public static ApplicationCommandOptionData getDiscordAccountInfo() {
		return ApplicationCommandOptionData.builder().name("getdiscord").description("Link info retrieval command")
				.addOption(ApplicationCommandOptionData.builder().name("centuria-displayname")
						.type(ApplicationCommandOption.Type.STRING.getValue())
						.description("Player to retrieve the Discord account details from").required(true).build())
				.type(ApplicationCommandOption.Type.SUB_COMMAND.getValue()).build();
	}

	/**
	 * The account kick command
	 */
	public static ApplicationCommandOptionData kick() {
		return ApplicationCommandOptionData.builder().name("kick").description("Kick a player")
				.addOption(ApplicationCommandOptionData.builder().name("centuria-displayname")
						.type(ApplicationCommandOption.Type.STRING.getValue()).description("Player to kick")
						.required(true).build())
				.addOption(ApplicationCommandOptionData.builder().name("reason")
						.type(ApplicationCommandOption.Type.STRING.getValue()).description("Kick reason").build())
				.type(ApplicationCommandOption.Type.SUB_COMMAND.getValue()).build();
	}

	/**
	 * The account ban command
	 */
	public static ApplicationCommandOptionData ban() {
		return ApplicationCommandOptionData.builder().name("permban").description("Permanently bans a player")
				.addOption(ApplicationCommandOptionData.builder().name("centuria-displayname")
						.type(ApplicationCommandOption.Type.STRING.getValue()).description("Player to ban")
						.required(true).build())
				.addOption(ApplicationCommandOptionData.builder().name("reason")
						.type(ApplicationCommandOption.Type.STRING.getValue()).description("Ban reason").build())
				.type(ApplicationCommandOption.Type.SUB_COMMAND.getValue()).build();
	}

	/**
	 * The account ban command
	 */
	public static ApplicationCommandOptionData tempBan() {
		return ApplicationCommandOptionData.builder().name("tempban").description("Temporarily bans a player")
				.addOption(ApplicationCommandOptionData.builder().name("centuria-displayname")
						.type(ApplicationCommandOption.Type.STRING.getValue()).description("Player to ban")
						.required(true).build())
				.addOption(ApplicationCommandOptionData.builder().name("days")
						.type(ApplicationCommandOption.Type.INTEGER.getValue())
						.description("Days to ban the player for").required(true).build())
				.addOption(ApplicationCommandOptionData.builder().name("reason")
						.type(ApplicationCommandOption.Type.STRING.getValue()).description("Ban reason").build())
				.type(ApplicationCommandOption.Type.SUB_COMMAND.getValue()).build();
	}

	/**
	 * The account pardon command
	 */
	public static ApplicationCommandOptionData pardon() {
		return ApplicationCommandOptionData.builder().name("pardon").description("Removes player penalties")
				.addOption(ApplicationCommandOptionData.builder().name("centuria-displayname")
						.type(ApplicationCommandOption.Type.STRING.getValue()).description("Player to ban")
						.required(true).build())
				.addOption(ApplicationCommandOptionData.builder().name("reason")
						.type(ApplicationCommandOption.Type.STRING.getValue()).description("Pardon reason").build())
				.type(ApplicationCommandOption.Type.SUB_COMMAND.getValue()).build();
	}

	/**
	 * Handles slash commands
	 * 
	 * @param event   Command event
	 * @param guild   Guild it was run in
	 * @param gateway Client
	 */
	public static Mono<?> handle(ApplicationCommandInteractionEvent event, Guild guild,
			GatewayDiscordClient gateway) {
		ApplicationCommandInteractionData data = (ApplicationCommandInteractionData) event.getInteraction().getData()
				.data().get();
		String command = data.name().get();
		if (command.equalsIgnoreCase("centuria")) {
			// Right command, find the subcommand
			String subCmd = data.options().get().get(0).name();
			switch (subCmd) {
			case "getdiscord": {
				// Required permissions: mod (ingame)
				CenturiaAccount modacc = LinkUtils
						.getAccountByDiscordID(event.getInteraction().getUser().getId().asString());
				if (modacc == null) {
					event.reply("**Error:** You dont have a Centuria account linked to your Discord account").block();
					return Mono.empty();
				}

				String permLevel = "member";
				if (modacc.getPlayerInventory().containsItem("permissions")) {
					permLevel = modacc.getPlayerInventory().getItem("permissions").getAsJsonObject()
							.get("permissionLevel").getAsString();
				}
				if (!GameServer.hasPerm(permLevel, "moderator")) {
					event.reply("**Error:** no Centuria moderator permissions.").block();
					return Mono.empty();
				}

				// Find player UUID
				String uuid = AccountManager.getInstance()
						.getUserByDisplayName(data.options().get().get(0).options().get().get(0).value().get());
				if (uuid == null) {
					// Respond with error message
					event.reply("**Error:** player not recognized.").block();
					return Mono.empty();
				}
				CenturiaAccount acc = AccountManager.getInstance().getAccount(uuid);
				if (acc == null) {
					// Respond with error message
					event.reply("**Error:** player not recognized.").block();
					return Mono.empty();
				}

				// Check account link
				String userID = LinkUtils.getDiscordAccountFrom(acc);
				if (userID == null) {
					// Respond with error message
					event.reply("**Error:** the specified account has not been paired with any Discord account.")
							.block();
					return Mono.empty();
				}

				// Show account info
				String res = "Discord user ID: " + userID;
				try {
					res = "Discord user: `"
							+ DiscordBotModule.getClient().getUserById(Snowflake.of(userID)).block().getTag() + " ("
							+ userID + ")`";
				} catch (Exception e) {
				}
				event.reply(res).block();
				break;
			}
			case "kick": {
				// Required permissions: mod (ingame)
				CenturiaAccount modacc = LinkUtils
						.getAccountByDiscordID(event.getInteraction().getUser().getId().asString());
				if (modacc == null) {
					event.reply("**Error:** You dont have a Centuria account linked to your Discord account").block();
					return Mono.empty();
				}

				String permLevel = "member";
				if (modacc.getPlayerInventory().containsItem("permissions")) {
					permLevel = modacc.getPlayerInventory().getItem("permissions").getAsJsonObject()
							.get("permissionLevel").getAsString();
				}
				if (!GameServer.hasPerm(permLevel, "moderator")) {
					event.reply("**Error:** no Centuria moderator permissions.").block();
					return Mono.empty();
				}

				// Find player UUID
				var params = data.options().get().get(0).options().get();
				String uuid = AccountManager.getInstance().getUserByDisplayName(params.get(0).value().get());
				if (uuid == null) {
					// Respond with error message
					event.reply("**Error:** player not recognized.").block();
					return Mono.empty();
				}
				CenturiaAccount acc = AccountManager.getInstance().getAccount(uuid);
				if (acc == null) {
					// Respond with error message
					event.reply("**Error:** player not recognized.").block();
					return Mono.empty();
				}
				if (acc.getOnlinePlayerInstance() == null) {
					event.reply("**Error:** player not online.").block();
					return Mono.empty();
				}

				// Kick
				if (params.size() == 1) {
					event.reply("Kicked player " + acc.getDisplayName()).block();
					acc.kick(modacc.getAccountID(), null);
				} else if (params.size() == 2) {
					event.reply("Kicked player " + acc.getDisplayName() + ": " + params.get(1).value().get()).block();
					acc.kick(modacc.getAccountID(), params.get(1).value().get());
				}
				break;
			}
			case "permban": {
				// Required permissions: mod (ingame)
				CenturiaAccount modacc = LinkUtils
						.getAccountByDiscordID(event.getInteraction().getUser().getId().asString());
				if (modacc == null) {
					event.reply("**Error:** You dont have a Centuria account linked to your Discord account").block();
					return Mono.empty();
				}

				String permLevel = "member";
				if (modacc.getPlayerInventory().containsItem("permissions")) {
					permLevel = modacc.getPlayerInventory().getItem("permissions").getAsJsonObject()
							.get("permissionLevel").getAsString();
				}
				if (!GameServer.hasPerm(permLevel, "moderator")) {
					event.reply("**Error:** no Centuria moderator permissions.").block();
					return Mono.empty();
				}

				// Find player UUID
				var params = data.options().get().get(0).options().get();
				String uuid = AccountManager.getInstance().getUserByDisplayName(params.get(0).value().get());
				if (uuid == null) {
					// Respond with error message
					event.reply("**Error:** player not recognized.").block();
					return Mono.empty();
				}
				CenturiaAccount acc = AccountManager.getInstance().getAccount(uuid);
				if (acc == null) {
					// Respond with error message
					event.reply("**Error:** player not recognized.").block();
					return Mono.empty();
				}
				if (acc.isBanned()) {
					event.reply("**Error:** player is already banned.").block();
					return Mono.empty();
				}

				// Ban
				if (params.size() == 1) {
					event.reply("Banned player " + acc.getDisplayName()).block();
					acc.ban(modacc.getAccountID(), null);
				} else if (params.size() == 2) {
					event.reply("Banned player " + acc.getDisplayName() + ": " + params.get(1).value().get()).block();
					acc.ban(modacc.getAccountID(), params.get(1).value().get());
				}
				break;
			}
			case "tempban": {
				// Required permissions: mod (ingame)
				CenturiaAccount modacc = LinkUtils
						.getAccountByDiscordID(event.getInteraction().getUser().getId().asString());
				if (modacc == null) {
					event.reply("**Error:** You dont have a Centuria account linked to your Discord account").block();
					return Mono.empty();
				}

				String permLevel = "member";
				if (modacc.getPlayerInventory().containsItem("permissions")) {
					permLevel = modacc.getPlayerInventory().getItem("permissions").getAsJsonObject()
							.get("permissionLevel").getAsString();
				}
				if (!GameServer.hasPerm(permLevel, "moderator")) {
					event.reply("**Error:** no Centuria moderator permissions.").block();
					return Mono.empty();
				}

				// Find player UUID
				var params = data.options().get().get(0).options().get();
				String uuid = AccountManager.getInstance().getUserByDisplayName(params.get(0).value().get());
				if (uuid == null) {
					// Respond with error message
					event.reply("**Error:** player not recognized.").block();
					return Mono.empty();
				}
				CenturiaAccount acc = AccountManager.getInstance().getAccount(uuid);
				if (acc == null) {
					// Respond with error message
					event.reply("**Error:** player not recognized.").block();
					return Mono.empty();
				}
				if (acc.isBanned()) {
					event.reply("**Error:** player is already banned.").block();
					return Mono.empty();
				}

				// Tempban
				if (params.size() == 2) {
					event.reply("Temporarily banned player " + acc.getDisplayName()).block();
					acc.tempban(Integer.valueOf(params.get(1).value().get()), modacc.getAccountID(), null);
				} else if (params.size() == 3) {
					event.reply(
							"Temporarily banned player " + acc.getDisplayName() + ": " + params.get(2).value().get())
							.block();
					acc.tempban(Integer.valueOf(params.get(1).value().get()), modacc.getAccountID(),
							params.get(2).value().get());
				}
				break;
			}
			case "pardon": {
				// Required permissions: mod (ingame)
				CenturiaAccount modacc = LinkUtils
						.getAccountByDiscordID(event.getInteraction().getUser().getId().asString());
				if (modacc == null) {
					event.reply("**Error:** You dont have a Centuria account linked to your Discord account").block();
					return Mono.empty();
				}

				String permLevel = "member";
				if (modacc.getPlayerInventory().containsItem("permissions")) {
					permLevel = modacc.getPlayerInventory().getItem("permissions").getAsJsonObject()
							.get("permissionLevel").getAsString();
				}
				if (!GameServer.hasPerm(permLevel, "moderator")) {
					event.reply("**Error:** no Centuria moderator permissions.").block();
					return Mono.empty();
				}

				// Find player UUID
				var params = data.options().get().get(0).options().get();
				String uuid = AccountManager.getInstance().getUserByDisplayName(params.get(0).value().get());
				if (uuid == null) {
					// Respond with error message
					event.reply("**Error:** player not recognized.").block();
					return Mono.empty();
				}
				CenturiaAccount acc = AccountManager.getInstance().getAccount(uuid);
				if (acc == null) {
					// Respond with error message
					event.reply("**Error:** player not recognized.").block();
					return Mono.empty();
				}
				if (!acc.isBanned() && !acc.isMuted()) {
					event.reply("**Error:** player has no penalties.").block();
					return Mono.empty();
				}

				// Pardon
				if (params.size() == 1) {
					event.reply("Pardoned player " + acc.getDisplayName()).block();
					acc.pardon(modacc.getAccountID(), null);
				} else if (params.size() == 2) {
					event.reply("Pardoned player " + acc.getDisplayName()).block();
					acc.pardon(modacc.getAccountID(), params.get(1).value().get());
				}
				break;
			}
			case "getaccountinfo": {
				// Required permissions: mod (ingame)
				CenturiaAccount acc = LinkUtils
						.getAccountByDiscordID(event.getInteraction().getUser().getId().asString());
				if (acc == null) {
					event.reply("**Error:** You dont have a Centuria account linked to your Discord account").block();
					return Mono.empty();
				}

				String permLevel = "member";
				if (acc.getPlayerInventory().containsItem("permissions")) {
					permLevel = acc.getPlayerInventory().getItem("permissions").getAsJsonObject().get("permissionLevel")
							.getAsString();
				}
				if (!GameServer.hasPerm(permLevel, "moderator")) {
					event.reply("**Error:** no Centuria moderator permissions.").block();
					return Mono.empty();
				}

				// Find member
				String userID = data.options().get().get(0).options().get().get(0).value().get();
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
					event.reply(msg).subscribe();
				} else {
					// Return error
					event.reply("The given member has no Centuria account linked to their Discord account.")
							.subscribe();
				}
			}
			case "setup": {
				// Required permissions: admin (ingame), admin (discord)
				CenturiaAccount acc = LinkUtils
						.getAccountByDiscordID(event.getInteraction().getUser().getId().asString());
				if (acc == null) {
					event.reply(
							"**Error:** You dont have a Centuria account linked to your Discord account, if you are the owner of the Discord and Centuria server,\nplease link your account manually if the panel is not yet made.\n"
									+ "\n" + "To manually link your account (requires game server ownership):\n"
									+ "1. edit `accountlink.json`\n" + "2. add the following line between the `{}`: `\""
									+ event.getInteraction().getUser().getId().asString()
									+ "\":\"<insert-account-uuid>\"`\n"
									+ "3. go into the inventories folder, your account UUID, and create a new file: `pairedaccount.json`\n"
									+ "4. write the following to it: `{\"userId\":\""
									+ event.getInteraction().getUser().getId().asString() + "\"}`\n"
									+ "5. restart the server")
							.block();
					return Mono.empty();
				}

				// Check permissions
				if (!event.getInteraction().getUser().asMember(guild.getId()).block().getBasePermissions().block()
						.contains(Permission.ADMINISTRATOR)) {
					event.reply("**Error:** no Discord administrative permissions.").block();
					return Mono.empty();
				}
				String permLevel = "member";
				if (acc.getPlayerInventory().containsItem("permissions")) {
					permLevel = acc.getPlayerInventory().getItem("permissions").getAsJsonObject().get("permissionLevel")
							.getAsString();
				}
				if (!GameServer.hasPerm(permLevel, "admin")) {
					event.reply("**Error:** no Centuria administrative permissions.").block();
					return Mono.empty();
				}

				// Create message
				InteractionApplicationCommandCallbackSpec.Builder msg = InteractionApplicationCommandCallbackSpec
						.builder();

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
				msg.ephemeral(true);

				// Send message
				event.reply(msg.build()).block();
			}
			case "createaccountpanel": {
				// Required permissions: admin (ingame), admin (discord)
				CenturiaAccount acc = LinkUtils
						.getAccountByDiscordID(event.getInteraction().getUser().getId().asString());
				if (acc == null) {
					event.reply(
							"**Error:** You dont have a Centuria account linked to your Discord account, if you are the owner of the Discord and Centuria server,\nplease link your account manually if the panel is not yet made.\n"
									+ "\n" + "To manually link your account (requires game server ownership):\n"
									+ "1. edit `accountlink.json`\n" + "2. add the following line between the `{}`: `\""
									+ event.getInteraction().getUser().getId().asString()
									+ "\":\"<insert-account-uuid>\"`\n"
									+ "3. go into the inventories folder, your account UUID, and create a new file: `pairedaccount.json`\n"
									+ "4. write the following to it: `{\"userId\":\""
									+ event.getInteraction().getUser().getId().asString() + "\"}`\n"
									+ "5. restart the server")
							.block();
					return Mono.empty();
				}

				// Check permissions
				if (!event.getInteraction().getUser().asMember(guild.getId()).block().getBasePermissions().block()
						.contains(Permission.ADMINISTRATOR)) {
					event.reply("**Error:** no Discord administrative permissions.").block();
					return Mono.empty();
				}
				String permLevel = "member";
				if (acc.getPlayerInventory().containsItem("permissions")) {
					permLevel = acc.getPlayerInventory().getItem("permissions").getAsJsonObject().get("permissionLevel")
							.getAsString();
				}
				if (!GameServer.hasPerm(permLevel, "admin")) {
					event.reply("**Error:** no Centuria administrative permissions.").block();
					return Mono.empty();
				}

				// Show modal
				event.presentModal("Account Panel Creation", "createaccountpanel",
						Arrays.asList(ActionRow.of(TextInput.paragraph("message", "Message description")))).block();

				break;
			}
			}
		}
		return Mono.empty();
	}

}