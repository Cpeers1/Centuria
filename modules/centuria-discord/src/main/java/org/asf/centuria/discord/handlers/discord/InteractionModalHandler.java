package org.asf.centuria.discord.handlers.discord;

import org.asf.centuria.accounts.CenturiaAccount;
import org.asf.centuria.discord.DiscordBotModule;
import org.asf.centuria.discord.LinkUtils;
import org.asf.centuria.discord.handlers.discord.interactions.forms.AppealHandler;
import org.asf.centuria.discord.handlers.discord.interactions.forms.FeedbackReplyHandler;
import org.asf.centuria.discord.handlers.discord.interactions.forms.RegistrationHandler;
import org.asf.centuria.discord.handlers.discord.interactions.forms.ReportReplyHandler;
import org.asf.centuria.discord.handlers.discord.interactions.forms.UpdateDisplayNameHandler;
import org.asf.centuria.networking.gameserver.GameServer;

import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ModalSubmitInteractionEvent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.rest.util.Color;
import discord4j.rest.util.Permission;
import reactor.core.publisher.Mono;

public class InteractionModalHandler {

	/**
	 * Handles modal interaction
	 * 
	 * @param event   Modal submission event
	 * @param gateway Discord client
	 * @return Result Mono object
	 */
	public static Mono<?> handle(ModalSubmitInteractionEvent event, GatewayDiscordClient gateway) {
		String id = event.getCustomId();

		if (id.equals("dummy")) {
			return event.deferEdit();
		} else if (id.equals("updatedisplayname")) {
			return UpdateDisplayNameHandler.handle(id, event, gateway);
		} else if (id.equals("accountregistration")) {
			return RegistrationHandler.handle(id, event, gateway);
		} else if (id.equals("appealform")) {
			return AppealHandler.handle(id, event, gateway);
		} else if (id.startsWith("feedbackreply/")) {
			return FeedbackReplyHandler.handle(id, event, gateway);
		} else if (id.startsWith("reportreply/")) {
			return ReportReplyHandler.handle(id, event, gateway);
		} else if (id.equalsIgnoreCase("createaccountpanel")) {
			event.deferReply().block();
			var guild = event.getInteraction().getGuild().block();

			// Required permissions: admin (ingame), admin (discord)
			CenturiaAccount acc = LinkUtils.getAccountByDiscordID(event.getInteraction().getUser().getId().asString());
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

			// Create panel
			String argsStr = event.getInteraction().getData().data().get().components().get().get(0).components().get()
					.get(0).value().get()
					+ "\n\nPlease enable DMs for this server, many interactions with the bot are done via DM.";
			if (argsStr.length() > 4000) {
				// Warn about the length
				event.getInteraction().getChannel().block()
						.createMessage("Embed description is too long, please make a shorter message.").subscribe();
				return Mono.empty();
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
			event.getInteraction().getChannel().block().createMessage(msg.build()).subscribe();
			event.getReply().block().delete().block();
		}

		// Default handler
		return Mono.empty();
	}

}
