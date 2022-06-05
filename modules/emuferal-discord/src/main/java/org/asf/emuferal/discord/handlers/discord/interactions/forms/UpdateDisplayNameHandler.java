package org.asf.emuferal.discord.handlers.discord.interactions.forms;

import org.asf.emuferal.accounts.AccountManager;
import org.asf.emuferal.accounts.EmuFeralAccount;
import org.asf.emuferal.discord.LinkUtils;

import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ModalSubmitInteractionEvent;
import reactor.core.publisher.Mono;

public class UpdateDisplayNameHandler {

	/**
	 * Handles the 'update display name' form submission event
	 * 
	 * @param event   Modal submission event
	 * @param gateway Discord client
	 * @return Result Mono object
	 */
	public static Mono<?> handle(String id, ModalSubmitInteractionEvent event, GatewayDiscordClient gateway) {
		// Load fields
		String newName = event.getInteraction().getData().data().get().components().get().get(0).components().get()
				.get(0).value().get();

		// Load account manager
		AccountManager manager = AccountManager.getInstance();

		// Find owner UserID
		String userID = event.getInteraction().getUser().getId().asString();

		// Find account
		EmuFeralAccount account = LinkUtils.getAccountByDiscordID(userID);
		if (account == null)
			return Mono.empty();

		// Verify name validity
		if (!newName.matches("^[0-9A-Za-z\\-_. ]+") || newName.length() > 16 || newName.length() < 2) {
			// Reply with error
			return event.reply("Invalid display name.");
		}

		// Check if the name is in use
		if (manager.isDisplayNameInUse(newName)) {
			// Reply with error
			return event.reply("Selected display name is already in use.");
		}

		// Update display name
		manager.releaseDisplayName(account.getDisplayName());
		account.updateDisplayName(newName);
		manager.lockDisplayName(newName, account.getAccountID());
		account.kick();
		return event.reply("Display name updates successfully.");
	}
}
