package org.asf.centuria.accounts.impl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.UUID;

import org.apache.logging.log4j.MarkerManager;
import org.asf.centuria.Centuria;
import org.asf.centuria.accounts.AccountManager;
import org.asf.centuria.accounts.CenturiaAccount;
import org.asf.centuria.accounts.LevelInfo;
import org.asf.centuria.accounts.PlayerInventory;
import org.asf.centuria.accounts.SaveManager;
import org.asf.centuria.accounts.SaveMode;
import org.asf.centuria.accounts.SaveSettings;
import org.asf.centuria.dms.DMManager;
import org.asf.centuria.entities.players.Player;
import org.asf.centuria.modules.eventbus.EventBus;
import org.asf.centuria.modules.events.accounts.AccountDeletionEvent;
import org.asf.centuria.packets.xt.gameserver.inventory.InventoryItemDownloadPacket;
import org.asf.centuria.social.SocialEntry;
import org.asf.centuria.social.SocialManager;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

public class FileBasedAccountObject extends CenturiaAccount {

	private int userID;
	private boolean isNew;
	private String userUUID;
	private String loginName;
	private String displayName;
	private SaveManager manager;
	private FileBasedPlayerInventory sharedInv;
	private FileBasedPlayerInventory mainInv;
	private JsonObject privacy;
	private LevelInfo level;
	private long lastLogin = -1;
	private File userFile;

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

	public FileBasedAccountObject(File uf) throws IOException {
		// Parse account file
		userUUID = Files.readAllLines(uf.toPath()).get(0);
		loginName = Files.readAllLines(uf.toPath()).get(1);
		isNew = Files.readAllLines(uf.toPath()).get(2).equals("true");
		displayName = Files.readAllLines(uf.toPath()).get(3);
		userID = Integer.parseInt(Files.readAllLines(uf.toPath()).get(4));

		// Find existing inventory
		Player old = getOnlinePlayerInstance();
		if (old == null || !(old.account.getSaveSharedInventory() instanceof FileBasedPlayerInventory)
				|| !(old.account.getSaveSpecificInventory() instanceof FileBasedPlayerInventory)) {
			// Load inventories
			sharedInv = new FileBasedPlayerInventory(userUUID, "");
			SaveMode mode = getSaveMode();
			if (mode == SaveMode.SINGLE)
				mainInv = sharedInv;
			else if (!sharedInv.getItem("savemanifest").getAsJsonObject().has("activeSave")) {
				// Clean up
				mode = SaveMode.SINGLE;
				sharedInv.deleteItem("savemanifest");
			}

		} else {
			// Use the existing inventory object
			sharedInv = (FileBasedPlayerInventory) old.account.getSaveSharedInventory();
			mainInv = (FileBasedPlayerInventory) old.account.getSaveSpecificInventory();
			if (old.account.getSaveMode() == SaveMode.MANAGED)
				manager = old.account.getSaveManager();
		}

		// Load manager
		if (manager == null && getSaveMode() == SaveMode.MANAGED) {
			manager = new FileBasedSaveManager(sharedInv, this);
			mainInv = new FileBasedPlayerInventory(userUUID, manager.getCurrentActiveSave());
		}

		// Load login timestamp
		lastLogin = uf.lastModified() / 1000;
		userFile = uf;
	}

	@Override
	public String getLoginName() {
		return loginName;
	}

	@Override
	public String getDisplayName() {
		return displayName;
	}

	@Override
	public String getAccountID() {
		return userUUID;
	}

	@Override
	public int getAccountNumericID() {
		return userID;
	}

	@Override
	public boolean isPlayerNew() {
		if (getSaveMode() == SaveMode.MANAGED)
			return !mainInv.containsItem("finishedtutorial");
		return isNew;
	}

	@Override
	public void finishedTutorial() {
		isNew = false;

		try {
			// Save
			Files.writeString(new File("accounts/" + userUUID).toPath(),
					userUUID + "\n" + loginName + "\n" + isNew + "\n" + displayName + "\n" + userID);
		} catch (IOException e) {
		}

		// Managed save mode
		if (getSaveMode() == SaveMode.MANAGED)
			mainInv.setItem("finishedtutorial", new JsonObject());
	}

