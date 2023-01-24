package org.asf.centuria.accounts.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.UUID;
import java.util.function.Consumer;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import org.apache.logging.log4j.MarkerManager;
import org.asf.centuria.Centuria;
import org.asf.centuria.accounts.AccountManager;
import org.asf.centuria.accounts.CenturiaAccount;
import org.asf.centuria.accounts.PlayerInventory;
import org.asf.centuria.accounts.SaveManager;
import org.asf.centuria.accounts.SaveSettings;
import org.asf.centuria.modules.eventbus.EventBus;
import org.asf.centuria.modules.events.accounts.AccountRegistrationEvent;
import org.asf.centuria.packets.xt.gameserver.inventory.InventoryItemDownloadPacket;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

public class FileBasedAccountManager extends AccountManager {

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

	private static SecureRandom rnd = new SecureRandom();
	private static HashMap<String, Integer> passswordLock = new HashMap<String, Integer>();
	private int lastAccountID = 0;

	public FileBasedAccountManager() {
		File idTrackFile = new File("account.lastid.info");
		if (!idTrackFile.exists()) {
			if (new File("accounts").exists())
				lastAccountID = new File("accounts").listFiles().length;
			try {
				Files.writeString(Path.of("account.lastid.info"), Integer.toString(lastAccountID));
			} catch (IOException e) {
			}
		} else
			try {
				lastAccountID = Integer.valueOf(Files.readAllLines(idTrackFile.toPath()).get(0));
			} catch (NumberFormatException | IOException e) {
			}
	}

