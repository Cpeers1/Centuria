package org.asf.emuferal.accounts.highlevel.impl;

import java.util.ArrayList;

import org.asf.emuferal.accounts.PlayerInventory;
import org.asf.emuferal.accounts.highlevel.InteractionMemoryAccessor;
import org.asf.emuferal.accounts.highlevel.itemdata.item.ItemComponent;
import org.asf.emuferal.networking.smartfox.SmartfoxClient;
import org.asf.emuferal.packets.xt.gameserver.inventory.InventoryItemPacket;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class InteractionMemoryAccessorImpl extends InteractionMemoryAccessor {

	private ArrayList<Integer> changedLevels = new ArrayList<Integer>();

	public InteractionMemoryAccessorImpl(PlayerInventory inventory) {
		super(inventory);
	}

	@Override
	public void prepareLevel(int level) {
		// Check level existence
		if (!inventory.getAccessor().hasInventoryObject("304", level)) {
			// Create object
			inventory.getAccessor().createInventoryObject("304", level,
					new ItemComponent("TreasureInteractable", new JsonObject()), // treasure
					new ItemComponent("SocialExpanseInteractable", new JsonObject()), // harvesting
					new ItemComponent("SocialExpanseGroupLinearObjectsProgress", new JsonObject()), // unused
					new ItemComponent("DailyQuestInteractable", new JsonObject()) // daily tasks
			);

			// Mark what files to save
			addItemToSave("304");
			if (!changedLevels.contains(level))
				changedLevels.add(level);
		}
	}

	@Override
	public void saveTo(SmartfoxClient client) {
		// Send changed levels
		if (changedLevels.size() != 0) {
			InventoryItemPacket pk = new InventoryItemPacket();
			JsonArray arr = new JsonArray();

			for (Integer levelID : changedLevels) {
				// Add level to array
				arr.add(inventory.getAccessor().findInventoryObject("304", levelID));
			}

			// Clear and mark item to save
			changedLevels.clear();
			addItemToSave("304");
			pk.item = arr;

			// Send packet
			client.sendPacket(pk);
		}

		// Save
		String[] items = getItemsToSave();
		for (String item : items)
			inventory.setItem(item, inventory.getItem(item));
		completedSave();
	}

	@Override
	public void prepareHarvestItem(int levelID, String itemID) {
		// Check level existence
		if (inventory.getAccessor().hasInventoryObject("304", levelID)) {
			// Check item
			JsonObject container = resourceContainerJson(levelID, "SocialExpanseInteractable", "interactions");
			if (!container.has(itemID)) {
				// Create interaction object
				JsonObject obj = new JsonObject();
				obj.addProperty("numHarvests", 0);
				obj.addProperty("lastHarvestTime", 0);
				container.add(itemID, obj);

				// Mark what files to save
				addItemToSave("304");
				if (!changedLevels.contains(levelID))
					changedLevels.add(levelID);
			}
		}
	}

	@Override
	public void prepareTreasureItem(int levelID, String itemID) {
		// Check level existence
		if (inventory.getAccessor().hasInventoryObject("304", levelID)) {
			// Check item
			JsonObject container = resourceContainerJson(levelID, "TreasureInteractable", "interactions");
			if (!container.has(itemID)) {
				// Create interaction object
				JsonObject obj = new JsonObject();
				obj.addProperty("lastLootTime", 0);
				obj.addProperty("isLooted", false);
				container.add(itemID, obj);

				// Mark what files to save
				addItemToSave("304");
				if (!changedLevels.contains(levelID))
					changedLevels.add(levelID);
			}
		}
	}

	@Override
	public void prepareDailyTaskEntry(int levelID, String itemID) {
		// Check level existence
		if (inventory.getAccessor().hasInventoryObject("304", levelID)) {
			// Check item
			JsonObject container = resourceContainerJson(levelID, "DailyQuestInteractable", "dailyQuests");
			if (!container.has(itemID)) {
				// Create interaction object
				JsonObject obj = new JsonObject();
				obj.addProperty("lastCompletionTime", 0);
				container.add(itemID, obj);

				// Mark what files to save
				addItemToSave("304");
				if (!changedLevels.contains(levelID))
					changedLevels.add(levelID);
			}
		}
	}

	@Override
	public long getLastHarvestTime(int levelID, String itemID) {
		// Check level existence
		if (inventory.getAccessor().hasInventoryObject("304", levelID)) {
			// Check item
			JsonObject container = resourceContainerJson(levelID, "SocialExpanseInteractable", "interactions");
			if (container.has(itemID)) {
				// Return timestamp
				return container.get(itemID).getAsJsonObject().get("lastHarvestTime").getAsLong();
			}
		}
		return 0;
	}

	@Override
	public int getLastHarvestCount(int levelID, String itemID) {
		// Check level existence
		if (inventory.getAccessor().hasInventoryObject("304", levelID)) {
			// Check item
			JsonObject container = resourceContainerJson(levelID, "SocialExpanseInteractable", "interactions");
			if (container.has(itemID)) {
				// Return timestamp
				return container.get(itemID).getAsJsonObject().get("numHarvests").getAsInt();
			}
		}
		return 0;
	}

	@Override
	public long getLastTreasureUnlockTime(int levelID, String itemID) {
		// Check level existence
		if (inventory.getAccessor().hasInventoryObject("304", levelID)) {
			// Check item
			JsonObject container = resourceContainerJson(levelID, "TreasureInteractable", "interactions");
			if (container.has(itemID)) {
				// Return timestamp
				return container.get(itemID).getAsJsonObject().get("lastLootTime").getAsLong();
			}
		}
		return 0;
	}

	@Override
	public boolean hasTreasureBeenUnlocked(int levelID, String itemID) {
		// Check level existence
		if (inventory.getAccessor().hasInventoryObject("304", levelID)) {
			// Check item
			JsonObject container = resourceContainerJson(levelID, "TreasureInteractable", "interactions");
			if (container.has(itemID)) {
				// Return timestamp
				return container.get(itemID).getAsJsonObject().get("isLooted").getAsBoolean();
			}
		}
		return false;
	}

	@Override
	public long getLastDailyTaskTime(int levelID, String itemID) {
		// Check level existence
		if (inventory.getAccessor().hasInventoryObject("304", levelID)) {
			// Check item
			JsonObject container = resourceContainerJson(levelID, "DailyQuestInteractable", "dailyQuests");
			if (container.has(itemID)) {
				// Return timestamp
				return container.get(itemID).getAsJsonObject().get("lastCompletionTime").getAsLong();
			}
		}
		return 0;
	}

	// Shared utility for memory containers
	private JsonObject resourceContainerJson(int levelID, String component, String container) {
		if (inventory.getAccessor().hasInventoryObject("304", levelID)) {
			// Find object
			JsonObject item = inventory.getAccessor().findInventoryObject("304", levelID).get("components")
					.getAsJsonObject();
			if (item.has(component)) {
				// Create or retrieve the container
				JsonObject cont = item.get(component).getAsJsonObject();
				if (!cont.has(container)) {
					// Create new
					cont.add(container, new JsonObject());

					// Mark what files to save
					addItemToSave("304");
					if (!changedLevels.contains(levelID))
						changedLevels.add(levelID);
				}

				// Return container
				return cont.get(container).getAsJsonObject();
			} else {
				// Invalid
				return null;
			}
		}

		// Invalid
		return null;
	}

	@Override
	public void harvested(int levelID, String itemID) {
		// Check level existence
		if (inventory.getAccessor().hasInventoryObject("304", levelID)) {
			int harvests = getLastHarvestCount(levelID, itemID);

			// Check item
			JsonObject container = resourceContainerJson(levelID, "SocialExpanseInteractable", "interactions");
			if (container.has(itemID)) {
				// Delete old object
				container.remove(itemID);
			}

			// Create interaction object
			JsonObject obj = new JsonObject();
			obj.addProperty("numHarvests", harvests + 1);
			obj.addProperty("lastHarvestTime", System.currentTimeMillis());
			container.add(itemID, obj);

			// Mark what files to save
			addItemToSave("304");
			if (!changedLevels.contains(levelID))
				changedLevels.add(levelID);
		}
	}

	@Override
	public void unlocked(int levelID, String itemID) {
		// Check level existence
		if (inventory.getAccessor().hasInventoryObject("304", levelID)) {
			// Check item
			JsonObject container = resourceContainerJson(levelID, "TreasureInteractable", "interactions");
			if (container.has(itemID)) {
				// Delete old object
				container.remove(itemID);
			}

			// Create interaction object
			JsonObject obj = new JsonObject();
			obj.addProperty("lastLootTime", System.currentTimeMillis());
			obj.addProperty("isLooted", true);
			container.add(itemID, obj);

			// Mark what files to save
			addItemToSave("304");
			if (!changedLevels.contains(levelID))
				changedLevels.add(levelID);
		}
	}

	@Override
	public void completedTask(int levelID, String itemID) {
		// Check level existence
		if (inventory.getAccessor().hasInventoryObject("304", levelID)) {
			// Check item
			JsonObject container = resourceContainerJson(levelID, "DailyQuestInteractable", "dailyQuests");
			if (container.has(itemID)) {
				// Delete old object
				container.remove(itemID);
			}

			// Create interaction object
			JsonObject obj = new JsonObject();
			obj.addProperty("lastCompletionTime", System.currentTimeMillis());
			container.add(itemID, obj);

			// Mark what files to save
			addItemToSave("304");
			if (!changedLevels.contains(levelID))
				changedLevels.add(levelID);
		}
	}

}