	@Override
	public boolean updateLoginName(String username) {
		// Check name validity
		if (!username.matches("^[A-Za-z0-9@._#]+$") || username.contains(".cred")
				|| !username.matches(".*[A-Za-z0-9]+.*") || username.isBlank() || username.length() > 320
				|| AccountManager.getInstance().getUserByLoginName(username) != null)
			return false;

		// Prevent blacklisted names from being used
		for (String name : nameBlacklist) {
			if (username.equalsIgnoreCase(name))
				return false;
		}

		// Prevent banned and filtered words
		for (String word : username.split(" ")) {
			if (muteWords.contains(word.replaceAll("[^A-Za-z0-9]", "").toLowerCase())) {
				return false;
			}

			if (filterWords.contains(word.replaceAll("[^A-Za-z0-9]", "").toLowerCase())) {
				return false;
			}
		}

		// Set login name
		File f = new File("accounts/" + username);
		if (f.exists())
			return false;
		try {
			Centuria.logger
					.info("Set login name of " + userUUID + " to " + username + " (old name was " + loginName + ")");
			loginName = username;
			Files.writeString(new File("accounts/" + userUUID).toPath(),
					userUUID + "\n" + loginName + "\n" + isNew + "\n" + displayName + "\n" + userID);
			Files.writeString(new File("accounts/" + loginName).toPath(), userUUID + "\n" + loginName);
			return true;
		} catch (IOException e) {
		}

		return false;
	}

	@Override
	public boolean updateDisplayName(String name) {
		// Check validity
		if (!name.matches("^[0-9A-Za-z\\-_. ]+") || name.length() > 16 || name.length() < 2)
			return false;

		// Prevent blacklisted names from being used
		for (String nm : nameBlacklist) {
			if (name.equalsIgnoreCase(nm))
				return false;
		}

		// Prevent banned and filtered words
		for (String word : name.split(" ")) {
			if (muteWords.contains(word.replaceAll("[^A-Za-z0-9]", "").toLowerCase())) {
				return false;
			}

			if (filterWords.contains(word.replaceAll("[^A-Za-z0-9]", "").toLowerCase())) {
				return false;
			}
		}

		// Remove lockout
		if (isRenameRequired())
			new File("accounts/" + userUUID + ".requirechangename").delete();

		try {
			// Log
			Centuria.logger.info(MarkerManager.getMarker("Accounts"),
					"Display name changed of " + loginName + ": " + displayName + " -> " + name);

			// Store the name
			displayName = name;

			// Save to disk
			Files.writeString(new File("accounts/" + userUUID).toPath(),
					userUUID + "\n" + loginName + "\n" + isNew + "\n" + displayName + "\n" + userID);
			return true;
		} catch (IOException e) {
		}
		return false;
	}

	@Override
	public JsonObject getPrivacySettings() {
		if (privacy != null)
			return privacy;

		File privacyFile = new File("accounts/" + userUUID + ".privacy");
		if (privacyFile.exists()) {
			try {
				privacy = JsonParser.parseString(Files.readString(privacyFile.toPath())).getAsJsonObject();
				return privacy;
			} catch (JsonSyntaxException | IOException e) {
				privacy = new JsonObject();
				privacy.addProperty("voice_chat", "following");
			}
		}

		privacy = new JsonObject();
		privacy.addProperty("voice_chat", "following");
		savePrivacySettings(privacy);
		return privacy;
	}

	@Override
	public void savePrivacySettings(JsonObject settings) {
		privacy = settings;
		File privacyFile = new File("accounts/" + userUUID + ".privacy");
		try {
			Files.writeString(privacyFile.toPath(), privacy.toString());
		} catch (IOException e) {
		}
	}

	@Override
	public String getActiveLook() {
		if (getSaveMode() == SaveMode.MANAGED) {
			JsonObject looks = new JsonObject();
			if (mainInv.containsItem("activelooks"))
				looks = mainInv.getItem("activelooks").getAsJsonObject();
			else {
				looks.addProperty("activeLook", UUID.randomUUID().toString());
				looks.addProperty("activeSanctuaryLook", UUID.randomUUID().toString());
				mainInv.setItem("activelooks", looks);
			}
			if (!looks.has("activeLook")) {
				looks.addProperty("activeLook", UUID.randomUUID().toString());
				mainInv.setItem("activelooks", looks);
			}
			return looks.get("activeLook").getAsString();
		}

		// Looks
		File lookFiles = new File("accounts/" + userUUID + ".looks");
		lookFiles.mkdirs();

		// Active look
		File activeLookFileC = new File("accounts/" + userUUID + ".looks/active.look");
		String activeLook = UUID.randomUUID().toString();
		try {
			if (activeLookFileC.exists()) {
				activeLook = Files.readAllLines(activeLookFileC.toPath()).get(0);
			} else {
				Files.writeString(activeLookFileC.toPath(), activeLook);
			}
		} catch (IOException e) {
		}

		return activeLook;
	}

