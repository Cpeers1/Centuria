package org.asf.emuferal.accounts;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import org.asf.emuferal.EmuFeral;
import org.asf.emuferal.ipbans.IpBanManager;
import org.asf.emuferal.modules.eventbus.EventBus;
import org.asf.emuferal.modules.events.accounts.AccountBanEvent;
import org.asf.emuferal.modules.events.accounts.AccountKickEvent;
import org.asf.emuferal.modules.events.accounts.AccountMuteEvent;
import org.asf.emuferal.modules.events.accounts.AccountPardonEvent;
import org.asf.emuferal.networking.chatserver.ChatClient;
import org.asf.emuferal.players.Player;

import com.google.gson.JsonObject;

public abstract class EmuFeralAccount {

	/**
	 * Retrieves the account login name
	 *
	 * @return Login name string
	 */
	public abstract String getLoginName();

	/**
	 * Retrieves the account display name
	 *
	 * @return Display name string
	 */
	public abstract String getDisplayName();

	/**
	 * Retrieves the account ID
	 *
	 * @return Account ID (usually a UUID)
	 */
	public abstract String getAccountID();

	/**
	 * Retrieves the account numeric ID
	 *
	 * @return Account numeric ID
	 */
	public abstract int getAccountNumericID();

	/**
	 * Checks if the player completed the tutorial
	 *
	 * @return True if the player hasn't finished the tutorial, false otherwise.
	 */
	public abstract boolean isPlayerNew();

	/**
	 * Used to mark the tutorial as finished
	 */
	public abstract void finishedTutorial();

	/**
	 * Used to update the player display name
	 *
	 * @param name New display name
	 * @return True if valid, false otherwise
	 */
	public abstract boolean updateDisplayName(String name);

	/**
	 * Retrieves the player inventory
	 *
	 * @return PlayerInventory instance
	 */
	public abstract PlayerInventory getPlayerInventory();

	/**
	 * Retrieves or creates the account privacy settings
	 *
	 * @return JsonObject instance
	 */
	public abstract JsonObject getPrivacySettings();

	/**
	 * Saves privacy settings
	 *
	 * @param settings JsonObject instance containing privacy settings
	 */
	public abstract void savePrivacySettings(JsonObject settings);

	/**
	 * Retrieves the active character look
	 *
	 * @return Character look ID
	 */
	public abstract String getActiveLook();

	/**
	 * Retrieves the active sanctuary look
	 *
	 * @return Sanctuary look ID
	 */
	public abstract String getActiveSanctuaryLook();

	/**
	 * Assigns the active character look
	 *
	 * @param lookID Character look ID
	 */
	public abstract void setActiveLook(String lookID);

	/**
	 * Assigns the active sanctuary look
	 *
	 * @param lookID Sanctuary look ID
	 */
	public abstract void setActiveSanctuaryLook(String lookID);

	/**
	 * Checks if the account needs to be renamed
	 *
	 * @return True if the account needs to be renamed, false otherwise
	 */
	public abstract boolean isRenameRequired();

	/**
	 * Forces the account to require a name change
	 */
	public abstract void forceNameChange();

	/**
	 * Retrieves the last time this account was logged into
	 *
	 * @return Login Unix timestamp (seconds) or -1 if not found
	 */
	public abstract long getLastLoginTime();

	/**
	 * Updates the last login timestamp
	 */
	public abstract void login();

	/**
	 * Retrieves the player level object
	 *
	 * @return LevelInfo instance
	 */
	public abstract LevelInfo getLevel();

	/**
	 * Retrieves the player object
	 *
	 * @return Player instance or null if offline
	 */
	public abstract Player getOnlinePlayerInstance();

	/**
	 * Deletes the account from disk and kicks all connected instances
	 */
	public abstract void deleteAccount();

	/**
	 * Kicks the player if online
	 *
	 * @return true if successful, false otherwise
	 */
	public boolean kick() {
		return kick(null);
	}

	/**
	 * Kicks the player if online
	 *
	 * @param reason Kick reason
	 * @return true if successful, false otherwise
	 */
	public boolean kick(String reason) {
		return kick("SYSTEM", reason);
	}

	/**
	 * Kicks the player if online
	 *
	 * @param issuer Kick issuer
	 * @param reason Kick reason
	 * @return true if successful, false otherwise
	 */
	public boolean kick(String issuer, String reason) {
		// Locate online player
		Player plr = getOnlinePlayerInstance();

		if (plr != null) {
			// Dispatch event
			EventBus.getInstance().dispatchEvent(new AccountKickEvent(this, issuer, reason));

			// Log
			String issuerNm = issuer;
			if (!issuerNm.equals("SYSTEM")) {
				EmuFeralAccount acc = AccountManager.getInstance().getAccount(issuer);
				if (acc != null)
					issuerNm = acc.getDisplayName();
			}
			System.out.println("Kicked " + getDisplayName() + ": " + (reason == null ? "Unspecified reason" : reason)
					+ " (issued by " + issuerNm + ")");

			// Kick the player
			plr.client.sendPacket("%xt%ua%-1%4086%");
			try {
				Thread.sleep(3000);
			} catch (InterruptedException e) {
			}
			plr.client.disconnect();

			// Return success
			return true;
		}

		// Return failure
		return false;
	}

