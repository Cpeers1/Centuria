package org.asf.emuferal.discord.handlers.discord.interactions.selectmenu;

import java.util.ArrayList;

import org.asf.emuferal.discord.ServerConfigUtils;

import com.google.gson.JsonObject;

import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.SelectMenuInteractionEvent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.SelectMenu;
import discord4j.core.object.component.SelectMenu.Option;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Role;
import discord4j.core.object.entity.channel.GuildChannel;
import discord4j.core.object.entity.channel.Channel.Type;
import discord4j.core.retriever.EntityRetrievalStrategy;
import discord4j.core.spec.InteractionApplicationCommandCallbackSpec;
import reactor.core.publisher.Mono;

public class ServerConfigMenuHandler {

	/**
	 * Handles the 'server config' select menu event
	 * 
	 * @param event   Select menu event
	 * @param gateway Discord client
	 * @return Result Mono object
	 */
	public static Mono<?> handle(String id, SelectMenuInteractionEvent event, GatewayDiscordClient gateway) {
		// Find option and show results
		String option = event.getValues().get(0);
		switch (option) {
		case "modrole":
			return event.reply(roleSetting(
					"moderatorRole", "Moderator Role",
					new String[] { "moderator", "mod", "emuferal" }, event.getMessage().get().getGuild().block()));
		case "devrole":
			return event.reply(roleSetting("developerRole", "Developer Role",
					new String[] { "developer", "dev", "emuferal" }, event.getMessage().get().getGuild().block()));
		case "announcementrole":
			return event.reply(roleSetting("announcementPingRole", "Announcement Ping Role",
					new String[] { "announcement", "ping", "emuferal" }, event.getMessage().get().getGuild().block()));
		case "announcementchannel":
			return event.reply(channelSetting("announcementChannel", "Announcement Channel",
					new String[] { "announcement", "emuferal" }, event.getMessage().get().getGuild().block()));
		case "reportchannel":
			return event.reply(channelSetting("reportChannel", "Member Report Review Channel",
					new String[] { "moderator", "moderation", "report", "review", "emuferal" },
					event.getMessage().get().getGuild().block()));
		case "moderationlogchannel":
			return event.reply(channelSetting("moderationLogChannel", "Moderation Log Channel",
					new String[] { "moderator", "moderation", "log", "emuferal" },
					event.getMessage().get().getGuild().block()));
		case "feedbackchannel":
			return event.reply(channelSetting("feedbackChannel", "Feedback Review Channel",
					new String[] { "developer", "development", "feedback", "review", "emuferal" },
					event.getMessage().get().getGuild().block()));
		}

		// Default response
		return Mono.empty();
	}

	// Easy-access method for channels
	private static InteractionApplicationCommandCallbackSpec channelSetting(String configOptionName, String channelName,
			String[] filter, Guild guild) {
		// Find existing role
		String lastSelectedChannel = null;
		JsonObject obj = ServerConfigUtils.getServerConfig(guild.getId().asString());
		if (obj.has(configOptionName))
			lastSelectedChannel = obj.get(configOptionName).getAsString();

		// Build role list
		int i = 0;
		ArrayList<Option> roleSelectionList = new ArrayList<Option>();
		for (GuildChannel channel : guild.getChannels(EntityRetrievalStrategy.STORE_FALLBACK_REST).toIterable()) {
			if (channel.getType() != Type.GUILD_TEXT)
				continue;

			String gChId = channel.getId().asString();
			String gChName = channel.getName();

			// Show channels matching the filter first
			if (!filterApplies(gChName, filter))
				continue;
			Option opt = Option.of(gChName, gChId);
			if (lastSelectedChannel != null && lastSelectedChannel.equals(gChId))
				opt = opt.withDefault(true);
			roleSelectionList.add(opt);

			i++;
			// Prevent size from going past the discord limit
			if (i == 25)
				break;
		}
		for (GuildChannel channel : guild.getChannels(EntityRetrievalStrategy.STORE_FALLBACK_REST).toIterable()) {
			if (channel.getType() != Type.GUILD_TEXT)
				continue;

			String gChId = channel.getId().asString();
			String gChName = channel.getName();

			// Dont show channels that have already been shown
			if (filterApplies(gChName, filter))
				continue;
			Option opt = Option.of(gChName, gChId);
			if (lastSelectedChannel != null && lastSelectedChannel.equals(gChId))
				opt = opt.withDefault(true);
			roleSelectionList.add(opt);

			i++;
			// Prevent size from going past the discord limit
			if (i == 25)
				break;
		}

		// Build message
		InteractionApplicationCommandCallbackSpec.Builder msg = InteractionApplicationCommandCallbackSpec.builder();
		msg.content("Channel configuration: **" + channelName + "**");
		msg.addComponent(ActionRow.of(SelectMenu.of("serveroptionselection/" + configOptionName, roleSelectionList)));

		// Return message
		return msg.ephemeral(true).build();
	}

	// Easy-access method for roles
	private static InteractionApplicationCommandCallbackSpec roleSetting(String configOptionName, String roleName,
			String[] filter, Guild guild) {
		// Find existing role
		String lastSelectedRole = null;
		JsonObject obj = ServerConfigUtils.getServerConfig(guild.getId().asString());
		if (obj.has(configOptionName))
			lastSelectedRole = obj.get(configOptionName).getAsString();

		// Build role list
		int i = 0;
		ArrayList<Option> roleSelectionList = new ArrayList<Option>();
		for (Role role : guild.getRoles(EntityRetrievalStrategy.STORE_FALLBACK_REST).toIterable()) {
			if (role.isEveryone())
				continue;

			String gRoleId = role.getId().asString();
			String gRoleName = role.getName();

			// Show roles matching the filter first
			if (!filterApplies(gRoleName, filter))
				continue;
			Option opt = Option.of(gRoleName, gRoleId);
			if (lastSelectedRole != null && lastSelectedRole.equals(gRoleId))
				opt = opt.withDefault(true);
			roleSelectionList.add(opt);

			i++;
			// Prevent size from going past the discord limit
			if (i == 25)
				break;
		}
		for (Role role : guild.getRoles(EntityRetrievalStrategy.STORE_FALLBACK_REST).toIterable()) {
			if (role.isEveryone())
				continue;

			String gRoleId = role.getId().asString();
			String gRoleName = role.getName();

			// Dont show roles that were already shown
			if (filterApplies(gRoleName, filter))
				continue;
			Option opt = Option.of(gRoleName, gRoleId);
			if (lastSelectedRole != null && lastSelectedRole.equals(gRoleId))
				opt = opt.withDefault(true);
			roleSelectionList.add(opt);

			i++;
			// Prevent size from going past the discord limit
			if (i == 25)
				break;
		}

		// Build message
		InteractionApplicationCommandCallbackSpec.Builder msg = InteractionApplicationCommandCallbackSpec.builder();
		msg.content("Role configuration: **" + roleName + "**");
		msg.addComponent(ActionRow.of(SelectMenu.of("serveroptionselection/" + configOptionName, roleSelectionList)));

		// Return message
		return msg.ephemeral(true).build();
	}

	// Method to check filters
	private static boolean filterApplies(String data, String[] filter) {
		for (String fData : filter) {
			if (data.toLowerCase().contains(fData.toLowerCase()))
				return true;
		}
		return false;
	}
}
