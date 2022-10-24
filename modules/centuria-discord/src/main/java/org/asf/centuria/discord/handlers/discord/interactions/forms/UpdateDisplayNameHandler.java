package org.asf.centuria.discord.handlers.discord.interactions.forms;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import org.asf.centuria.accounts.AccountManager;
import org.asf.centuria.accounts.CenturiaAccount;
import org.asf.centuria.discord.LinkUtils;
import org.asf.centuria.packets.xt.gameserver.inventory.InventoryItemDownloadPacket;

import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ModalSubmitInteractionEvent;
import reactor.core.publisher.Mono;

public class UpdateDisplayNameHandler {

	private static String[] nameBlacklist = new String[] { "kit", "kitsendragn", "kitsendragon", "fera", "fero",
			"wwadmin", "ayli", "komodorihero", "wwsam", "blinky", "fer.ocity" };

	private static ArrayList<String> muteWords = new ArrayList<String>();
	private static ArrayList<String> filterWords = new ArrayList<String>();

	static {
		// Load filter
		try {
			InputStream strm = InventoryItemDownloadPacket.class.getClassLoader()
					.getResourceAsStream("textfilter/filter.txt");
			String lines = new String(strm.readAllBytes(), "UTF-8").replace("\r", "");
			for (String line : lines.split("\n")) {
				if (line.isEmpty() || line.startsWith("#"))
					continue;

				String data = line.trim();
				while (data.contains("  "))
					data = data.replace("  ", "");

				for (String word : data.split(" "))
					filterWords.add(word.toLowerCase());
			}
			strm.close();
		} catch (IOException e) {
		}

		// Load ban words
		try {
			InputStream strm = InventoryItemDownloadPacket.class.getClassLoader()
					.getResourceAsStream("textfilter/instamute.txt");
			String lines = new String(strm.readAllBytes(), "UTF-8").replace("\r", "");
			for (String line : lines.split("\n")) {
				if (line.isEmpty() || line.startsWith("#"))
					continue;

				String data = line.trim();
				while (data.contains("  "))
					data = data.replace("  ", "");

				for (String word : data.split(" "))
					muteWords.add(word.toLowerCase());
			}
			strm.close();
		} catch (IOException e) {
		}
	}

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
		CenturiaAccount account = LinkUtils.getAccountByDiscordID(userID);
		if (account == null)
			return Mono.empty();

		// Verify name validity
		if (!newName.matches("^[0-9A-Za-z\\-_. ]+") || newName.length() > 16 || newName.length() < 2) {
			// Reply with error
			return event.reply("Invalid display name.");
		}

		// Verify name blacklist
		for (String name : nameBlacklist) {
			if (newName.equalsIgnoreCase(name)) {
				// Reply with error
				return event.reply("Invalid display name: this name may not be used.").withEphemeral(true);
			}
		}

		// Verify name with filters
		for (String word : newName.split(" ")) {
			if (muteWords.contains(word.replaceAll("[^A-Za-z0-9]", "").toLowerCase())) {
				// Reply with error
				return event.reply("Invalid display name: this name may not be used.").withEphemeral(true);
			}

			if (filterWords.contains(word.replaceAll("[^A-Za-z0-9]", "").toLowerCase())) {
				// Reply with error
				return event.reply("Invalid display name: this name may not be used.").withEphemeral(true);
			}
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
		account.kickDirect("SYSTEM", "Display name changed");
		return event.reply("Display name updates successfully.");
	}
}