	/**
	 * IP-bans online players
	 *
	 * @return true if successful, false otherwise
	 */
	public boolean ipban() {
		return ipban(null);
	}

	/**
	 * IP-bans online players
	 *
	 * @param reason Kick reason
	 * @return true if successful, false otherwise
	 */
	public boolean ipban(String reason) {
		return ipban("SYSTEM", reason);
	}

	/**
	 * IP-bans online players
	 *
	 * @param issuer Kick issuer
	 * @param reason Kick reason
	 * @return true if successful, false otherwise
	 */
	public boolean ipban(String issuer, String reason) {
		Player plr = getOnlinePlayerInstance();
		if (plr != null) {
			// Apply IP ban
			InetSocketAddress ip = (InetSocketAddress) plr.client.getSocket().getRemoteSocketAddress();
			InetAddress addr = ip.getAddress();
			String ipaddr = addr.getHostAddress();
			IpBanManager manager = IpBanManager.getInstance();
			if (!manager.isIPBanned(ipaddr))
				manager.banIP(ipaddr);

			// Apply regular ban
			ban(issuer, reason);

			// Return success
			return true;
		}

		// Return failure
		return false;
	}

	/**
	 * Bans the player
	 */
	public void ban() {
		ban(null);
	}

	/**
	 * Bans the player
	 *
	 * @param reason Ban reason
	 */
	public void ban(String reason) {
		ban("SYSTEM", reason);
	}

	/**
	 * Bans the player
	 *
	 * @param issuer Ban issuer
	 * @param reason Ban reason
	 */
	public void ban(String issuer, String reason) {
		// Ban the player
		JsonObject banInfo = new JsonObject();
		banInfo.addProperty("type", "ban");
		banInfo.addProperty("unbanTimestamp", -1);
		getPlayerInventory().setItem("penalty", banInfo);

		// Find online player
		Player plr = getOnlinePlayerInstance();
		if (plr != null && plr.account != this) {
			// Sync to online player
			plr.account.ban(issuer, reason);
			return;
		}

		if (plr != null) {
			// Kick the player
			plr.client.sendPacket("%xt%ua%-1%3561%");
			try {
				Thread.sleep(3000);
			} catch (InterruptedException e) {
			}
			plr.client.disconnect();
		}

		// Disconnect it from the chat server
		for (ChatClient cl : EmuFeral.chatServer.getClients()) {
			if (cl.getPlayer().getAccountID().equals(getAccountID())) {
				cl.disconnect();
			}
		}

		// Dispatch event
		EventBus.getInstance().dispatchEvent(new AccountBanEvent(this, -1, issuer, reason));

		// Log
		String issuerNm = issuer;
		if (!issuerNm.equals("SYSTEM")) {
			EmuFeralAccount acc = AccountManager.getInstance().getAccount(issuer);
			if (acc != null)
				issuerNm = acc.getDisplayName();
		}
		System.out.println("Premanently banned " + getDisplayName() + ": "
				+ (reason == null ? "Unspecified reason" : reason) + " (issued by " + issuerNm + ")");
	}

	/**
	 * Temporarily bans the player
	 *
	 * @param days How long to ban the player in days
	 */
	public void tempban(int days) {
		tempban(days, null);
	}

	/**
	 * Temporarily bans the player
	 *
	 * @param days   How long to ban the player in days
	 * @param reason Ban reason
	 */
	public void tempban(int days, String reason) {
		tempban(days, "SYSTEM", reason);
	}

	/**
	 * Temporarily bans the player
	 *
	 * @param days   How long to ban the player in days
	 * @param issuer Ban issuer
	 * @param reason Ban reason
	 */
	public void tempban(int days, String issuer, String reason) {
		// Ban the player
		JsonObject banInfo = new JsonObject();
		banInfo.addProperty("type", "ban");
		banInfo.addProperty("unbanTimestamp", System.currentTimeMillis() + (days * 24 * 60 * 60 * 1000));
		getPlayerInventory().setItem("penalty", banInfo);

		// Find online player
		Player plr = getOnlinePlayerInstance();
		if (plr != null) {
			// Kick the player
			plr.client.sendPacket("%xt%ua%-1%3561%");
			try {
				Thread.sleep(3000);
			} catch (InterruptedException e) {
			}
			plr.client.disconnect();
		}

		// Dispatch event
		EventBus.getInstance().dispatchEvent(new AccountBanEvent(this, days, issuer, reason));

		// Log
		String issuerNm = issuer;
		if (!issuerNm.equals("SYSTEM")) {
			EmuFeralAccount acc = AccountManager.getInstance().getAccount(issuer);
			if (acc != null)
				issuerNm = acc.getDisplayName();
		}
		System.out.println(
				"Temporarily banned " + getDisplayName() + ": " + (reason == null ? "Unspecified reason" : reason)
						+ " (issued by " + issuerNm + ", unban in " + days + " days)");
	}

