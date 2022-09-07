package org.asf.centuria.interactions.modules;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.asf.centuria.accounts.CenturiaAccount;
import org.asf.centuria.accounts.highlevel.itemdata.item.ItemComponent;
import org.asf.centuria.entities.players.Player;
import org.asf.centuria.interactions.NetworkedObjects;
import org.asf.centuria.interactions.dataobjects.NetworkedObject;
import org.asf.centuria.interactions.dataobjects.ObjectCollection;
import org.asf.centuria.interactions.dataobjects.StateInfo;
import org.asf.centuria.interactions.modules.quests.QuestDefinition;
import org.asf.centuria.interactions.modules.quests.QuestObjective;
import org.asf.centuria.interactions.modules.quests.QuestTask;
import org.asf.centuria.packets.xt.gameserver.inventory.InventoryItemDownloadPacket;
import org.asf.centuria.packets.xt.gameserver.inventory.InventoryItemPacket;
import org.asf.centuria.packets.xt.gameserver.inventory.InventoryItemRemovedPacket;
import org.asf.centuria.packets.xt.gameserver.quests.QuestCommand;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class QuestManager extends InteractionModule {

	private static String firstQuest = "7537";
	private static HashMap<String, String> questMap = new HashMap<String, String>();
	private static HashMap<String, QuestDefinition> questDefinitions = new HashMap<String, QuestDefinition>();
	static {
		try {
			// Load the quest map
			InputStream strm = InventoryItemDownloadPacket.class.getClassLoader().getResourceAsStream("questline.json");
			JsonObject helper = JsonParser.parseString(new String(strm.readAllBytes(), "UTF-8")).getAsJsonObject();
			JsonObject quests = helper.get("QuestMap").getAsJsonObject();
			for (String key : quests.keySet()) {
				questMap.put(key, quests.get(key).getAsString());
			}
			strm.close();

			// Load quest definitions
			strm = InventoryItemDownloadPacket.class.getClassLoader().getResourceAsStream("quests.json");
			helper = JsonParser.parseString(new String(strm.readAllBytes(), "UTF-8")).getAsJsonObject();
			quests = helper.get("Quests").getAsJsonObject();
			for (String key : quests.keySet()) {
				JsonObject def = quests.get(key).getAsJsonObject();
				QuestDefinition quest = new QuestDefinition();
				quest.defID = def.get("defID").getAsInt();
				quest.name = def.get("name").getAsString();
				quest.levelOverrideID = def.get("levelOverrideID").getAsInt();
				quest.questLocation = def.get("questLocation").getAsInt();

				JsonArray objectives = def.get("objectives").getAsJsonArray();
				for (JsonElement ele : objectives) {
					JsonObject obj = ele.getAsJsonObject();
					QuestObjective objective = new QuestObjective();
					objective.title = obj.get("title").getAsString();
					objective.isLastObjective = obj.get("isLastObjective").getAsBoolean();
					JsonArray tasks = obj.get("tasks").getAsJsonArray();
					for (JsonElement taskEle : tasks) {
						JsonObject tsk = taskEle.getAsJsonObject();
						QuestTask task = new QuestTask();
						task.targetProgress = tsk.get("targetProgress").getAsInt();
						objective.tasks.add(task);
					}
					quest.objectives.add(objective);
				}
				questDefinitions.put(key, quest);
			}
			strm.close();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Retrieves the active quest ID of a player
	 * 
	 * @param player Account to retrieve the active quest from
	 * @return Quest defID string or null if all quests are completed
	 */
	public String getActiveQuest(CenturiaAccount player) {
		for (int i = 0; i < 2; i++) {
			try {
				JsonObject progressionMap = player.getPlayerInventory().getAccessor().findInventoryObject("311", 22781)
						.get("components").getAsJsonObject().get("SocialExpanseLinearGenericQuestsCompletion")
						.getAsJsonObject();
				JsonArray arr = progressionMap.get("completedQuests").getAsJsonArray();
				ArrayList<String> completedQuests = new ArrayList<String>();
				arr.forEach(t -> completedQuests.add(t.getAsString()));
				String quest = firstQuest;
				while (quest != null) {
					if (!completedQuests.contains(quest))
						return quest;

					quest = questMap.get(quest);
				}
				return null;
			} catch (Exception e) {
				// Damaged container, lets reset it
				JsonObject oldObj = player.getPlayerInventory().getAccessor().removeInventoryObject("311", 22781);

				// Build entry
				JsonObject questObject = new JsonObject();
				questObject.add("completedQuests", new JsonArray());

				// Save and send to the client
				player.getPlayerInventory().getAccessor().createInventoryObject("311", 22781,
						new ItemComponent("SocialExpanseLinearGenericQuestsCompletion", questObject));
				var plr = player.getOnlinePlayerInstance();
				if (plr != null) {
					if (oldObj != null) {
						InventoryItemRemovedPacket pkR = new InventoryItemRemovedPacket();
						pkR.items = new String[] { oldObj.get("id").getAsString() };
						plr.client.sendPacket(pkR);
					}
					InventoryItemPacket pk = new InventoryItemPacket();
					pk.item = player.getPlayerInventory().getAccessor().findInventoryObject("311", 22781);
					plr.client.sendPacket(pk);
				}
			}
		}
		return null;
	}

	/**
	 * Retrieves the next quest ID of a player
	 * 
	 * @param player Account to retrieve the next quest ID from
	 * @return Quest defID string or null if all quests are completed after the
	 *         current one
	 */
	public String getNextQuest(CenturiaAccount player) {
		return questMap.get(getActiveQuest(player));
	}

	@Override
	public void prepareWorld(int levelID, List<String> ids, Player player) {
		String activeQuest = getActiveQuest(player.account);
		if (activeQuest != null) {
			QuestDefinition quest = questDefinitions.get(activeQuest);

			// Check location
			// 0 = mugmyre
			// 1 = lakeroot
			// 2 = blood tundra
			if ((levelID == 2147 && quest.questLocation == 0) || (levelID == 9687 && quest.questLocation == 1)
					|| (levelID == 2364 && quest.questLocation == 2)) {
				// Load objects
				String[] collections = NetworkedObjects
						.getCollectionIdsForOverride(Integer.toString(quest.levelOverrideID));
				for (String id : collections) {
					ObjectCollection objs = NetworkedObjects.getObjects(id);
					for (String uuid : objs.objects.keySet()) {
						ids.add(uuid);
					}
				}
			}
		}
	}

	// For checking if a object is a NPC
	public boolean isNPC(NetworkedObject object) {
		// Check commands, command 3 is dialogue so then its a NPC
		for (ArrayList<StateInfo> states : object.stateInfo.values()) {
			if (states.stream().anyMatch(t -> t.command.equals("67")))
				return true;
			for (StateInfo state : states) {
				for (ArrayList<StateInfo> sstates : state.branches.values()) {
					for (StateInfo sstate : sstates) {
						if (sstate.command.equals("67"))
							return true;
					}
				}
			}
		}
		return false;
	}

	@Override
	public boolean canHandle(Player player, String id, NetworkedObject object) {
		String activeQuest = getActiveQuest(player.account);
		if (activeQuest != null) {
			QuestDefinition quest = questDefinitions.get(activeQuest);

			// Check location
			// 0 = mugmyre
			// 1 = lakeroot
			// 2 = blood tundra
			if ((player.levelID == 2147 && quest.questLocation == 0)
					|| (player.levelID == 9687 && quest.questLocation == 1)
					|| (player.levelID == 2364 && quest.questLocation == 2)) {
				// Load objects
				String[] collections = NetworkedObjects
						.getCollectionIdsForOverride(Integer.toString(quest.levelOverrideID));
				for (String colID : collections) {
					ObjectCollection objs = NetworkedObjects.getObjects(colID);
					for (String uuid : objs.objects.keySet()) {
						if (uuid.equals(id))
							return true;
					}
				}
			}
		}
		return false;
	}

	@Override
	public boolean handleInteractionSuccess(Player player, String id, NetworkedObject object, int state) {
		if (canHandle(player, id, object)) {
			id = id;
		}
		return false;
	}

	@Override
	public boolean shouldDestroyResource(Player player, String id, NetworkedObject object, int state,
			boolean destroyOnCompletion) {
		if (canHandle(player, id, object))
			return true;
		return destroyOnCompletion;
	}

	@Override
	public boolean handleInteractionDataRequest(Player player, String id, NetworkedObject object, int state) {
		if (canHandle(player, id, object)) {

			String activeQuest = getActiveQuest(player.account);
			if (activeQuest != null) {
				QuestDefinition quest = questDefinitions.get(activeQuest);

				// Find quest objective
				int objectiveID = player.questObjective;
				int taskID = player.questTask;

				QuestObjective objective = quest.objectives.get(objectiveID - 1);
				QuestTask task = objective.tasks.get(taskID - 1);

				// Update quest NPCs
				String[] collections = NetworkedObjects
						.getCollectionIdsForOverride(Integer.toString(quest.levelOverrideID));
				for (String colID : collections) {
					ObjectCollection objs = NetworkedObjects.getObjects(colID);
					for (String key : objs.objects.keySet()) {
						NetworkedObject obj = objs.objects.get(key);
						if (isNPC(obj)) {
							// Update
							QuestCommand cmd = new QuestCommand();
							cmd.type = 1;
							cmd.id = key;
							cmd.params.add("1");
							cmd.params.add(Integer.toString(taskID));
							cmd.params.add(Integer.toString(player.questStage++));
							player.client.sendPacket(cmd);
						}
					}
				}

				id = id;
			}
		}
		return false;
	}

}