	static {
		Thread th = new Thread(() -> {
			while (true) {
				HashMap<String, Integer> passswordLock;
				while (true) {
					try {
						passswordLock = new HashMap<String, Integer>(FileBasedAccountManager.passswordLock);
						break;
					} catch (ConcurrentModificationException e) {
					}
				}

				for (String pwd : passswordLock.keySet()) {
					if (passswordLock.get(pwd) - 1 <= 0) {
						FileBasedAccountManager.passswordLock.remove(pwd);
					} else {
						FileBasedAccountManager.passswordLock.put(pwd, passswordLock.get(pwd) - 1);
					}
				}

				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					break;
				}
			}
		});
		th.setDaemon(true);
		th.start();
	}

	@Override
	public String authenticate(String username, char[] password) {
		// Check name validity
		if (!username.matches("^[A-Za-z0-9@._#]+$") || username.contains(".cred")
				|| !username.matches(".*[A-Za-z0-9]+.*") || username.isBlank())
			return null;

		// Find the account
		String id = null;
		if (!new File("accounts").exists())
			new File("accounts").mkdirs();
		File uf = new File("accounts/" + username);
		if (uf.exists()) {
			try {
				id = Files.readAllLines(uf.toPath()).get(0);
			} catch (IOException e) {
			}
		} else
			return null;

		// Return null if the password is on cooldown
		if (passswordLock.containsKey(id))
			return null;

		// If it has one, check password
		File credentialFile = new File("accounts/" + id + ".cred");
		if (hasPassword(id) && !isPasswordUpdateRequested(id)) {
			try {
				// Load credentials
				FileInputStream strm = new FileInputStream(credentialFile);
				int length = ByteBuffer.wrap(strm.readNBytes(4)).getInt();
				byte[] salt = new byte[length];
				for (int i = 0; i < length; i++) {
					salt[i] = (byte) strm.read();
				}
				length = ByteBuffer.wrap(strm.readNBytes(4)).getInt();
				byte[] hash = new byte[length];
				for (int i = 0; i < length; i++) {
					hash[i] = (byte) strm.read();
				}
				strm.close();

				// Get current hash
				byte[] cHash = getHash(salt, password);

				// Compare hashes
				if (hash.length != cHash.length) {
					passswordLock.put(id, 8);
					try {
						Thread.sleep(8000);
					} catch (InterruptedException e) {
					}
					return null;
				}
				for (int i = 0; i < hash.length; i++) {
					if (hash[i] != cHash[i]) {
						passswordLock.put(id, 8);
						try {
							Thread.sleep(8000);
						} catch (InterruptedException e) {
						}
						return null;
					}
				}
			} catch (IOException e) {
				try {
					Thread.sleep(8000);
				} catch (InterruptedException e2) {
				}
				return null;
			}
		}

		// Login is valid
		return id;
	}

	@Override
	public boolean isPasswordUpdateRequested(String userID) {
		return new File("accounts/" + userID + ".credsave").exists();
	}

	@Override
	public void makePasswordUpdateRequested(String userID) {
		try {
			new File("accounts/" + userID + ".credsave").createNewFile();
		} catch (IOException e) {
		}
	}

	@Override
	public boolean hasPassword(String userID) {
		return new File("accounts/" + userID + ".cred").exists();
	}

	@Override
	public boolean updatePassword(String userID, char[] password) {
		try {
			// Generate salt and hash
			byte[] salt = salt();
			byte[] hash = getHash(salt, password);

			// Save
			File credentialFile = new File("accounts/" + userID + ".cred");
			FileOutputStream strm = new FileOutputStream(credentialFile);
			strm.write(ByteBuffer.allocate(4).putInt(salt.length).array());
			strm.write(salt);
			strm.write(ByteBuffer.allocate(4).putInt(hash.length).array());
			strm.write(hash);
			strm.close();

			// Delete request
			if (new File("accounts/" + userID + ".credsave").exists())
				new File("accounts/" + userID + ".credsave").delete();
			return true;
		} catch (Exception e) {
		}
		return false;
	}

	@Override
	public String register(String username) {
		// Check name validity
		if (!username.matches("^[A-Za-z0-9@._#]+$") || username.contains(".cred")
				|| !username.matches(".*[A-Za-z0-9]+.*") || username.isBlank() || username.length() > 320)
			return null;

		// Prevent banned and filtered words
		for (String word : username.split(" ")) {
			if (muteWords.contains(word.replaceAll("[^A-Za-z0-9]", "").toLowerCase())) {
				return null;
			}

			if (filterWords.contains(word.replaceAll("[^A-Za-z0-9]", "").toLowerCase())) {
				return null;
			}
		}

		// Prevent blacklisted names from being used
		for (String name : nameBlacklist) {
			if (username.equalsIgnoreCase(name))
				return null;
		}

		try {
			// Create folder
			if (!new File("accounts").exists())
				new File("accounts").mkdirs();

			// Find a ID that isn't in use
			String id = UUID.randomUUID().toString();
			while (new File("accounts/" + id).exists())
				id = UUID.randomUUID().toString();

			// Save account details
			File uf = new File("accounts/" + username);
			if (uf.exists()) {
				// Account exists, return null
				return null;
			}

			// Increase last ID and save it to disk
			lastAccountID++;
			Files.writeString(Path.of("account.lastid.info"), Integer.toString(lastAccountID));

			Files.writeString(uf.toPath(), id + "\n" + username);
			Files.writeString(new File("accounts/" + id).toPath(),
					id + "\n" + username + "\ntrue\n" + username + "\n" + lastAccountID);

			// Managed save mode if enabled
			if (Centuria.defaultUseManagedSaves) {
				// Create managed save data

				// First create a save manifest
				PlayerInventory sharedInv = new FileBasedPlayerInventory(id, "");
				sharedInv.setItem("savemanifest", new JsonObject());

				// Load save manager
				SaveManager manager = new FileBasedSaveManager(sharedInv, getAccount(id));

				// Find default save settings
				JsonObject defaultSaveSettings;
				try {
					defaultSaveSettings = JsonParser.parseString(Files.readString(Path.of("savemanager.json")))
							.getAsJsonObject();
				} catch (JsonSyntaxException | IOException e) {
					sharedInv.deleteItem("savemanifest");
					throw new RuntimeException(e);
				}

				// Create saves
				String defaultSaveName = defaultSaveSettings.get("migrationSaveName").getAsString();
				for (String saveName : defaultSaveSettings.get("saves").getAsJsonObject().keySet()) {
					JsonObject saveSettings = defaultSaveSettings.get("saves").getAsJsonObject().get(saveName)
							.getAsJsonObject();
					if (!manager.createSave(saveName)) {
						sharedInv.deleteItem("savemanifest");
						throw new RuntimeException("Save creation failure");
					}

					// Write settings
					PlayerInventory inv = new FileBasedPlayerInventory(id, saveName);
					SaveSettings settings = inv.getSaveSettings();
					saveSettings.addProperty("tradeLockID", saveName);
					settings.load(saveSettings);
					inv.writeSaveSettings();
				}

				// Switch save
				if (!manager.switchSave(defaultSaveName)) {
					sharedInv.deleteItem("savemanifest");
					throw new RuntimeException("Save creation failure");
				}
			}

			// Dispatch event
			EventBus.getInstance().dispatchEvent(new AccountRegistrationEvent(getAccount(id)));

			// Log
			Centuria.logger.info(MarkerManager.getMarker("Accounts"),
					"Account registered: " + id + ", login name: " + username);

			// Return account ID
			return id;
		} catch (IOException e) {
		}

		return null; // Failure
	}

	@Override
	public CenturiaAccount getAccount(String userID) {
		// Find the account
		File uf = new File("accounts/" + userID);
		if (uf.exists())
			try {
				return new FileBasedAccountObject(uf);
			} catch (IOException e) {
				return null;
			}
		else
			return null;
	}

	// Salt and hash
	private static byte[] salt() {
		byte[] salt = new byte[32];
		rnd.nextBytes(salt);
		return salt;
	}

	public static byte[] getHash(byte[] salt, char[] password) {
		KeySpec spec = new PBEKeySpec(password, salt, 65536, 128);
		try {
			SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512");
			return factory.generateSecret(spec).getEncoded();
		} catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
			return null;
		}
	}

	@Override
	public String getUserByLoginName(String loginName) {
		// Check name validity
		if (!loginName.matches("^[A-Za-z0-9@._#]+$") || loginName.contains(".cred")
				|| !loginName.matches(".*[A-Za-z0-9]+.*") || loginName.isBlank())
			return null;

		// Find the account
		if (!new File("accounts").exists())
			new File("accounts").mkdirs();
		File uf = new File("accounts/" + loginName);
		if (uf.exists()) {
			try {
				String userID = Files.readAllLines(uf.toPath()).get(0);

				// Check existence
				if (new File("accounts/" + userID).exists())
					return userID; // Account found
			} catch (IOException e) {
			}
		}

		// Account not found
		return null;
	}

	@Override
	public String getUserByDisplayName(String displayName) {
		// Check validity
		if (!displayName.matches("^[0-9A-Za-z\\-_. ]+") || displayName.length() > 16 || displayName.length() < 2)
			return null;

		// Find file
		if (new File("displaynames/" + displayName).exists()) {
			try {
				String userID = Files.readAllLines(Path.of("displaynames/" + displayName)).get(0);

				// Check existence
				if (new File("accounts/" + userID).exists())
					return userID; // Account found
			} catch (IOException e) {
			}
		}

		// Attempt to find using a case-insensitive method
		if (new File("displaynames").exists())
			for (File dsp : new File("displaynames").listFiles(t -> !t.isDirectory())) {
				if (dsp.getName().equalsIgnoreCase(displayName))
					try {
						String userID = Files.readAllLines(dsp.toPath()).get(0);

						// Check existence
						if (new File("accounts/" + userID).exists())
							return userID; // Account found
					} catch (IOException e) {
					}
			}

		return null;
	}

	@Override
	public boolean isDisplayNameInUse(String displayName) {
		if (new File("displaynames/" + displayName).exists())
			return true;

		// Prevent blacklisted names from being used
		for (String name : nameBlacklist) {
			if (displayName.equalsIgnoreCase(name))
				return true;
		}

		// Attempt to find using a case-insensitive method
		if (new File("displaynames").exists())
			for (File dsp : new File("displaynames").listFiles(t -> !t.isDirectory())) {
				if (dsp.getName().equalsIgnoreCase(displayName))
					return true;
			}

		return false;
	}

	@Override
	public boolean releaseDisplayName(String displayName) {
		if (new File("displaynames/" + displayName).exists()) {
			new File("displaynames/" + displayName).delete();
			return true;
		}

		// Attempt to find using a case-insensitive method
		if (new File("displaynames").exists())
			for (File dsp : new File("displaynames").listFiles(t -> !t.isDirectory())) {
				if (dsp.getName().equalsIgnoreCase(displayName)) {
					dsp.delete();
					return true;
				}
			}

		return false;
	}

	@Override
	public boolean lockDisplayName(String displayName, String userID) {
		if (!isDisplayNameInUse(displayName)) {
			if (!new File("displaynames").exists())
				new File("displaynames").mkdirs();

			try {
				Files.writeString(Path.of("displaynames/" + displayName), userID);
				return true;
			} catch (IOException e) {
			}
		}

		return false;
	}

	@Override
	public void runForAllAccounts(Consumer<CenturiaAccount> action) {
		// Find all accounts
		for (File accFile : new File("accounts").listFiles(t -> !t.isDirectory())) {
			try {
				UUID.fromString(accFile.getName());

				// Run action
				action.accept(getAccount(accFile.getName()));
			} catch (Exception e) {
				// Not a account file
			}
		}
	}

	@Override
	public void releaseLoginName(String username) {
		// Check name validity
		if (!username.matches("^[A-Za-z0-9@._#]+$") || username.contains(".cred")
				|| !username.matches(".*[A-Za-z0-9]+.*") || username.isBlank() || username.length() > 320)
			return;
		File f = new File("accounts/" + username);
		if (f.exists())
			f.delete();
	}

}
