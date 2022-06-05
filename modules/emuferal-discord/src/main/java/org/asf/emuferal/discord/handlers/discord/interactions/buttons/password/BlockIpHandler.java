package org.asf.emuferal.discord.handlers.discord.interactions.buttons.password;

import org.asf.emuferal.accounts.AccountManager;
import org.asf.emuferal.accounts.EmuFeralAccount;
import org.asf.emuferal.discord.UserIpBlockUtils;

import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.object.entity.Message;
import discord4j.discordjson.json.MessageReferenceData;
import reactor.core.publisher.Mono;

public class BlockIpHandler {

	/**
	 * Block IP button event
	 * 
	 * @param event   Button event
	 * @param gateway Discord client
	 * @return Result Mono object
	 */
	public static Mono<?> handle(String id, ButtonInteractionEvent event, GatewayDiscordClient gateway) {
		// Parse request
		String uid = id.split("/")[1];
		String gid = id.split("/")[2];
		String ip = id.split("/")[3];

		// Verify interaction owner
		String str = event.getInteraction().getUser().getId().asString();
		if (uid.equals(str)) {
			// Locate EmuFeralAccount
			EmuFeralAccount acc = AccountManager.getInstance().getAccount(gid);
			if (acc != null) {
				// Block the IP
				UserIpBlockUtils.blockIp(acc, ip);
				if (!event.getMessage().get().getData().messageReference().isAbsent()) {
					try {
						MessageReferenceData ref = event.getMessage().get().getData().messageReference().get();
						Message oMsg = gateway.getMessageById(Snowflake.of(ref.channelId().get().asString()),
								Snowflake.of(ref.messageId().get())).block();
						oMsg.edit().withComponents().subscribe();
					} catch (Exception e) {
					}
				}
				event.getMessage().get().edit().withComponents().subscribe();
				return event.reply(
						"Successfully blocked the IP from accessing your account, you can unblock it via ingame commands.");
			} else {
				// Reply error
				event.getMessage().get().edit().withComponents().subscribe();
				return event.reply("The account you are attempting to block an IP for does not exist anymore.");
			}
		}

		// Default response
		return Mono.empty();
	}
}
