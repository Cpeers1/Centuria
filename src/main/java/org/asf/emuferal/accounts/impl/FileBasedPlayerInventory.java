package org.asf.emuferal.accounts.impl;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;

import org.asf.emuferal.accounts.PlayerInventory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

public class FileBasedPlayerInventory extends PlayerInventory {

	private String id;
	private HashMap<String, JsonElement> cache = new HashMap<String, JsonElement>();

	public FileBasedPlayerInventory(String userID) {
		id = userID;

		// Data fixer
		try {
			if (!new File("inventories/" + id).exists()) {
				new File("inventories/" + id).mkdirs();
				new File("inventories/" + id + "/fixed").createNewFile();
				return;
			}

			if (new File("inventories/" + id + "/fixed").exists())
				return;

			// Combine all objects
			JsonArray inventory = new JsonArray();
			for (File obj : new File("inventories/" + id).listFiles(t -> t.isFile() && t.getName().endsWith(".json"))) {
				// Check validity
				if (!obj.getName().matches("^[0-9]+\\.json$"))
					continue;
				
				// Load object
				JsonElement ele = JsonParser.parseString(Files.readString(obj.toPath()));
				if (ele.isJsonArray()) {
					inventory.addAll(ele.getAsJsonArray());
					obj.delete();
				}
			}

			// Split objects
			HashMap<String, JsonArray> inventoryObjects = new HashMap<String, JsonArray>();
			for (JsonElement ele : inventory) {
				String type = ele.getAsJsonObject().get("type").getAsString();
				if (!inventoryObjects.containsKey(type))
					inventoryObjects.put(type, new JsonArray());
				inventoryObjects.get(type).add(ele);
			}

			// Save objects
			inventoryObjects.forEach((file, object) -> {
				try {
					Files.writeString(Path.of("inventories/" + id + "/" + file + ".json"), object.toString());
				} catch (IOException e) {
				}
			});

			new File("inventories/" + id + "/fixed").createNewFile();
		} catch (IOException e) {
		}
	}

	@Override
	public boolean containsItem(String itemID) {
		if (!itemID.matches("^[A-Za-z0-9]+"))
			return false;

		if (cache.containsKey(itemID))
			return true;

		return new File("inventories/" + id + "/" + itemID + ".json").exists();
	}

	@Override
	public JsonElement getItem(String itemID) {
		if (!itemID.matches("^[A-Za-z0-9]+"))
			return null;

		if (cache.containsKey(itemID))
			return cache.get(itemID);

		if (new File("inventories/" + id + "/" + itemID + ".json").exists()) {
			try {
				String json = Files.readString(Path.of("inventories/" + id + "/" + itemID + ".json"));
				cache.put(itemID, JsonParser.parseString(json));
				return cache.get(itemID);
			} catch (IOException e) {
			}
		}
		return null;
	}

	@Override
	public void setItem(String itemID, JsonElement itemData) {
		if (!itemID.matches("^[A-Za-z0-9]+"))
			return;

		cache.put(itemID, itemData);
		try {
			if (!new File("inventories/" + id).exists()) {
				new File("inventories/" + id).mkdirs();
			}
			Files.writeString(Path.of("inventories/" + id + "/" + itemID + ".json"), itemData.toString());
		} catch (IOException e) {
		}
	}

	@Override
	public void deleteItem(String itemID) {
		if (!itemID.matches("^[A-Za-z0-9]+"))
			return;

		if (cache.containsKey(itemID))
			cache.remove(itemID);
		if (new File("inventories/" + id + "/" + itemID + ".json").exists())
			new File("inventories/" + id + "/" + itemID + ".json").delete();
	}

	/**
	 * Deletes the inventory from disk
	 */
	public void delete() {
		deleteDir(new File("inventories/" + id));
	}

	private void deleteDir(File dir) {
		for (File subDir : dir.listFiles(t -> t.isDirectory())) {
			deleteDir(subDir);
		}
		for (File file : dir.listFiles(t -> !t.isDirectory())) {
			file.delete();
		}
		dir.delete();
	}

}
