package org.asf.emuferal.discord;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.SecureRandom;
import java.util.ConcurrentModificationException;
import java.util.HashMap;

import org.asf.emuferal.accounts.AccountManager;
import org.asf.emuferal.accounts.EmuFeralAccount;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import discord4j.common.util.Snowflake;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.spec.MessageCreateSpec;

public class LinkUtils {

	// Account link
	private static JsonObject accountLinks = new JsonObject();

	// Pairing codes
	private static HashMap<String, PairCodeData> pairCodes = new HashMap<String, PairCodeData>();
	private static SecureRandom rnd = new SecureRandom();

	// Start code expiry thread
	static {
		Thread th = new Thread(() -> {
			while (true) {
				HashMap<String, PairCodeData> codes;
				while (true) {
					try {
						codes = new HashMap<String, PairCodeData>(pairCodes);
						break;
					} catch (ConcurrentModificationException e) {
					}
				}

				for (String c : codes.keySet()) {
					if (pairCodes.get(c).timeRemaining - 1 <= 0) {
						pairCodes.remove(c);
					} else {
						pairCodes.get(c).timeRemaining--;
					}
				}

				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					break;
				}
			}
		}, "Pair code cleanup");
		th.setDaemon(true);
		th.start();
	}

	// Saves the account registry
	private static void saveRegistry() {
		// Load account link registry file path as file object
		File accountLinkRegistry = new File("accountlink.json");

		try {
			// Write file
			Files.writeString(accountLinkRegistry.toPath(), accountLinks.toString());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	// Pairing code data
	private static class PairCodeData {
		public long timeRemaining;
		public String code;
	}

	/**
	 * Initializes the link database
	 */
	public static void init() {
		// Load account link registry file path as file object
		File accountLinkRegistry = new File("accountlink.json");

		// Create it if non-existent
		System.out.println("Loading link database...");
		if (!accountLinkRegistry.exists()) {
			saveRegistry();
		} else {
			// Load registry
			try {
				accountLinks = JsonParser.parseString(Files.readString(accountLinkRegistry.toPath())).getAsJsonObject();
			} catch (JsonSyntaxException | IOException e) {
				throw new RuntimeException(e);
			}
		}
		System.out.println("Successfully loaded link database.");
	}

	/**
	 * Generates a EmuFeral pairing code
	 * 
	 * @param userID Discord UserID
	 * @return Account pair code
	 */
	public static String generatePairCode(String userID) {
		// Generate code
		String code = "";
		while (true) {
			long cd = rnd.nextLong(99999999l);
			while (cd < 10000000l)
				cd = rnd.nextLong(99999999l);
			code = Long.toString(cd, 16);

			// Find code
			boolean found = false;
			HashMap<String, PairCodeData> codes;
			while (true) {
				try {
					codes = new HashMap<String, PairCodeData>(pairCodes);
					break;
				} catch (ConcurrentModificationException e) {
				}
			}
			for (String uid : codes.keySet()) {
				if (codes.get(uid).code.equals(code)) {
					// Found it
					found = true;
					break;
				}
			}

			if (!found)
				break; // We have a unused code
		}

		// Add code to memory
		PairCodeData data = new PairCodeData();
		data.timeRemaining = 15 * 60;
		data.code = code;
		pairCodes.put(userID, data);

		return code;
	}

	/**
	 * Finds a Discord account ID by pairing code (consumes the code if found)
	 * 
	 * @param code Account pairing code
	 * @return Discord UserID or null
	 */
	public static String useCode(String code) {
		HashMap<String, PairCodeData> codes;
		while (true) {
			try {
				codes = new HashMap<String, PairCodeData>(pairCodes);
				break;
			} catch (ConcurrentModificationException e) {
			}
		}

		// Find token by user ID
		for (String uid : codes.keySet()) {
			if (codes.get(uid).code.equals(code)) {
				// Remove code
				pairCodes.remove(uid);

				// Return ID
				return uid;
			}
		}

		// Return null as the code wasn't recognized
		return null;
	}

	/**
	 * Checks if the given EmuFeral account is paired with any discord account
	 * 
	 * @param account Account to check
	 * @return True if the account has been paired with a Discord account, false
	 *         otherwise.
	 */
	public static boolean isPairedWithDiscord(EmuFeralAccount account) {
		return account.getPlayerInventory().containsItem("pairedaccount");
	}

	/**
	 * Retrieves the discord account linked with a EmuFeral account
	 * 
	 * @param account Account to check
	 * @return Discord UserID string or null
	 */
	public static String getDiscordAccountFrom(EmuFeralAccount account) {
		if (account.getPlayerInventory().containsItem("pairedaccount"))
			return account.getPlayerInventory().getItem("pairedaccount").getAsJsonObject().get("userId").getAsString();
		return null;
	}

	/**
	 * Pairs a discord account with a EmuFeral account
	 * 
	 * @param account    Account to pair
	 * @param userID     Discord userID to pair
	 * @param address    IP address that requested to pair the account
	 * @param isTransfer True if this is an account transfer, false otherwise
	 * @param sendDM     True to send a DM to the owner, false otherwise
	 */
	public static void pairAccount(EmuFeralAccount account, String userID, String address, boolean sendDM,
			boolean isTransfer) {
		// Remove pair from if present inventory
		if (account.getPlayerInventory().containsItem("pairedaccount"))
			account.getPlayerInventory().deleteItem("pairedaccount");

		// Unpair from link registration
		if (accountLinks.has(userID))
			accountLinks.remove(userID);

		// Add new link
		JsonObject link = new JsonObject();
		link.addProperty("userId", userID);
		account.getPlayerInventory().setItem("pairedaccount", link);

		// Add to registry
		accountLinks.addProperty(userID, account.getAccountID());
		saveRegistry();

		if (sendDM) {
			// DM the user
			try {
				MessageCreateSpec.Builder msg = MessageCreateSpec.builder();

				// Message content
				if (!isTransfer)
					msg.content("Successfully linked your EmuFeral account with your discord account!\n" + "\n"
							+ "Here follow the details of the player that was paired:\n" + "**Account login name:** `"
							+ account.getLoginName() + "`\n" + "**Ingame player name:** `" + account.getDisplayName()
							+ "`\n" + (address != null ? "**Paired from IP:** `" + address + "`\n" : "")
							+ "**Last login time:** "
							+ (account.getLastLoginTime() == -1 ? "`Unknown`"
									: "<t:" + account.getLastLoginTime() + ">")
							+ "\n" + "**Paired at:** <t:" + System.currentTimeMillis() / 1000 + ">\n" + "\n" + "\n"
							+ "If you didnt intend to link this account, press the button below to remove the link.");
				else
					msg.content("Successfully transferred your EmuFeral account to your new discord account!\n" + "\n"
							+ "Here follow the details of the player that was transferred to your account:\n"
							+ "**Account login name:** `" + account.getLoginName() + "`\n" + "**Ingame player name:** `"
							+ account.getDisplayName() + "`\n"
							+ (address != null ? "**Paired from IP:** `" + address + "`\n" : "")
							+ "**Last login time:** "
							+ (account.getLastLoginTime() == -1 ? "`Unknown`"
									: "<t:" + account.getLastLoginTime() + ">")
							+ "\n" + "**Paired at:** <t:" + System.currentTimeMillis() / 1000 + ">");

				// Unlink and dismiss buttons
				if (!isTransfer)
					msg.addComponent(ActionRow.of(
							Button.danger("unlink/" + userID + "/" + account.getAccountID(), "Remove link"),
							Button.primary("dismiss", "Dismiss")));
				else
					msg.addComponent(ActionRow.of(Button.primary("dismiss", "Dismiss")));

				// Send response
				DiscordBotModule.getClient().getUserById(Snowflake.of(userID)).block().getPrivateChannel().block()
						.createMessage(msg.build()).subscribe();
			} catch (Exception e) {
			}
		}
	}

	/**
	 * Unpairs a discord account from a EmuFeral account
	 * 
	 * @param account Account to unpair
	 * @param address IP address that requested to unpair the account
	 * @param sendDM  True to send a DM to the current account owner, false
	 *                otherwise
	 */
	public static void unpairAccount(EmuFeralAccount account, String address, boolean sendDM) {
		// Load account details
		String userID = account.getPlayerInventory().getItem("pairedaccount").getAsJsonObject().get("userId")
				.getAsString();

		// Remove pair from inventory
		account.getPlayerInventory().deleteItem("pairedaccount");

		// Unpair from link registration
		if (accountLinks.has(userID))
			accountLinks.remove(userID);
		saveRegistry();

		if (sendDM) {
			// DM the user
			try {
				MessageCreateSpec.Builder msg = MessageCreateSpec.builder();

				// Message content
				msg.content("Your account has just been unpaired from EmuFeral.\n" + "\n"
						+ "Here follow the details of the player that was unpaired:\n" + "**Account login name:** `"
						+ account.getLoginName() + "`\n" + "**Ingame player name:** `" + account.getDisplayName()
						+ "`\n" + (address != null ? "**Unpaired from IP:** `" + address + "`\n" : "")
						+ "**Last login time:** "
						+ (account.getLastLoginTime() == -1 ? "`Unknown`" : "<t:" + account.getLastLoginTime() + ">")
						+ "\n" + "**Unpaired at:** <t:" + System.currentTimeMillis() / 1000 + ">\n" + "\n" + "\n"
						+ "If you didn't request to unpair your account, please re-connect it by pressing the button below and\ncontact the server owner for possible hack attempts.");

				// Re-connect and dismiss button
				msg.addComponent(ActionRow.of(
						Button.danger("relink/" + userID + "/" + account.getAccountID(), "Re-connect account"),
						Button.primary("dismiss", "Dismiss")));

				// Send response
				DiscordBotModule.getClient().getUserById(Snowflake.of(userID)).block().getPrivateChannel().block()
						.createMessage(msg.build()).subscribe();
			} catch (Exception e) {
			}
		}
	}

	/**
	 * Checks if the given Discord account is paired with any EmuFeral account
	 * 
	 * @param userID Discord UserID to check
	 * @return True if the account has been paired with a account account, false
	 *         otherwise.
	 */
	public static boolean isPairedWithEmuFeral(String userID) {
		return accountLinks.has(userID);
	}

	/**
	 * Retrieves a EmuFeral account by Discord UserID
	 * 
	 * @param userID Discord UserID to find the account for
	 * @return EmuFeralAccount instance or null
	 */
	public static EmuFeralAccount getAccountByDiscordID(String userID) {
		if (accountLinks.has(userID)) {
			// Find and return account
			String accountID = accountLinks.get(userID).getAsString();
			return AccountManager.getInstance().getAccount(accountID);
		}
		return null;
	}

}
