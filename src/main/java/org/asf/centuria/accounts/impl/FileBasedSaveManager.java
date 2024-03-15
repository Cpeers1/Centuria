package org.asf.centuria.accounts.impl;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

import org.apache.logging.log4j.MarkerManager;
import org.asf.centuria.Centuria;
import org.asf.centuria.accounts.CenturiaAccount;
import org.asf.centuria.accounts.PlayerInventory;
import org.asf.centuria.accounts.SaveManager;
import org.asf.centuria.accounts.SaveSettings;

import com.google.gson.JsonObject;

public class FileBasedSaveManager extends SaveManager {

	private PlayerInventory sharedInv;
	private CenturiaAccount account;

	public FileBasedSaveManager(PlayerInventory sharedInv, CenturiaAccount account) {
		this.account = account;
		this.sharedInv = sharedInv;
	}

	@Override
	public String getCurrentActiveSave() {
		return sharedInv.getItem("savemanifest").getAsJsonObject().get("activeSave").getAsString();
	}

	@Override
	public boolean saveExists(String save) {
		if (!save.matches("^[A-Za-z0-9 _.()\\-]+$"))
			return false;
		return new File("inventories/" + account.getAccountID() + "/" + save).exists();
	}

	@Override
	public boolean createSave(String save) {
		if (!save.matches("^[A-Za-z0-9 _.()\\-]+$"))
			return false;
		if (saveExists(save))
			return false;

		// Create save
		new File("inventories/" + account.getAccountID() + "/" + save).mkdirs();

		// Write default settings
		try {
			SaveSettings settings = new SaveSettings();
			settings.tradeLockID = save;
			Files.writeString(Path.of("inventories/" + account.getAccountID() + "/" + save + "/savesettings.json"),
					settings.writeToObject().toString());
		} catch (IOException e) {
			return false; // Failed
		}

		// Log
		Centuria.logger.info(MarkerManager.getMarker("SaveManager"),
				"Created save: " + save + " for " + account.getAccountID() + " (" + account.getLoginName() + ")");
		return true;
	}

	private void deleteDir(File dir) {
		if (!dir.exists())
			return;
		if (Files.isSymbolicLink(dir.toPath())) {
			// DO NOT RECURSE
			dir.delete();
			return;
		}

		for (File subDir : dir.listFiles(t -> t.isDirectory())) {
			deleteDir(subDir);
		}
		for (File file : dir.listFiles(t -> !t.isDirectory())) {
			file.delete();
		}
		dir.delete();
	}

	@Override
	public boolean deleteSave(String save) {
		if (!save.matches("^[A-Za-z0-9 _.()\\-]+$"))
			return false;
		if (!saveExists(save))
			return false;

		// Delete data
		deleteDir(new File("inventories/" + account.getAccountID() + "/" + save));

		// Log
		Centuria.logger.info(MarkerManager.getMarker("SaveManager"),
				"Deleted save: " + save + " of " + account.getAccountID() + " (" + account.getLoginName() + ")");
		return true;
	}

	@Override
	public boolean switchSave(String save) {
		if (!save.matches("^[A-Za-z0-9 _.()\\-]+$"))
			return false;
		if (!saveExists(save))
			return false;

		// Switch save
		JsonObject man = sharedInv.getItem("savemanifest").getAsJsonObject();
		man.addProperty("activeSave", save);
		sharedInv.setItem("savemanifest", man);

		// Log
		Centuria.logger.info(MarkerManager.getMarker("SaveManager"), "Switch active save to " + save + " for "
				+ account.getAccountID() + " (" + account.getLoginName() + ")");
		return true;
	}

	@Override
	public String[] getSaves() {
		ArrayList<String> saves = new ArrayList<String>();

		// Find saves
		for (File dir : new File("inventories/" + account.getAccountID()).listFiles(t -> t.isDirectory())) {
			if (new File(dir, "savesettings.json").exists())
				saves.add(dir.getName());
		}

		return saves.stream().sorted().toArray(t -> new String[t]);
	}

	@Override
	public PlayerInventory getSaveSpecificInventoryOf(String save) {
		if (!saveExists(save))
			return null;
		return new FileBasedPlayerInventory(account.getAccountID(), save);
	}

}
