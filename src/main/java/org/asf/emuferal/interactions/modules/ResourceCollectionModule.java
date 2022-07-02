package org.asf.emuferal.interactions.modules;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import org.asf.emuferal.interactions.NetworkedObjects;
import org.asf.emuferal.interactions.dataobjects.NetworkedObject;
import org.asf.emuferal.interactions.dataobjects.StateInfo;
import org.asf.emuferal.interactions.modules.resourcecollection.ResourceDefinition;
import org.asf.emuferal.interactions.modules.resourcecollection.ResourceType;
import org.asf.emuferal.interactions.modules.resourcecollection.rewards.HarvestReward;
import org.asf.emuferal.interactions.modules.resourcecollection.rewards.LootReward;
import org.asf.emuferal.interactions.modules.resourcecollection.tables.HarvestTable;
import org.asf.emuferal.interactions.modules.resourcecollection.tables.LootTable;
import org.asf.emuferal.modules.IEmuFeralModule;
import org.asf.emuferal.modules.ModuleManager;
import org.asf.emuferal.networking.smartfox.SmartfoxClient;
import org.asf.emuferal.packets.xt.gameserver.inventory.InventoryItemDownloadPacket;
import org.asf.emuferal.players.Player;
import org.asf.emuferal.util.WeightedSelectorUtil;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class ResourceCollectionModule extends InteractionModule {

	private static Random rnd = new Random();
	private static HashMap<String, ResourceDefinition> resources = new HashMap<String, ResourceDefinition>();
	private static HashMap<String, HarvestTable> harvestTables = new HashMap<String, HarvestTable>();
	private static HashMap<String, LootTable> lootTables = new HashMap<String, LootTable>();

	// Called to load the tables into memory
	private static void importObjectsIntoMemory(JsonObject data) {
		// Load resources
		if (data.has("Resources")) {
			JsonObject r = data.get("Resources").getAsJsonObject();
			for (String id : r.keySet()) {
				ResourceDefinition def;
				if (resources.containsKey(id)) {
					// Transform
					def = resources.get(id);
				} else {
					// Create
					def = new ResourceDefinition();
					resources.put(id, def);
				}

				// Add items
				JsonObject defObj = r.get(id).getAsJsonObject();
				if (defObj.has("objectName"))
					def.objectName = defObj.get("objectName").getAsString();
				if (defObj.has("lootType"))
					def.lootType = defObj.get("lootType").getAsString().equals("HARVEST") ? ResourceType.HARVEST
							: ResourceType.LOOT;
				if (defObj.has("lootTableId"))
					def.lootTableId = defObj.get("lootTableId").getAsInt();
				if (defObj.has("respawnSeconds"))
					def.respawnSeconds = defObj.get("respawnSeconds").getAsDouble();
				if (defObj.has("interactionsBeforeDespawn"))
					def.interactionsBeforeDespawn = defObj.get("interactionsBeforeDespawn").getAsInt();
			}
		}

		// Load harvest tables
		if (data.has("HarvestTables")) {
			JsonObject t = data.get("HarvestTables").getAsJsonObject();
			for (String id : t.keySet()) {
				HarvestTable def;
				if (harvestTables.containsKey(id)) {
					// Transform
					def = harvestTables.get(id);
				} else {
					// Create
					def = new HarvestTable();
					harvestTables.put(id, def);
				}

				// Add items
				JsonObject defObj = t.get(id).getAsJsonObject();
				if (defObj.has("objectName"))
					def.objectName = defObj.get("objectName").getAsString();
				if (defObj.has("rewards")) {
					// Overwrite rewards
					def.rewards.clear();
					JsonArray rewards = defObj.get("rewards").getAsJsonArray();
					for (JsonElement ele : rewards) {
						JsonObject rewardInfo = ele.getAsJsonObject();
						HarvestReward reward = new HarvestReward();
						reward.itemId = rewardInfo.get("itemId").getAsString();
						reward.minCount = rewardInfo.get("minCount").getAsInt();
						reward.maxCount = rewardInfo.get("maxCount").getAsInt();
						reward.weight = rewardInfo.get("weight").getAsInt();
						def.rewards.add(reward);
					}
				}
			}
		}

		// Load loot tables
		if (data.has("LootTables")) {
			JsonObject t = data.get("LootTables").getAsJsonObject();
			for (String id : t.keySet()) {
				LootTable def;
				if (lootTables.containsKey(id)) {
					// Transform
					def = lootTables.get(id);
				} else {
					// Create
					def = new LootTable();
					lootTables.put(id, def);
				}

				// Add items
				JsonObject defObj = t.get(id).getAsJsonObject();
				if (defObj.has("objectName"))
					def.objectName = defObj.get("objectName").getAsString();
				if (defObj.has("rewards")) {
					// Overwrite rewards
					def.rewards.clear();
					JsonArray rewards = defObj.get("rewards").getAsJsonArray();
					for (JsonElement ele : rewards) {
						JsonObject rewardInfo = ele.getAsJsonObject();
						LootReward reward = new LootReward();
						if (rewardInfo.get("hasItem").getAsBoolean())
							reward.itemId = rewardInfo.get("itemId").getAsString();
						if (rewardInfo.get("hasTableReference").getAsBoolean())
							reward.referencedTableId = rewardInfo.get("referencedTableId").getAsString();
						reward.minCount = rewardInfo.get("minCount").getAsInt();
						reward.maxCount = rewardInfo.get("maxCount").getAsInt();
						reward.weight = rewardInfo.get("weight").getAsInt();
						def.rewards.add(reward);
					}
				}
			}
		}
	}

	static {
		initCollection();
	}

	private static void initCollection() {
		try {
			// Load the vanilla resource data
			InputStream strm = InventoryItemDownloadPacket.class.getClassLoader()
					.getResourceAsStream("resourcecollection.json");
			JsonObject helper = JsonParser.parseString(new String(strm.readAllBytes(), "UTF-8")).getAsJsonObject();
			importObjectsIntoMemory(helper);
			strm.close();

			// Load transformers
			loadTransformers(ResourceCollectionModule.class);

			// Load module transformers
			for (IEmuFeralModule module : ModuleManager.getInstance().getAllModules()) {
				loadTransformers(module.getClass());
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private static void loadTransformers(Class<?> cls) {
		URL source = cls.getProtectionDomain().getCodeSource().getLocation();

		// Generate a base URL
		String baseURL = "";
		String fileName = "";
		try {
			File sourceFile = new File(source.toURI());
			fileName = sourceFile.getName();
			if (sourceFile.isDirectory()) {
				baseURL = source + (source.toString().endsWith("/") ? "" : "/");
			} else {
				baseURL = "jar:" + source + "!/";
			}
		} catch (Exception e) {
			return;
		}

		try {
			// Find the transformer document
			InputStream strm = new URL(baseURL + "resourcetransformers/index.json").openStream();
			JsonArray index = JsonParser.parseString(new String(strm.readAllBytes(), "UTF-8")).getAsJsonArray();
			strm.close();

			// Load all transformers
			for (JsonElement ele : index) {
				try {
					// Find the transformer document
					strm = new URL(baseURL + "resourcetransformers/" + ele.getAsString()).openStream();
					JsonObject transformer = JsonParser.parseString(new String(strm.readAllBytes(), "UTF-8"))
							.getAsJsonObject();
					strm.close();

					// Load transformer
					importObjectsIntoMemory(transformer);
				} catch (Exception e) {
					System.err.println("Transformer failed to load: " + ele.getAsString() + " (" + fileName + "): "
							+ e.getClass().getSimpleName() + (e.getMessage() != null ? ": " + e.getMessage() : ""));
					e.printStackTrace();
				}
			}
		} catch (Exception e) {
		}
	}

	@Override
	public void prepareWorld(int levelID, List<String> ids, Player player) {
		// Find resource collection ids
		for (String id : ids) {
			NetworkedObject obj = NetworkedObjects.getObject(id);

			// Check if the object is a resource
			if (obj.primaryObjectInfo != null && resources.containsKey(Integer.toString(obj.primaryObjectInfo.defId))) {
				ResourceDefinition def = resources.get(Integer.toString(obj.primaryObjectInfo.defId));

				// Add the resource to the player's collection memory data
				if (def.lootType == ResourceType.HARVEST)
					player.account.getPlayerInventory().getInteractionMemory().prepareHarvestItem(levelID, id); // harvest
				else
					player.account.getPlayerInventory().getInteractionMemory().prepareTreasureItem(levelID, id); // treasure
			}
		}
	}

	@Override
	public boolean canHandle(Player player, String id, NetworkedObject obj) {
		// Check if the object is a resource
		if (obj.primaryObjectInfo != null && resources.containsKey(Integer.toString(obj.primaryObjectInfo.defId))) {
			ResourceDefinition def = resources.get(Integer.toString(obj.primaryObjectInfo.defId));

			// Check harvest validity
			if (def.lootType == ResourceType.HARVEST) {
				// Harvest

				// Retrieve harvest info
				long lastHarvest = player.account.getPlayerInventory().getInteractionMemory()
						.getLastHarvestTime(player.levelID, id);
				int harvested = player.account.getPlayerInventory().getInteractionMemory()
						.getLastHarvestCount(player.levelID, id);

				// Check reset
				if (def.respawnSeconds > -1 && lastHarvest + (def.respawnSeconds * 1000) < System.currentTimeMillis()) {
					player.account.getPlayerInventory().getInteractionMemory().resetHarvestCount(player.levelID, id); // reset
																														// harvest
																														// counter
					return true; // Valid as it had reseted
				}

				// Check harvest count
				if (harvested < def.interactionsBeforeDespawn) {
					return true;
				}
			} else {
				// Treasure

				// Retrieve info
				long lasUnlock = player.account.getPlayerInventory().getInteractionMemory()
						.getLastTreasureUnlockTime(player.levelID, id);

				// Check reset
				if (def.respawnSeconds > -1 && lasUnlock + (def.respawnSeconds * 1000) < System.currentTimeMillis()) {
					return true; // Valid as it had reseted
				}

				// Check unlock
				return !player.account.getPlayerInventory().getInteractionMemory()
						.hasTreasureBeenUnlocked(player.levelID, id);
			}
		}

		// Check if its treasure
		for (ArrayList<StateInfo> states : obj.stateInfo.values()) {
			for (StateInfo state : states) {
				if (!state.branches.isEmpty()) {
					for (ArrayList<StateInfo> branches : state.branches.values()) {
						for (StateInfo branch : branches)
							if (branch.command.equals("41")) {

								return true;
							}
					}
				}
			}
		}

		return false;
	}

	@Override
	public boolean shouldDestroyResource(Player player, String id, NetworkedObject obj, int stateN,
			boolean destroyOnCompletion) {
		// Check if the object is a resource
		if (obj.primaryObjectInfo != null && resources.containsKey(Integer.toString(obj.primaryObjectInfo.defId))) {
			ResourceDefinition def = resources.get(Integer.toString(obj.primaryObjectInfo.defId));

			// Check harvest
			if (def.lootType == ResourceType.HARVEST) {
				// Harvest

				// Retrieve harvest info
				int harvested = player.account.getPlayerInventory().getInteractionMemory()
						.getLastHarvestCount(player.levelID, id);

				// Check harvest count
				if (harvested + 1 < def.interactionsBeforeDespawn) {
					destroyOnCompletion = false; // Can be re-harvested
				}
			}
		}

		return destroyOnCompletion;
	}

	@Override
	public boolean handleInteractionSuccess(Player player, String id, NetworkedObject obj, int state) {
		// Check if the object is a resource
		if (obj.primaryObjectInfo != null && resources.containsKey(Integer.toString(obj.primaryObjectInfo.defId))) {
			ResourceDefinition def = resources.get(Integer.toString(obj.primaryObjectInfo.defId));

			// Check harvest
			if (def.lootType == ResourceType.HARVEST) {
				// Harvest

				// Retrieve harvest info
				int harvested = player.account.getPlayerInventory().getInteractionMemory()
						.getLastHarvestCount(player.levelID, id);

				// Check harvest count
				if (harvested < def.interactionsBeforeDespawn) {
					HarvestTable table = harvestTables.get(Integer.toString(def.lootTableId));
					if (table != null) {
						// Find reward
						HashMap<HarvestReward, Integer> options = new HashMap<HarvestReward, Integer>();
						table.rewards.forEach(t -> {
							options.put(t, t.weight);
						});
						HarvestReward reward = WeightedSelectorUtil.select(options);
						if (reward != null) {
							// Give reward
							int count = rnd.nextInt(reward.maxCount + 1);
							while (count < reward.minCount)
								count = rnd.nextInt(reward.maxCount + 1);
							player.account.getPlayerInventory().getItemAccessor(player)
									.add(Integer.parseInt(reward.itemId), count);
						}

						// Set harvest count and timestamp
						player.account.getPlayerInventory().getInteractionMemory().harvested(player.levelID, id);
						return true;
					}
				}
			} else
				return true;
		}
		return false;
	}

	@Override
	public boolean handleInteractionDataRequest(Player player, String id, NetworkedObject obj, int stateN) {
		// Check if the object is a resource
		if (obj.primaryObjectInfo != null && resources.containsKey(Integer.toString(obj.primaryObjectInfo.defId))) {
			ResourceDefinition def = resources.get(Integer.toString(obj.primaryObjectInfo.defId));

			// Check harvest
			if (def.lootType == ResourceType.LOOT) {
				// Treasure

				// Give reward
				giveLootReward(player, id, Integer.toString(def.lootTableId));

				// Set unlocked and timestamp
				player.account.getPlayerInventory().getInteractionMemory().unlocked(player.levelID, id);
				return true;
			}
		}

		// Check if its treasure
		for (ArrayList<StateInfo> states : obj.stateInfo.values()) {
			for (StateInfo state : states) {
				if (!state.branches.isEmpty()) {
					for (ArrayList<StateInfo> branches : state.branches.values()) {
						for (StateInfo branch : branches) {
							if (branch.command.equals("41") && branch.params.length > 0) {
								// It is, lets find the table

								// Give reward
								giveLootReward(player, id, branch.params[0]);

								// Set unlocked and timestamp
								player.account.getPlayerInventory().getInteractionMemory().unlocked(player.levelID, id);
								return true;
							}
						}
					}
				}
			}
		}

		return false;
	}

	private void giveLootReward(Player player, String id, String lootTableId) {
		LootTable table = lootTables.get(lootTableId);
		if (table != null) {
			// Find reward
			HashMap<LootReward, Integer> options = new HashMap<LootReward, Integer>();
			table.rewards.forEach(t -> {
				options.put(t, t.weight);
			});
			LootReward reward = WeightedSelectorUtil.select(options);
			if (reward != null) {
				// Give reward
				int count = rnd.nextInt(reward.maxCount + 1);
				while (count < reward.minCount)
					count = rnd.nextInt(reward.maxCount + 1);

				if (reward.itemId != null) {
					player.account.getPlayerInventory().getItemAccessor(player).add(Integer.parseInt(reward.itemId),
							count);
				}
				if (reward.referencedTableId != null) {
					// Give table
					giveLootReward(player, id, reward.referencedTableId);
				}
			}
		}
	}

	@Override
	public boolean initializeWorldObjects(SmartfoxClient client, String id, NetworkedObject obj) {
		Player player = (Player) client.container;

		// Prepare state field
		int pState = -1;

		// Check if the object is a resource
		if (obj.primaryObjectInfo != null && resources.containsKey(Integer.toString(obj.primaryObjectInfo.defId))) {
			ResourceDefinition def = resources.get(Integer.toString(obj.primaryObjectInfo.defId));

			// Check harvest
			if (def.lootType == ResourceType.HARVEST) {
				// Harvest

				// Retrieve harvest info
				long lastHarvest = player.account.getPlayerInventory().getInteractionMemory()
						.getLastHarvestTime(player.levelID, id);
				int harvested = player.account.getPlayerInventory().getInteractionMemory()
						.getLastHarvestCount(player.levelID, id);

				pState = 0;

				// Check reset
				if (def.respawnSeconds > -1 && lastHarvest + (def.respawnSeconds * 1000) < System.currentTimeMillis()) {
					// Reset harvest count
					player.account.getPlayerInventory().getInteractionMemory().resetHarvestCount(player.levelID, id);
				} else {
					// Check harvest count
					if (harvested >= def.interactionsBeforeDespawn) {
						pState = 2;
					}
				}
			}
		}

		// Check if the object is a resource
		if (obj.primaryObjectInfo != null && resources.containsKey(Integer.toString(obj.primaryObjectInfo.defId))) {
			ResourceDefinition def = resources.get(Integer.toString(obj.primaryObjectInfo.defId));

			// Check harvest
			if (def.lootType == ResourceType.LOOT) {
				// Treasure

				pState = 0;

				// Retrieve info
				long lasUnlock = player.account.getPlayerInventory().getInteractionMemory()
						.getLastTreasureUnlockTime(player.levelID, id);

				// Check reset and last unlock
				if (def.respawnSeconds == -1 || lasUnlock + (def.respawnSeconds * 1000) > System.currentTimeMillis()
						|| player.account.getPlayerInventory().getInteractionMemory()
								.hasTreasureBeenUnlocked(player.levelID, id)) {
					pState = 2;
				}
			}
		}

		// Check if its treasure
		for (ArrayList<StateInfo> states : obj.stateInfo.values()) {
			for (StateInfo state : states) {
				if (!state.branches.isEmpty()) {
					for (ArrayList<StateInfo> branches : state.branches.values()) {
						for (StateInfo branch : branches) {
							if (branch.command.equals("41") && branch.params.length > 0) {
								// Treasure

								pState = 0;
								ResourceDefinition def = resources.get(Integer.toString(obj.subObjectInfo.defId));

								if(def != null)
								{
									// Retrieve info
									long lasUnlock = player.account.getPlayerInventory().getInteractionMemory()
											.getLastTreasureUnlockTime(player.levelID, id);

									// Check reset and last unlock
									if (def.respawnSeconds == -1
											|| lasUnlock + (def.respawnSeconds * 1000) > System.currentTimeMillis()
											|| player.account.getPlayerInventory().getInteractionMemory()
													.hasTreasureBeenUnlocked(player.levelID, id)) {
										pState = 2;
									}
								}
							}
						}
					}
				}
			}
		}

		return pState != -1;
	}

}
