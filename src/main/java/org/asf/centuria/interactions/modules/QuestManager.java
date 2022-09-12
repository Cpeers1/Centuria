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
	public int questLock = 4585; // Scratch My Back, locked to prevent broken quests breaking the server

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
					for (QuestTask task : objective.tasks) {
						if (task.harvestTrackers.containsKey(Integer.toString(object.subObjectInfo.defId))) {
							// Harvest tracker
							String trackerID = task.harvestTrackers.get(Integer.toString(object.subObjectInfo.defId));

							// Find tracker and state
							NetworkedObject tracker = NetworkedObjects.getObject(trackerID);
							var virtualStateInfo = tracker.stateInfo.get("1").get(1).branches;

							// Build and invoke virtual NetworkedObject
							NetworkedObject virt = new NetworkedObject();
							virt.containerId = object.containerId;
							virt.localType = object.localType;
							virt.locationInfo = object.locationInfo;
							virt.objectName = object.objectName;
							virt.primaryObjectInfo = object.primaryObjectInfo;
							virt.subObjectInfo = object.subObjectInfo;
							HashMap<String, ArrayList<StateInfo>> states = new HashMap<String, ArrayList<StateInfo>>();
							ArrayList<StateInfo> virtStates = new ArrayList<StateInfo>();
							StateInfo virtState = new StateInfo();
							virtState.branches = virtualStateInfo;
							virtStates.add(virtState);
							states.put("0", virtStates);
							virt.stateInfo = states;

							handleInteractionDataRequest(player, id, virt, 1);
						}
					}
				}
			}
		}
		return false;
	}

	@Override
	public int selectInteractionState(Player player, String id, NetworkedObject object) {
		if (canHandle(player, id, object) || (player.questObjective != 0 && !player.questStarted)) {
			String activeQuest = getActiveQuest(player.account);

			if (activeQuest != null) {
				QuestDefinition quest = questDefinitions.get(activeQuest);
				// Check if its a npc and if the quest is locked
				if (quest.defID == questLock && isNPC(object) && !Centuria.debugMode) {
					return -10; // Prevent dialogue
				}
			}

			if (activeQuest != null || (player.questObjective != 0 && !player.questStarted)) {
				if (isNPC(object) && (player.questObjective != 0 || player.questStarted)) {
					return player.questObjective + 1;
				}
			}
		}
		return -1;
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

			// Quest start
			if (!player.questStarted) {
				player.questStarted = true;
				player.questProgress = 0;
				player.questObjectData.clear();

				// Update objects
				reloadObjects(player, quest);
			} else {
				// Update
				int offset = 0;
				var branch = object.stateInfo.get("0").get(0).branches.get(Integer.toString(state));
				if (branch == null) {
					branch = object.stateInfo.get("1").get(0).branches.get(Integer.toString(state));
					offset = 1;
				}
				if (branch.size() <= offset + 1)
					return true;

				// Send quest commands
				QuestCommandVTPacket cmd = new QuestCommandVTPacket();
				cmd.type = 1;
				cmd.id = branch.get(offset).actorId;
				player.questObjectData.put(cmd.id, player.questObjectData.get(cmd.id) - 1);
				cmd.params.add(Integer.toString(player.questObjectData.get(cmd.id)));
				cmd.params.add("0");
				cmd.params.add("0");
				player.client.sendPacket(cmd);

				if (branch.get(1 + offset).params.length >= 2) {
					player.questObjectData.put(cmd.id + "-id", Integer.parseInt(branch.get(1 + offset).params[2]));

					QuestCommandPacket qcmd = new QuestCommandPacket();
					qcmd.type = 20;
					qcmd.id = "-1";
					qcmd.params.add(branch.get(1 + offset).params[2]);
					player.questObjectData.put(cmd.id + "-collect",
							player.questObjectData.get(cmd.id + "-collect") + 1);
					player.questProgress++;
					qcmd.params.add(Integer.toString(player.questObjectData.get(cmd.id + "-collect")));
					player.client.sendPacket(qcmd);
				} else if (isNPC(object)) {
					player.questProgress++;
				}

				// Check task
				if (player.questObjectData.get(cmd.id) <= 0) {
					// Task finished, get task object
					NetworkedObject taskObject = NetworkedObjects.getObject(cmd.id);

					// Check if the task contains a reference
					for (var states : taskObject.stateInfo.values()) {
						for (var tState : states) {
							if (tState.command.equals("13")) {
								// Reference found check if its recognized
								if (player.questObjectData.containsKey(tState.actorId)) {
									// Lower the quest object value
									cmd = new QuestCommandVTPacket();
									cmd.type = 1;
									cmd.id = tState.actorId;
									player.questObjectData.put(tState.actorId,
											player.questObjectData.get(tState.actorId) - 1);
									cmd.params.add(Integer.toString(player.questObjectData.get(tState.actorId)));
									cmd.params.add("0");
									cmd.params.add("0");
									player.client.sendPacket(cmd);
								}
							}
						}
					}
				}

				// Check objective
				int max = 0;
				for (QuestTask task : objective.tasks) {
					max += task.targetProgress;
				}
				if (player.questProgress >= max) {
					// Next objective
					player.questProgress = 0;
					player.questObjectData.clear();
					player.questObjective++;
					objectiveID++;

					// Send packet
					QuestCommandPacket qcmd = new QuestCommandPacket();
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
						player.account.getPlayerInventory().deleteItem("quest-" + player.levelID);

						// Finish quest
						JsonObject obj = player.account.getPlayerInventory().getAccessor().findInventoryObject("311",
								22781);
						JsonObject progressionMap = obj.get("components").getAsJsonObject()
								.get("SocialExpanseLinearGenericQuestsCompletion").getAsJsonObject();
						JsonArray arr = progressionMap.get("completedQuests").getAsJsonArray();
						arr.add(quest.defID);

						// Save and create inventory update
						player.account.getPlayerInventory().setItem("311",
								player.account.getPlayerInventory().getItem("311"));
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

						// Find reward
						for (StateInfo st : object.stateInfo.get(Integer.toString(player.questObjective))) {
							st.branches.forEach((k, v) -> {
								v.forEach(st2 -> {
									if (st2.command.equals("41")) {
										// Give reward
										ResourceCollectionModule.giveLootReward(player, st2.params[0],
												object.primaryObjectInfo.type, object.primaryObjectInfo.defId);
									}
								});
							});
						}

						return true;
					}
				}
			}
		}

		return true;
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
												player.questObjectData.put(t2.actorId, Integer.parseInt(t2.params[0]));
												player.questObjectData.put(t2.actorId + "-collect", 0);
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