	/**
	 * Mutes the player
	 *
	 * @param days    Amount of days to mute for
	 * @param hours   Amount of hours to mute for
	 * @param minutes Amount of minutes to mute for
	 */
	public void mute(int days, int hours, int minutes) {
		mute(days, hours, minutes, null);
	}

	/**
	 * Mutes the player
	 *
	 * @param days    Amount of days to mute for
	 * @param hours   Amount of hours to mute for
	 * @param minutes Amount of minutes to mute for
	 * @param reason  Mute reason
	 */
	public void mute(int days, int hours, int minutes, String reason) {
		mute(days, hours, minutes, "SYSTEM", reason);
	}

	/**
	 * Mutes the player
	 *
	 * @param days    Amount of days to mute for
	 * @param hours   Amount of hours to mute for
	 * @param minutes Amount of minutes to mute for
	 * @param issuer  Mute issuer
	 * @param reason  Mute reason
	 */
	public void mute(int days, int hours, int minutes, String issuer, String reason) {
		// Apply mute
		JsonObject muteInfo = new JsonObject();
		muteInfo.addProperty("type", "mute");
		muteInfo.addProperty("unmuteTimestamp", System.currentTimeMillis() + (minutes * 60 * 1000)
				+ (hours * 60 * 60 * 1000) + (days * 24 * 60 * 60 * 1000));
		getPlayerInventory().setItem("penalty", muteInfo);

		// Sync online player
		Player plr = getOnlinePlayerInstance();
		if (plr != null && plr.account != this) {
			plr.account.mute(days, hours, minutes, issuer, reason);
			return;
		}

		// Dispatch event
		EventBus.getInstance()
				.dispatchEvent(new AccountMuteEvent(this, muteInfo.get("unmuteTimestamp").getAsLong(), issuer, reason));

		// Log
		String issuerNm = issuer;
		if (!issuerNm.equals("SYSTEM")) {
			EmuFeralAccount acc = AccountManager.getInstance().getAccount(issuer);
			if (acc != null)
				issuerNm = acc.getDisplayName();
		}
		System.out.println(
				"Muted " + getDisplayName() + ": " + (reason == null ? "Unspecified reason" : reason) + " (issued by "
						+ issuerNm + ", unmute in " + days + " days, " + hours + " hours and " + minutes + " minutes)");
	}

	/**
	 * Pardons the player
	 */
	public void pardon() {
		pardon(null);
	}

	/**
	 * Pardons the player
	 *
	 * @param reason Pardon reason
	 */
	public void pardon(String reason) {
		pardon("SYSTEM", reason);
	}

	/**
	 * Pardons the player
	 *
	 * @param issuer Pardon issuer
	 * @param reason Pardon reason
	 */
	public void pardon(String issuer, String reason) {
		// Remove penalties
		if (getPlayerInventory().containsItem("penalty"))
			getPlayerInventory().deleteItem("penalty");

		// Sync online player
		Player plr = getOnlinePlayerInstance();
		if (plr != null && plr.account != this) {
			plr.account.pardon(issuer, reason);
			return;
		}

		// Dispatch event
		EventBus.getInstance().dispatchEvent(new AccountPardonEvent(this, issuer, reason));

		// Log
		String issuerNm = issuer;
		if (!issuerNm.equals("SYSTEM")) {
			EmuFeralAccount acc = AccountManager.getInstance().getAccount(issuer);
			if (acc != null)
				issuerNm = acc.getDisplayName();
		}
		System.out.println("Pardoned " + getDisplayName() + ": " + (reason == null ? "Unspecified reason" : reason)
				+ " (issued by " + issuerNm + ")");
	}

	/**
	 * Checks if the current user is banned
	 *
	 * @return True if banned, false otherwise
	 */
	public boolean isBanned() {
		if (getPlayerInventory().containsItem("penalty")
				&& getPlayerInventory().getItem("penalty").getAsJsonObject().get("type").getAsString().equals("ban")) {
			JsonObject banInfo = getPlayerInventory().getItem("penalty").getAsJsonObject();
			if (banInfo.get("unbanTimestamp").getAsLong() == -1
					|| banInfo.get("unbanTimestamp").getAsLong() > System.currentTimeMillis()) {
				return true;
			} else
				getPlayerInventory().deleteItem("penalty");
		}

		return false;
	}

	/**
	 * Checks if the current user is muted
	 *
	 * @return True if muted, false otherwise
	 */
	public boolean isMuted() {
		if (getPlayerInventory().containsItem("penalty")
				&& getPlayerInventory().getItem("penalty").getAsJsonObject().get("type").getAsString().equals("mute")) {
			JsonObject muteInfo = getPlayerInventory().getItem("penalty").getAsJsonObject();
			if (muteInfo.get("unmuteTimestamp").getAsLong() == -1
					|| muteInfo.get("unmuteTimestamp").getAsLong() > System.currentTimeMillis()) {
				return true;
			} else
				getPlayerInventory().deleteItem("penalty");
		}

		return false;
	}

}