	@Override
	public String getActiveSanctuaryLook() {
		if (getSaveMode() == SaveMode.MANAGED) {
			JsonObject looks = new JsonObject();
			if (mainInv.containsItem("activelooks"))
				looks = mainInv.getItem("activelooks").getAsJsonObject();
			else {
				looks.addProperty("activeLook", UUID.randomUUID().toString());
				looks.addProperty("activeSanctuaryLook", UUID.randomUUID().toString());
				mainInv.setItem("activelooks", looks);
			}
			if (!looks.has("activeSanctuaryLook")) {
				looks.addProperty("activeSanctuaryLook", UUID.randomUUID().toString());
				mainInv.setItem("activelooks", looks);
			}
			return looks.get("activeSanctuaryLook").getAsString();
		}

		// Sanctuary looks
		File sLookFiles = new File("accounts/" + userUUID + ".sanctuary.looks");
		sLookFiles.mkdirs();

		// Active look
		File activeSLookFileC = new File("accounts/" + userUUID + ".sanctuary.looks/active.look");
		String activeSanctuaryLook = UUID.randomUUID().toString();
		try {
			if (activeSLookFileC.exists()) {
				activeSanctuaryLook = Files.readAllLines(activeSLookFileC.toPath()).get(0);
			} else {
				Files.writeString(activeSLookFileC.toPath(), activeSanctuaryLook);
			}
		} catch (IOException e) {
		}

		return activeSanctuaryLook;
	}

	@Override
	public void setActiveLook(String lookID) {
		if (lookID == null)
			return;
		if (getSaveMode() == SaveMode.MANAGED) {
			JsonObject looks = new JsonObject();
			if (mainInv.containsItem("activelooks"))
				looks = mainInv.getItem("activelooks").getAsJsonObject();
			else {
				looks.addProperty("activeSanctuaryLook", UUID.randomUUID().toString());
			}
			looks.addProperty("activeLook", lookID);
			mainInv.setItem("activelooks", looks);
			return;
		}
		try {
			File activeLookFileC = new File("accounts/" + userUUID + ".looks/active.look");
			Files.writeString(activeLookFileC.toPath(), lookID);
		} catch (IOException e) {
		}
	}

	@Override
	public void setActiveSanctuaryLook(String lookID) {
		if (getSaveMode() == SaveMode.MANAGED) {
			JsonObject looks = new JsonObject();
			if (mainInv.containsItem("activelooks"))
				looks = mainInv.getItem("activelooks").getAsJsonObject();
			else {
				looks.addProperty("activeLook", UUID.randomUUID().toString());
			}
			looks.addProperty("activeSanctuaryLook", lookID);
			mainInv.setItem("activelooks", looks);
			return;
		}
		try {
			File activeSLookFileC = new File("accounts/" + userUUID + ".sanctuary.looks/active.look");
			Files.writeString(activeSLookFileC.toPath(), lookID);
		} catch (IOException e) {
		}
	}

	@Override
	public boolean isRenameRequired() {
		return new File("accounts/" + userUUID + ".requirechangename").exists();
	}

	@Override
	public void forceNameChange() {
		if (!isRenameRequired())
			try {
				new File("accounts/" + userUUID + ".requirechangename").createNewFile();
			} catch (IOException e) {
			}
	}

	@Override
	public LevelInfo getLevel() {
		if (level == null)
			level = new LevelManager(this);

		return level;
	}

	@Override
	public long getLastLoginTime() {
		return lastLogin;
	}

	@Override
	public void login() {
		long time = System.currentTimeMillis();
		userFile.setLastModified(time);
		lastLogin = time / 1000;
	}

	@Override
	public Player getOnlinePlayerInstance() {
		return Centuria.gameServer != null ? Centuria.gameServer.getPlayer(getAccountID()) : null;
	}

