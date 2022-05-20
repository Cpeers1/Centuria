package org.asf.emuferal.accounts.impl;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.UUID;

import org.asf.emuferal.accounts.EmuFeralAccount;
import org.asf.emuferal.accounts.PlayerInventory;

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

	public FileBasedAccountObject(File uf) throws IOException {
		// Parse account file
		userUUID = Files.readAllLines(uf.toPath()).get(0);
		loginName = Files.readAllLines(uf.toPath()).get(1);
		isNew = Files.readAllLines(uf.toPath()).get(2).equals("true");
		displayName = Files.readAllLines(uf.toPath()).get(3);
		userID = Integer.parseInt(Files.readAllLines(uf.toPath()).get(4));

		// Load inventory object
		inv = new FileBasedPlayerInventory(userUUID);
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
		if (!name.matches("^[0-9A-Za-z\\-_.]+") || name.length() > 16 || name.length() < 2)
			return false;

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

}
