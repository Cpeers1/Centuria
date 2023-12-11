package org.asf.centuria.accounts.impl;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;

import org.asf.centuria.accounts.PlayerInventory;
import org.asf.centuria.accounts.SaveSettings;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class FileBasedPlayerInventory extends PlayerInventory {

	private String id;
	private HashMap<String, JsonElement> cache = new HashMap<String, JsonElement>();
	private SaveSettings settings;
	private String prefix = "";

	public FileBasedPlayerInventory(String userID, String save) {
		id = userID;

		// Create directories
		if (!new File("inventories/" + id + "/" + save).exists()) {
			new File("inventories/" + id + "/" + save).mkdirs();
		}

		// Load save settings
		settings = new SaveSettings();
		prefix = "/" + save;
		if (containsItem("savesettings"))
			settings.load(getItem("savesettings").getAsJsonObject());

		// Data fixer
		if (!new File("inventories/" + id + "/" + save + "/fixed").exists())
			return;

		// Reset lockpicks
		if (getAccessor().findInventoryObject("104", 8372) == null) {
			new File("inventories/" + id + "/" + save + "/fixed").delete();
			return;
		}
		JsonObject obj = getAccessor().findInventoryObject("104", 8372).get("components").getAsJsonObject()
				.get("Quantity").getAsJsonObject();
		obj.remove("quantity");
		obj.addProperty("quantity", 0);
		setItem("104", getItem("104"));

		new File("inventories/" + id + "/" + save + "/fixed").delete();
	}

	@Override
	public boolean containsItem(String itemID) {
		if (!itemID.matches("^[A-Za-z0-9_\\-.]+"))
			return false;

		if (cache.containsKey(itemID))
			return true;

		return new File("inventories/" + id + prefix + "/" + itemID + ".json").exists();
	}

	@Override
	public JsonElement getItem(String itemID) {
		if (!itemID.matches("^[A-Za-z0-9_\\-.]+"))
			return null;

		if (cache.containsKey(itemID))
			return cache.get(itemID);

		if (new File("inventories/" + id + prefix + "/" + itemID + ".json").exists()) {
			try {
				String json = Files.readString(Path.of("inventories/" + id + prefix + "/" + itemID + ".json"));
				JsonElement ele = JsonParser.parseString(json);
				cache.put(itemID, ele);

				// Load into accessor cache
				if (ele.isJsonArray()) {
					ele.getAsJsonArray().forEach(t -> {
						if (t.isJsonObject()) {
							JsonObject obj = t.getAsJsonObject();
							if (obj.has("id") && obj.has("type")) {
								getAccessor().cacheItem(obj.get("id").getAsString(), obj.get("type").getAsString());
							}
						}
					});
				}
				return cache.get(itemID);
			} catch (IOException e) {
			}
		}
		return null;
	}

	@Override
	public void setItem(String itemID, JsonElement itemData) {
		if (!itemID.matches("^[A-Za-z0-9_\\-.]+"))
			return;

		cache.put(itemID, itemData);
		try {
			Files.writeString(Path.of("inventories/" + id + prefix + "/" + itemID + ".json"), itemData.toString());

			// Load into accessor cache
			if (itemData.isJsonArray()) {
				itemData.getAsJsonArray().forEach(t -> {
					if (t.isJsonObject()) {
						JsonObject obj = t.getAsJsonObject();
						if (obj.has("id") && obj.has("type")) {
							getAccessor().cacheItem(obj.get("id").getAsString(), obj.get("type").getAsString());
						}
					}
				});
			}
		} catch (IOException e) {
		}
	}

	@Override
	public void deleteItem(String itemID) {
		if (!itemID.matches("^[A-Za-z0-9_\\-.]+"))
			return;

		if (cache.containsKey(itemID)) {
			JsonElement itemData = cache.remove(itemID);

			// Remove accessor cache
			if (itemData.isJsonArray()) {
				itemData.getAsJsonArray().forEach(t -> {
					if (t.isJsonObject()) {
						JsonObject obj = t.getAsJsonObject();
						if (obj.has("id") && obj.has("type")) {
							getAccessor().removeItemFromCache(obj.get("id").getAsString());
						}
					}
				});
			}
		}
		if (new File("inventories/" + id + prefix + "/" + itemID + ".json").exists())
			new File("inventories/" + id + prefix + "/" + itemID + ".json").delete();
	}

	/**
	 * Deletes the inventory from disk
	 */
	public void delete() {
		deleteDir(new File("inventories/" + id + prefix));
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
	public SaveSettings getSaveSettings() {
		return settings;
	}

	@Override
	public void writeSaveSettings() {
		setItem("savesettings", settings.writeToObject());
	}

}
