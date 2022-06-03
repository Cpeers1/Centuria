package org.asf.emuferal.accounts;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import org.asf.emuferal.EmuFeral;
import org.asf.emuferal.ipbans.IpBanManager;
import org.asf.emuferal.modules.eventbus.EventBus;
import org.asf.emuferal.modules.events.accounts.AccountBanEvent;
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
		// Locate online player
		Player plr = getOnlinePlayerInstance();

		if (plr != null) {
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
			ban();

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
		// Ban the player
		JsonObject banInfo = new JsonObject();
		banInfo.addProperty("type", "ban");
		banInfo.addProperty("unbanTimestamp", -1);
		getPlayerInventory().setItem("penalty", banInfo);

		// Find online player
		Player plr = getOnlinePlayerInstance();
		if (plr != null && plr.account != this) {
			// Sync to online player
			plr.account.ban();
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
		EventBus.getInstance().dispatchEvent(new AccountBanEvent(this, -1));
	}

	/**
	 * Temporarily bans the player
	 * 
	 * @param days How long to ban the player in days
	 */
	public void tempban(int days) {
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
		EventBus.getInstance().dispatchEvent(new AccountBanEvent(this, days));
	}

	/**
	 * Mutes the player
	 */
	public void mute(int days, int hours, int minutes) {
		// Apply mute
		JsonObject muteInfo = new JsonObject();
		muteInfo.addProperty("type", "mute");
		muteInfo.addProperty("unmuteTimestamp", System.currentTimeMillis() + (minutes * 60 * 1000)
				+ (hours * 60 * 60 * 1000) + (days * 24 * 60 * 60 * 1000));
		getPlayerInventory().setItem("penalty", muteInfo);

		// Sync online player
		Player plr = getOnlinePlayerInstance();
		if (plr != null && plr.account != this) {
			plr.account.mute(days, hours, minutes);
			return;
		}

		// Dispatch event
		EventBus.getInstance().dispatchEvent(new AccountMuteEvent(this, muteInfo.get("unmuteTimestamp").getAsLong()));
	}

	/**
	 * Pardons the player
	 */
	public void pardon() {
		// Remove penalties
		if (getPlayerInventory().containsItem("penalty"))
			getPlayerInventory().deleteItem("penalty");

		// Sync online player
		Player plr = getOnlinePlayerInstance();
		if (plr != null && plr.account != this) {
			plr.account.pardon();
			return;
		}

		// Dispatch event
		EventBus.getInstance().dispatchEvent(new AccountPardonEvent(this));
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