	@Override
	public void deleteAccount() {
		if (!new File("accounts/" + loginName).exists()) {
			// Account does not exist
			return;
		}

		// Dispatch event
		EventBus.getInstance().dispatchEvent(new AccountDeletionEvent(this));

		// Delete login file
		new File("accounts/" + loginName).delete();

		// Kick online player first
		kick("Account deletion in progress");

		// Delete account file
		new File("accounts/" + userUUID).delete();

		// Delete account password file
		if (new File("accounts/" + userUUID + ".cred").exists())
			new File("accounts/" + userUUID + ".cred").delete();

		// Delete looks
		deleteDir(new File("accounts/" + userUUID + ".looks"));
		deleteDir(new File("accounts/" + userUUID + ".sanctuary.looks"));

		// Release display name
		AccountManager.getInstance().releaseDisplayName(displayName);

		// Delete account from the social system
		if (SocialManager.getInstance().socialListExists(userUUID)) {
			SocialEntry[] followers = SocialManager.getInstance().getFollowerPlayers(userUUID);
			SocialEntry[] followings = SocialManager.getInstance().getFollowingPlayers(userUUID);
			for (SocialEntry user : followers) {
				SocialManager.getInstance().setBlockedPlayer(user.playerID, userUUID, false);
				SocialManager.getInstance().setFollowerPlayer(user.playerID, userUUID, false);
				SocialManager.getInstance().setFollowingPlayer(user.playerID, userUUID, false);
			}
			for (SocialEntry user : followings) {
				SocialManager.getInstance().setBlockedPlayer(user.playerID, userUUID, false);
				SocialManager.getInstance().setFollowerPlayer(user.playerID, userUUID, false);
				SocialManager.getInstance().setFollowingPlayer(user.playerID, userUUID, false);
			}
			SocialManager.getInstance().deleteSocialList(userUUID);
		}

		// Delete DMs
		DMManager manager = DMManager.getInstance();
		if (getSaveSharedInventory().containsItem("dms")) {
			// Loop through all DMs and close them
			JsonObject dms = getSaveSharedInventory().getItem("dms").getAsJsonObject();
			for (String userID : dms.keySet()) {
				// Load DM id
				String dmID = dms.get(userID).getAsString();
				if (!manager.dmExists(dmID))
					continue; // Skip

				// Remove all participants
				String[] participants = manager.getDMParticipants(dmID);
				for (String participant : participants) {
					// Remove the DM from player
					CenturiaAccount otherAccount = AccountManager.getInstance().getAccount(participant);
					if (otherAccount != null) {
						// Find DMs
						if (otherAccount.getSaveSharedInventory().containsItem("dms")) {
							// Load dm from player
							JsonObject otherDMs = otherAccount.getSaveSharedInventory().getItem("dms")
									.getAsJsonObject();

							// Find DM
							for (String plr : otherDMs.keySet()) {
								if (otherDMs.get(plr).getAsString().equals(dmID)) {
									// Remove DM from player
									otherDMs.remove(plr);
									break;
								}
							}

							// Save DM object
							otherAccount.getSaveSharedInventory().setItem("dms", dms);
						}
					}
				}

				// Delete DM
				manager.deleteDM(dmID);
			}
			getSaveSharedInventory().setItem("dms", dms);
		}

		// Log
		Centuria.logger.info("Account deleted: " + getAccountID() + ", login name: " + getLoginName()
				+ ", display name: " + getDisplayName());

		// Delete inventory
		mainInv.delete();
		sharedInv.delete();
	}

	private void deleteDir(File dir) {
		if (!dir.exists())
			return;

		for (File subDir : dir.listFiles(t -> t.isDirectory())) {
			deleteDir(subDir);
		}
		for (File file : dir.listFiles(t -> !t.isDirectory())) {
			file.delete();
		}
		dir.delete();
	}

	@Override
	public SaveManager getSaveManager() throws IllegalArgumentException {
		if (manager == null)
			throw new IllegalArgumentException("Not running through managed save data");
		return manager;
	}

	@Override
	public SaveMode getSaveMode() {
		return sharedInv.containsItem("savemanifest") ? SaveMode.MANAGED : SaveMode.SINGLE;
	}

	@Override
	public PlayerInventory getSaveSharedInventory() {
		return sharedInv;
	}

	@Override
	public PlayerInventory getSaveSpecificInventory() {
		return mainInv;
	}

