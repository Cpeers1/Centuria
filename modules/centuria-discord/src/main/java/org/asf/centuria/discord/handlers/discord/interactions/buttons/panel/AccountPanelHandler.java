package org.asf.centuria.discord.handlers.discord.interactions.buttons.panel;

import java.io.IOException;
import java.io.InputStream;

import org.asf.centuria.accounts.CenturiaAccount;
import org.asf.centuria.accounts.PlayerInventory;
import org.asf.centuria.discord.DiscordBotModule;
import org.asf.centuria.discord.LinkUtils;
import org.asf.centuria.interactions.modules.QuestManager;
import org.asf.centuria.packets.xt.gameserver.inventory.InventoryItemDownloadPacket;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.SelectMenu;
import discord4j.core.object.component.SelectMenu.Option;
import discord4j.core.spec.InteractionApplicationCommandCallbackSpec;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.rest.util.Color;
import discord4j.core.spec.EmbedCreateSpec;
import reactor.core.publisher.Mono;

public class AccountPanelHandler {

	/**
	 * Handles the 'account options' button event
	 * 
	 * @param event   Button event
	 * @param gateway Discord client
	 * @return Result Mono object
	 */
	public static Mono<?> handle(String id, ButtonInteractionEvent event, GatewayDiscordClient gateway) {
		// Find owner UserID
		String userID = event.getInteraction().getUser().getId().asString();

		// Check link
		if (!LinkUtils.isPairedWithCenturia(userID)) {
			// Return error
			InteractionApplicationCommandCallbackSpec.Builder msg = InteractionApplicationCommandCallbackSpec.builder();
			msg.content("Could not locate a Centuria account linked with your Discord account.");
			msg.ephemeral(true);
			return event.reply(msg.build());
		} else {
			try {
				// Find account
				CenturiaAccount account = LinkUtils.getAccountByDiscordID(userID);
				PlayerInventory inv = account.getPlayerInventory();

				// Create account panel embed
				EmbedCreateSpec.Builder embed = EmbedCreateSpec.builder();
				embed.color(Color.VIVID_VIOLET);
				embed.title("Centuria Account Panel");
				embed.description("This is your account panel for **" + account.getDisplayName()
						+ "**, here you can view your account information and change your account settings.\n\n_ _");
				embed.footer(DiscordBotModule.getServerName(), gateway.getSelf().block().getAvatarUrl());

				// Display name
				embed.addField("Display name", account.getDisplayName(), true);

				// Level
				if (account.getLevel().isLevelAvailable())
					embed.addField("Level", Integer.toString(account.getLevel().getLevel()), true);
				else
					embed.addField("Level", "Unknown", true);

				// Online status
				embed.addField("Is online?", account.getOnlinePlayerInstance() != null ? "Yes" : "No", true);

				// Avatar info
				String species = "Kitsune";
				// Find current species
				String lookID = account.getActiveLook();
				if (inv.containsItem("avatars")) {
					// Find avatar
					JsonArray looks = inv.getItem("avatars").getAsJsonArray();
					for (JsonElement lookEle : looks) {
						JsonObject look = lookEle.getAsJsonObject();
						if (look.get("id").getAsString().equals(lookID)) {
							// Found the avatar, lets find the species
							String defId = look.get("defId").getAsString();

							// Load avatar helper
							try {
								InputStream strm = InventoryItemDownloadPacket.class.getClassLoader()
										.getResourceAsStream("defaultitems/avatarhelper.json");
								JsonObject helper = JsonParser.parseString(new String(strm.readAllBytes(), "UTF-8"))
										.getAsJsonObject().get("Avatars").getAsJsonObject();
								strm.close();
								for (String aSpecies : helper.keySet()) {
									String aDefID = helper.get(aSpecies).getAsJsonObject().get("defId").getAsString();
									if (aDefID.equals(defId)) {
										// Found the species
										species = aSpecies;
										break;
									}
								}
							} catch (IOException e) {
							}
							break;
						}
					}
				}
				embed.addField("Avatar Species", species, true);

				// Quest
				var q = QuestManager.getActiveQuest(account);
				embed.addField("Current quest", q == null ? "None" : QuestManager.getQuest(q).name, true);

				// Last login
				embed.addField("Last login time",
						account.getLastLoginTime() == -1 ? "Unknown" : "<t:" + account.getLastLoginTime() + ">", true);

				// Build message
				MessageCreateSpec.Builder msg = MessageCreateSpec.builder();
				msg.addEmbed(embed.build());

				// Dropdown
				msg.addComponent(
						ActionRow.of(SelectMenu.of("accountoption", Option.of("Change display name", "displayname"),
								Option.of("Enable/disable 2-factor authentication", "2fa"),
								Option.of("Forgot password", "forgotpassword"),
								Option.of("Forgot login name", "forgotloginname"),
								Option.of("Download your account inventory (including avatars)", "downloaddata"))));

				// DM the message
				event.getInteraction().getUser().getPrivateChannel().block().createMessage(msg.build()).block();
			} catch (Exception e) {
				return event.reply("Please enable DMs for this server, the bot was unable to DM the account panel.")
						.withEphemeral(true);
			}

			// Send message
			return event.deferEdit();
		}
	}
}
