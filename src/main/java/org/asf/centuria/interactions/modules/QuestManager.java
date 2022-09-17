package org.asf.centuria.interactions.modules;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import org.asf.centuria.Centuria;
import org.asf.centuria.accounts.CenturiaAccount;
import org.asf.centuria.accounts.highlevel.itemdata.item.ItemComponent;
import org.asf.centuria.entities.players.Player;
import org.asf.centuria.interactions.InteractionManager;
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
import org.asf.centuria.packets.xt.gameserver.quests.QuestCommandPacket;
import org.asf.centuria.packets.xt.gameserver.quests.QuestCommandVTPacket;
import org.asf.centuria.packets.xt.gameserver.quests.QuestGenericLinearQuestCompletePacket;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class QuestManager extends InteractionModule {

	// The quest to refuse running
	// This will be the quest after the 3rd released each week
	// Ignored in debug mode
	public int questLock = 5738; // Detective Kobold, locked to prevent broken quests breaking the server

	// TODO:
	// - tutorial quest support

	private static String firstQuest = "7537";
	private static LinkedHashMap<String, String> questMap = new LinkedHashMap<String, String>();
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
						if (tsk.has("harvestTrackers")) {
							JsonObject trackers = tsk.get("harvestTrackers").getAsJsonObject();
							trackers.keySet().forEach(t -> task.harvestTrackers.put(t, trackers.get(t).getAsString()));
						}
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
	public static String getActiveQuest(CenturiaAccount player) {
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
	public static boolean isNPC(NetworkedObject object) {
		// Check commands, command 3 is dialogue so then its a NPC
		for (ArrayList<StateInfo> states : object.stateInfo.values()) {
			if (states.stream().anyMatch(t -> t.command.equals("3"))) {
				// Make sure this is actually a NPC and not a item
				if (object.subObjectInfo != null && object.subObjectInfo.defId == 0)
					return false; // It is not
				return true;
			}
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
				// Check if its a npc and if the quest is locked
				if (quest.defID == questLock && isNPC(object) && !Centuria.debugMode) {
					return true;
				}

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

				if (object.primaryObjectInfo != null && object.primaryObjectInfo.type == 31
						&& object.subObjectInfo != null) {
					// Check for harvest trackers
					QuestObjective objective = quest.objectives.get(player.questObjective);
					for (QuestTask task : objective.tasks) {
						if (task.harvestTrackers.containsKey(Integer.toString(object.subObjectInfo.defId))) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	@Override
	public boolean handleInteractionSuccess(Player player, String id, NetworkedObject object, int state) {
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
				// Check if its a npc and if the quest is locked
				if (quest.defID == questLock && isNPC(object) && !Centuria.debugMode) {
					// Inform the user
					Centuria.systemMessage(player, "Cannot start quest\n\n" + ""
							+ "You have finished all quests that are currently in working order.\n"
							+ "If development goes well, hopefully this quest and the two following it will become playable next week!\n\n"
							+ "" + "Apologies for the inconvenience.\n" + " - Centuria Development Team", true);
					return true;
				}

				if (object.primaryObjectInfo != null && object.primaryObjectInfo.type == 31
						&& object.subObjectInfo != null) {
					// Check for harvest trackers
					QuestObjective objective = quest.objectives.get(player.questObjective);
					int taskID = 0;
					for (QuestTask task : objective.tasks) {
						if (task.harvestTrackers.containsKey(Integer.toString(object.subObjectInfo.defId))) {
							// Harvest tracker
							String trackerID = task.harvestTrackers.get(Integer.toString(object.subObjectInfo.defId));

							// Find tracker and state
							NetworkedObject tracker = NetworkedObjects.getObject(trackerID);

							// Find object references
							for (ArrayList<StateInfo> states : tracker.stateInfo.values()) {
								for (StateInfo stt : states) {
									for (ArrayList<StateInfo> branches : stt.branches.values()) {
										for (StateInfo stt2 : branches) {
											if (stt2.command.equals("13")) {
												updateCounter(stt2.actorId, task, objective, quest, player, taskID,
														player.questObjective, id, object, stt);
											}
										}
									}
								}
							}
						}
						taskID++;
					}
				}
			}
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
	public boolean handleCommand(Player player, String id, NetworkedObject object, StateInfo stateInfo,
			StateInfo parent, HashMap<String, Object> memory) {
		if (canHandle(player, id, object)) {
			String activeQuest = getActiveQuest(player.account);
			if (activeQuest != null) {
				QuestDefinition quest = questDefinitions.get(activeQuest);

				// Check if its a npc and if the quest is locked
				if (quest.defID == questLock && isNPC(object) && !Centuria.debugMode) {
					return true;
				}

				// Find quest objective
				int objectiveID = player.questObjective;
				QuestObjective objective = quest.objectives.get(objectiveID);

				// Check location
				// 0 = mugmyre
				// 1 = lakeroot
				// 2 = blood tundra
				if ((player.levelID == 2147 && quest.questLocation == 0)
						|| (player.levelID == 9687 && quest.questLocation == 1)
						|| (player.levelID == 2364 && quest.questLocation == 2)) {
					switch (stateInfo.command) {

					case "67": {
						// Quest start
						if (!player.questStarted) {
							player.questStarted = true;
							player.questProgress = 0;
							player.questObjectData.clear();

							// Update objects
							reloadObjects(player, quest);
						}
						return true;
					}

					case "13": {
						// Reference
						memory.put("ref", stateInfo.actorId);
						return true;
					}

					case "20": {
						// Quest task update
						if (player.questStarted) {
							// Find task
							int taskID = Integer.parseInt(stateInfo.params[2]);
							QuestTask task = objective.tasks.get(taskID);

							// Find counter
							String counterID = memory.getOrDefault("ref", memory).toString();

							updateCounter(counterID, task, objective, quest, player, taskID, objectiveID, id, object,
									stateInfo);
						}
						return true;
					}

					}
				}
			}
		}
		return false;
	}

	private void updateCounter(String counterID, QuestTask task, QuestObjective objective, QuestDefinition quest,
			Player player, int taskID, int objectiveID, String id, NetworkedObject object, StateInfo stateInfo) {
		// Find max and current progress
		int cur = player.questObjectData.get(counterID);
		int rem = task.targetProgress - cur;

		// Update
		rem--;
		cur++;
		player.questObjectData.put(counterID, cur);
		player.questProgress++;

		// Send packet
		QuestCommandVTPacket cmd = new QuestCommandVTPacket();
		cmd.type = 1;
		cmd.id = counterID;
		cmd.params.add(Integer.toString(rem));
		cmd.params.add("0");
		cmd.params.add("0");
		player.client.sendPacket(cmd);

		// Send counter update packet
		QuestCommandPacket qcmd = new QuestCommandPacket();
		qcmd.type = 20;
		qcmd.id = "-1";
		qcmd.params.add(Integer.toString(taskID));
		qcmd.params.add(Integer.toString(cur));
		player.client.sendPacket(qcmd);

		// Check progress
		if (rem <= 0) {
			// Done, run branches of the counter
			var obj = NetworkedObjects.getObject(counterID);
			InteractionManager.runBranches(player, obj.stateInfo,
					Integer.toString(player.states.getOrDefault(counterID, 0)), id, object, stateInfo);
		}

		// Check objective
		int max = 0;
		for (QuestTask tsk : objective.tasks) {
			max += tsk.targetProgress;
		}
		if (player.questProgress >= max) {
			// Next objective
			player.questProgress = 0;
			player.questObjectData.clear();
			player.questObjective++;
			objectiveID++;

			// Send packet
			qcmd = new QuestCommandPacket();
			qcmd.type = 81;
			qcmd.id = "-1";
			qcmd.params.add(Integer.toString(player.questObjective));
			qcmd.params.add("0");
			player.client.sendPacket(qcmd);

			// Update
			objective = quest.objectives.get(objectiveID);

			// Update objects
			reloadObjects(player, quest);

			// Check if it is the last objective
			if (objective.isLastObjective) {
				// Quest finished
				player.questStarted = false;

				// Finish quest
				JsonObject obj = player.account.getPlayerInventory().getAccessor().findInventoryObject("311", 22781);
				JsonObject progressionMap = obj.get("components").getAsJsonObject()
						.get("SocialExpanseLinearGenericQuestsCompletion").getAsJsonObject();
				JsonArray arr = progressionMap.get("completedQuests").getAsJsonArray();
				arr.add(quest.defID);

				// Save and create inventory update
				player.account.getPlayerInventory().setItem("311", player.account.getPlayerInventory().getItem("311"));
				JsonArray update = new JsonArray();
				update.add(obj);

				// Send packet
				InventoryItemPacket pkt = new InventoryItemPacket();
				pkt.item = update;
				player.client.sendPacket(pkt);

				// Send completion
				QuestGenericLinearQuestCompletePacket comp = new QuestGenericLinearQuestCompletePacket();
				comp.questID = quest.defID;
				player.client.sendPacket(comp);
			}
		}
	}

	@Override
	public int isDataRequestValid(Player player, String id, NetworkedObject object, int state) {
		if (canHandle(player, id, object)) {
			if (canHandle(player, id, object)) {
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
						if (quest.defID == questLock && isNPC(object) && !Centuria.debugMode) {
							return 0;
						}
						return 1;
					}
				}
			}
		}
		return -1;
	}

	private static void reloadObjects(Player player, QuestDefinition quest) {
		// Update objects
		String[] collections = NetworkedObjects.getCollectionIdsForOverride(Integer.toString(quest.levelOverrideID));
		for (String colID : collections) {
			ObjectCollection objs = NetworkedObjects.getObjects(colID);
			for (String key : objs.objects.keySet()) {
				NetworkedObject obj = objs.objects.get(key);

				// Update quest NPCs
				if (isNPC(obj)) {
					// Update
					QuestCommandPacket cmd = new QuestCommandPacket();
					cmd.type = 1;
					cmd.id = key;
					cmd.params.add(Integer.toString(player.questObjective + 1));
					cmd.params.add("1");
					cmd.params.add("1");
					player.client.sendPacket(cmd);

					// Add objects
					obj.stateInfo.forEach((k, v) -> {
						v.forEach(t -> {
							if (t.command.equals("3")) {
								t.branches.forEach((k2, v2) -> {
									v2.forEach(t2 -> {
										// Check if its a quest object
										if (t2.command.equals("1") && !t2.actorId.equals("0")) {
											try {
												// Save object to memory
												player.questObjectData.put(t2.actorId, 0);
											} catch (Exception e) {
												// Not a valid object
											}
										}
									});
								});
							}
						});
					});
				}
			}
		}

		// Update quest objective objects
		for (String colID : collections) {
			ObjectCollection objs = NetworkedObjects.getObjects(colID);
			for (String key : objs.objects.keySet()) {
				NetworkedObject obj = objs.objects.get(key);

				// Update quest objective objects
				if (obj.primaryObjectInfo.type == 7 && obj.primaryObjectInfo.defId == 0
						&& player.questObjectData.containsKey(key)) {
					QuestCommandVTPacket cmd = new QuestCommandVTPacket();
					cmd.type = 1;
					cmd.id = key;
					cmd.params.add(Integer.toString(player.questObjectData.get(cmd.id)));
					cmd.params.add("0");
					cmd.params.add("0");
					player.client.sendPacket(cmd);
				}
			}
		}
	}

	/**
	 * Retrieves the index of the quest
	 * 
	 * @param quest Quest ID
	 * @return Quest index or -1
	 */
	public static int getQuestPosition(String quest) {
		if (!questMap.containsKey(quest))
			return -1;

		int ind = 0;
		for (String q : questMap.keySet()) {
			if (q.equals(quest))
				return ind;
			ind++;
		}
		return -1;
	}

	/**
	 * Retrieves quests by ID
	 * 
	 * @param quest Quest ID
	 * @return QuestDefinition instance or null
	 */
	public static QuestDefinition getQuest(String quest) {
		return questDefinitions.get(quest);
	}

}