	@Override
	public void migrateSaveDataToManagedMode() throws IllegalArgumentException {
		if (getSaveMode() == SaveMode.MANAGED)
			throw new IllegalArgumentException("Already using managed save data");

		// Kick the player
		kick("Account data migration in progress");

		// Log
		Centuria.logger.info("Account save migration started: " + getAccountID() + ", login name: " + getLoginName()
				+ ", display name: " + getDisplayName());

		// Get looks
		String sancLook = getActiveSanctuaryLook();
		String look = getActiveLook();

		// Migrate all data
		// This... will be very tricky
		// First create a save manifest
		sharedInv.setItem("savemanifest", new JsonObject());

		// Load save manager
		manager = new FileBasedSaveManager(sharedInv, this);

		// Find default save settings
		JsonObject defaultSaveSettings;
		try {
			defaultSaveSettings = JsonParser.parseString(Files.readString(Path.of("savemanager.json")))
					.getAsJsonObject();
		} catch (JsonSyntaxException | IOException e) {
			sharedInv.deleteItem("savemanifest");
			manager = null;
			throw new RuntimeException(e);
		}

		// Create saves
		String defaultSaveName = defaultSaveSettings.get("migrationSaveName").getAsString();
		for (String saveName : defaultSaveSettings.get("saves").getAsJsonObject().keySet()) {
			JsonObject saveSettings = defaultSaveSettings.get("saves").getAsJsonObject().get(saveName)
					.getAsJsonObject();
			if (!manager.createSave(saveName)) {
				sharedInv.deleteItem("savemanifest");
				manager = null;
				throw new RuntimeException("Save creation failure");
			}

			// Write settings
			PlayerInventory inv = new FileBasedPlayerInventory(userUUID, saveName);
			SaveSettings settings = inv.getSaveSettings();
			saveSettings.addProperty("tradeLockID", saveName);
			settings.load(saveSettings);
			inv.writeSaveSettings();
		}

		// Switch save
		if (!manager.switchSave(defaultSaveName)) {
			sharedInv.deleteItem("savemanifest");
			manager = null;
			throw new RuntimeException("Save creation failure");
		}

		// Migrate player data
		PlayerInventory inv = new FileBasedPlayerInventory(userUUID, defaultSaveName);
		migrateItem(sharedInv, "1", inv);
		migrateItem(sharedInv, "10", inv);
		migrateItem(sharedInv, "100", inv);
		migrateItem(sharedInv, "102", inv);
		migrateItem(sharedInv, "104", inv);
		migrateItem(sharedInv, "105", inv);
		migrateItem(sharedInv, "110", inv);
		migrateItem(sharedInv, "111", inv);
		migrateItem(sharedInv, "2", inv);
		migrateItem(sharedInv, "201", inv);
		migrateItem(sharedInv, "3", inv);
		migrateItem(sharedInv, "103", inv);
		migrateItem(sharedInv, "300", inv);
		migrateItem(sharedInv, "302", inv);
		migrateItem(sharedInv, "303", inv);
		migrateItem(sharedInv, "304", inv);
		migrateItem(sharedInv, "311", inv);
		migrateItem(sharedInv, "315", inv);
		migrateItem(sharedInv, "4", inv);
		migrateItem(sharedInv, "400", inv);
		migrateItem(sharedInv, "5", inv);
		migrateItem(sharedInv, "6", inv);
		migrateItem(sharedInv, "7", inv);
		migrateItem(sharedInv, "8", inv);
		migrateItem(sharedInv, "9", inv);
		migrateItem(sharedInv, "avatars", inv);
		migrateItem(sharedInv, "level", inv);
		migrateItem(sharedInv, "purchaselog", inv);
		inv.setItem("finishedtutorial", new JsonObject());

		// Switch over the inventory container
		mainInv = new FileBasedPlayerInventory(userUUID, manager.getCurrentActiveSave());

		// Set active looks
		setActiveLook(look);
		setActiveSanctuaryLook(sancLook);

		// Log
		Centuria.logger.info("Account save migration finished: " + getAccountID() + ", login name: " + getLoginName()
				+ ", display name: " + getDisplayName());
	}

	private void migrateItem(FileBasedPlayerInventory sharedInv, String itm, PlayerInventory inv) {
		if (sharedInv.containsItem(itm)) {
			inv.setItem(itm, sharedInv.getItem(itm));
			sharedInv.deleteItem(itm);
		}
	}

}
