package org.asf.centuria.accounts.highlevel.impl;

import java.util.ArrayList;

import org.asf.centuria.accounts.PlayerInventory;
import org.asf.centuria.accounts.highlevel.InteractionMemoryAccessor;
import org.asf.centuria.accounts.highlevel.itemdata.item.ItemComponent;
import org.asf.centuria.networking.smartfox.SmartfoxClient;
import org.asf.centuria.packets.xt.gameserver.inventory.InventoryItemPacket;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class InteractionMemoryAccessorImpl extends InteractionMemoryAccessor {

	private ArrayList<String> changedLevels = new ArrayList<String>();

	public InteractionMemoryAccessorImpl(PlayerInventory inventory) {
		super(inventory);
	}

	@Override
	public void prepareLevel(String level) {
		// Check level existence
		if (!inventory.getAccessor().hasInventoryObjectByDefId("304", level)) {
			// Create object
			inventory.getAccessor().createInventoryObject("304", level,
					new ItemComponent("TreasureInteractable", new JsonObject()), // treasure
					new ItemComponent("SocialExpanseInteractable", new JsonObject()), // harvesting
					new ItemComponent("SocialExpanseGroupLinearObjectsProgress", new JsonObject()), // unused
					new ItemComponent("DailyQuestInteractable", new JsonObject()) // daily tasks
			);

			// Mark what files to save
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

			for (String levelID : changedLevels) {
				// Add level to array
				JsonObject obj = inventory.getAccessor().findInventoryObjectByDefId("304", levelID);
				arr.add(obj);
				markChanged("304", obj.get("id").getAsString());
			}

			// Save
			saveItems("304", changedLevels.toArray(t -> new String[t]), true);

			// Clear and mark item to save
			changedLevels.clear();
			pk.item = arr;

			// Send packet
			client.sendPacket(pk);
		}
	}

	@Override
	public void prepareHarvestItem(String levelID, String itemID) {
		// Check level existence
		if (inventory.getAccessor().hasInventoryObjectByDefId("304", levelID)) {
			// Check item
			JsonObject container = resourceContainerJson(levelID, "SocialExpanseInteractable", "interactions");
			if (!container.has(itemID)) {
				// Create interaction object
				JsonObject obj = new JsonObject();
				obj.addProperty("numHarvests", 0);
				obj.addProperty("lastHarvestTime", 0);
				container.add(itemID, obj);

				// Mark what files to save
				markChanged("304", inventory.getAccessor().findInventoryObjectItemIdByDefId("304", levelID));
				if (!changedLevels.contains(levelID))
					changedLevels.add(levelID);
			}
		}
	}

	@Override
	public void prepareTreasureItem(String levelID, String itemID) {
		// Check level existence
		if (inventory.getAccessor().hasInventoryObjectByDefId("304", levelID)) {
			// Check item
			JsonObject container = resourceContainerJson(levelID, "TreasureInteractable", "interactions");
			if (!container.has(itemID)) {
				// Create interaction object
				JsonObject obj = new JsonObject();
				obj.addProperty("lastLootTime", 0);
				obj.addProperty("isLooted", false);
				container.add(itemID, obj);

				// Mark what files to save
				markChanged("304", inventory.getAccessor().findInventoryObjectItemIdByDefId("304", levelID));
				if (!changedLevels.contains(levelID))
					changedLevels.add(levelID);
			}
		}
	}

	@Override
	public void prepareDailyTaskEntry(String levelID, String itemID) {
		// Check level existence
		if (inventory.getAccessor().hasInventoryObjectByDefId("304", levelID)) {
			// Check item
			JsonObject container = resourceContainerJson(levelID, "DailyQuestInteractable", "dailyQuests");
			if (!container.has(itemID)) {
				// Create interaction object
				JsonObject obj = new JsonObject();
				obj.addProperty("lastCompletionTime", 0);
				container.add(itemID, obj);

				// Mark what files to save
				markChanged("304", inventory.getAccessor().findInventoryObjectItemIdByDefId("304", levelID));
				if (!changedLevels.contains(levelID))
					changedLevels.add(levelID);
			}
		}
	}

	@Override
	public long getLastHarvestTime(String levelID, String itemID) {
		// Check level existence
		if (inventory.getAccessor().hasInventoryObjectByDefId("304", levelID)) {
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
	public int getLastHarvestCount(String levelID, String itemID) {
		// Check level existence
		if (inventory.getAccessor().hasInventoryObjectByDefId("304", levelID)) {
			// Check item
			JsonObject container = resourceContainerJson(levelID, "SocialExpanseInteractable", "interactions");
			if (container.has(itemID)) {
				// Return harvest count
				return container.get(itemID).getAsJsonObject().get("numHarvests").getAsInt();
			}
		}
		return 0;
	}

	@Override
	public void resetHarvestCount(String levelID, String itemID) {
		// Check level existence
		if (inventory.getAccessor().hasInventoryObjectByDefId("304", levelID)) {
			// Check item
			JsonObject container = resourceContainerJson(levelID, "SocialExpanseInteractable", "interactions");
			if (container.has(itemID)) {
				// update harvest count
				var obj = container.get(itemID).getAsJsonObject();

				if (obj.has("numHarvests")) {
					obj.remove("numHarvests");
				}

				obj.addProperty("numHarvests", 0);
			}

			// Mark what files to save
			markChanged("304", inventory.getAccessor().findInventoryObjectItemIdByDefId("304", levelID));
			if (!changedLevels.contains(levelID))
				changedLevels.add(levelID);
		}
	}

	@Override
	public long getLastTreasureUnlockTime(String levelID, String itemID) {
		// Check level existence
		if (inventory.getAccessor().hasInventoryObjectByDefId("304", levelID)) {
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
	public boolean hasTreasureBeenUnlocked(String levelID, String itemID) {
		// Check level existence
		if (inventory.getAccessor().hasInventoryObjectByDefId("304", levelID)) {
			// Check item
			JsonObject container = resourceContainerJson(levelID, "TreasureInteractable", "interactions");
			if (container.has(itemID)) {
				// return status
				return container.get(itemID).getAsJsonObject().get("isLooted").getAsBoolean();
			}
		}
		return false;
	}

	@Override
	public long getLastDailyTaskTime(String levelID, String itemID) {
		// Check level existence
		if (inventory.getAccessor().hasInventoryObjectByDefId("304", levelID)) {
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
	private JsonObject resourceContainerJson(String levelID, String component, String container) {
		if (inventory.getAccessor().hasInventoryObjectByDefId("304", levelID)) {
			// Find object
			JsonObject base = inventory.getAccessor().findInventoryObjectByDefId("304", levelID);
			JsonObject item = base.get("components").getAsJsonObject();
			if (item.has(component)) {
				// Create or retrieve the container
				JsonObject cont = item.get(component).getAsJsonObject();
				if (!cont.has(container)) {
					// Create new
					cont.add(container, new JsonObject());

					// Mark what files to save
					markChanged("304", base.get("id").getAsString());
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
	public void harvested(String levelID, String itemID) {
		// Check level existence
		if (inventory.getAccessor().hasInventoryObjectByDefId("304", levelID)) {
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
			markChanged("304", inventory.getAccessor().findInventoryObjectItemIdByDefId("304", levelID));
			if (!changedLevels.contains(levelID))
				changedLevels.add(levelID);
		}
	}

	@Override
	public void unlocked(String levelID, String itemID) {
		// Check level existence
		if (inventory.getAccessor().hasInventoryObjectByDefId("304", levelID)) {
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
			markChanged("304", inventory.getAccessor().findInventoryObjectItemIdByDefId("304", levelID));
			if (!changedLevels.contains(levelID))
				changedLevels.add(levelID);
		}
	}

	@Override
	public void completedTask(String levelID, String itemID) {
		// Check level existence
		if (inventory.getAccessor().hasInventoryObjectByDefId("304", levelID)) {
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
			markChanged("304", inventory.getAccessor().findInventoryObjectItemIdByDefId("304", levelID));
			if (!changedLevels.contains(levelID))
				changedLevels.add(levelID);
		}
	}

}
