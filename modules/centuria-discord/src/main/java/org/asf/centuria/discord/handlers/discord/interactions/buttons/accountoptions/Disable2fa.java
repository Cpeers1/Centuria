package org.asf.centuria.discord.handlers.discord.interactions.buttons.accountoptions;

import org.asf.centuria.accounts.AccountManager;
import org.asf.centuria.accounts.CenturiaAccount;

import com.google.gson.JsonObject;

import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.entity.Message;
import discord4j.discordjson.json.MessageReferenceData;
import reactor.core.publisher.Mono;

public class Disable2fa {

	/**
	 * Disable 2fa event
	 * 
	 * @param event   Button event
	 * @param gateway Discord client
	 * @return Result Mono object
	 */
	public static Mono<?> handle(String id, ButtonInteractionEvent event, GatewayDiscordClient gateway) {
		// Parse request
		String uid = id.split("/")[1];
		String gid = id.split("/")[2];

		// Verify interaction owner
		String str = event.getInteraction().getUser().getId().asString();
		if (uid.equals(str)) {
			// Locate CenturiaAccount
			CenturiaAccount acc = AccountManager.getInstance().getAccount(gid);
			if (acc != null) {
				// Save option
				if (!acc.getPlayerInventory().containsItem("accountoptions"))
					acc.getPlayerInventory().setItem("accountoptions", new JsonObject());
				JsonObject config = acc.getPlayerInventory().getItem("accountoptions").getAsJsonObject();
				if (config.has("enable2fa"))
					config.remove("enable2fa");
				config.addProperty("enable2fa", false);
				acc.getPlayerInventory().setItem("accountoptions", config);

				// Update message
				MessageReferenceData ref = event.getMessage().get().getData().messageReference().get();
				Message oMsg = gateway.getMessageById(Snowflake.of(ref.channelId().get().asString()),
						Snowflake.of(ref.messageId().get())).block();
				oMsg.edit().withComponents(ActionRow.of(
						Button.success("confirmenable2fa/" + uid + "/" + acc.getAccountID(), "Enable"),
						Button.danger("confirmdisable2fa/" + uid + "/" + acc.getAccountID(), "Disable").disabled()))
						.subscribe();

				// Send reply
				event.getMessage().get().edit().withComponents().subscribe();
				return event.reply("Successfully disabled 2-factor authentication.");
			} else {
				// Reply error
				event.getMessage().get().edit().withComponents().subscribe();
				return event.reply("The account you are attempting to configure does not exist anymore.");
			}
		}

		// Default response
		return Mono.empty();
	}
}
