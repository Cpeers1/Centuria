package org.asf.emuferal.accounts.impl;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.UUID;

import org.asf.emuferal.EmuFeral;
import org.asf.emuferal.accounts.AccountManager;
import org.asf.emuferal.accounts.EmuFeralAccount;
import org.asf.emuferal.accounts.LevelInfo;
import org.asf.emuferal.accounts.PlayerInventory;
import org.asf.emuferal.dms.DMManager;
import org.asf.emuferal.players.Player;
import org.asf.emuferal.social.SocialEntry;
import org.asf.emuferal.social.SocialManager;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

public class FileBasedAccountObject extends EmuFeralAccount {

	private int userID;
	private boolean isNew;
	private String userUUID;
	private String loginName;
	private String displayName;
	private FileBasedPlayerInventory inv;
	private JsonObject privacy;
	private LevelInfo level;
	private long lastLogin = -1;
	private File userFile;

	public FileBasedAccountObject(File uf) throws IOException {
		// Parse account file
		userUUID = Files.readAllLines(uf.toPath()).get(0);
		loginName = Files.readAllLines(uf.toPath()).get(1);
		isNew = Files.readAllLines(uf.toPath()).get(2).equals("true");
		displayName = Files.readAllLines(uf.toPath()).get(3);
		userID = Integer.parseInt(Files.readAllLines(uf.toPath()).get(4));

		// Find existing inventory
		Player old = getOnlinePlayerInstance();
		if (old == null || !(old.account.getPlayerInventory() instanceof FileBasedPlayerInventory)) {
			// Load inventory
			inv = new FileBasedPlayerInventory(userUUID);
		} else {
			// Use the existing inventory object
			inv = (FileBasedPlayerInventory) old.account.getPlayerInventory();
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
	}

	@Override
	public boolean updateDisplayName(String name) {
		// Check validity
		if (!name.matches("^[0-9A-Za-z\\-_. ]+") || name.length() > 16 || name.length() < 2)
			return false;

		// Remove lockout
		if (isRenameRequired())
			new File("accounts/" + userUUID + ".requirechangename").delete();

		try {
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
	public PlayerInventory getPlayerInventory() {
		return inv;
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
		try {
			File activeLookFileC = new File("accounts/" + userUUID + ".looks/active.look");
			Files.writeString(activeLookFileC.toPath(), lookID);
		} catch (IOException e) {
		}
	}

	@Override
	public void setActiveSanctuaryLook(String lookID) {
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
		// TODO Auto-generated method stub

		if (level == null)
			level = new LevelInfo() {

				@Override
				public boolean isLevelAvailable() {
					return false;
				}

				@Override
				public int getLevel() {
					return -1;
				}

				@Override
				public int getTotalXP() {
					return -1;
				}

				@Override
				public int getCurrentXP() {
					return -1;
				}

				@Override
				public int getLevelupXPCount() {
					return -1;
				}

				@Override
				public void addXP(int xp) {
				}
			};

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
		return EmuFeral.gameServer.getPlayer(getAccountID());
	}

	@Override
	public void deleteAccount() {
		if (!new File("accounts/" + loginName).exists()) {
			// Account does not exist
			return;
		}

		// Delete login file
		new File("accounts/" + loginName).delete();

		// Kick online player first
		kick();

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
		if (getPlayerInventory().containsItem("dms")) {
			// Loop through all DMs and close them
			JsonObject dms = getPlayerInventory().getItem("dms").getAsJsonObject();
			for (String userID : dms.keySet()) {
				// Load DM id
				String dmID = dms.get(userID).getAsString();

				// Remove all participants
				String[] participants = manager.getDMParticipants(dmID);
				for (String participant : participants) {
					// Remove the DM from player
					EmuFeralAccount otherAccount = AccountManager.getInstance().getAccount(participant);
					if (otherAccount != null) {
						// Find DMs
						if (otherAccount.getPlayerInventory().containsItem("dms")) {
							// Load dm from player
							JsonObject otherDMs = otherAccount.getPlayerInventory().getItem("dms").getAsJsonObject();

							// Find DM
							for (String plr : otherDMs.keySet()) {
								if (otherDMs.get(plr).getAsString().equals(dmID)) {
									// Remove DM from player
									otherDMs.remove(plr);
									break;
								}
							}

							// Save DM object
							otherAccount.getPlayerInventory().setItem("dms", dms);
						}
					}
				}

				// Delete DM
				manager.deleteDM(dmID);
			}
			getPlayerInventory().setItem("dms", dms);
		}

		// Delete inventory
		inv.delete();
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

}
