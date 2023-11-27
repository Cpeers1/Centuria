package org.asf.centuria.interactions.modules;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import org.apache.logging.log4j.MarkerManager;
import org.asf.centuria.Centuria;
import org.asf.centuria.accounts.CenturiaAccount;
import org.asf.centuria.accounts.highlevel.itemdata.item.ItemComponent;
import org.asf.centuria.entities.players.Player;
import org.asf.centuria.interactions.InteractionManager;
import org.asf.centuria.interactions.NetworkedObjects;
import org.asf.centuria.interactions.dataobjects.NetworkedObject;
import org.asf.centuria.interactions.dataobjects.ObjectCollection;
import org.asf.centuria.interactions.dataobjects.StateInfo;
import org.asf.centuria.interactions.modules.quests.AbstractQuestPlugin;
import org.asf.centuria.interactions.modules.quests.QuestDefinition;
import org.asf.centuria.interactions.modules.quests.QuestObjective;
import org.asf.centuria.interactions.modules.quests.QuestTask;
import org.asf.centuria.interactions.modules.quests.plugins.UltimateRefreshmentPlugin;
import org.asf.centuria.modules.eventbus.EventBus;
import org.asf.centuria.modules.events.quests.QuestCompleteEvent;
import org.asf.centuria.modules.events.quests.QuestObjectiveCompletedEvent;
import org.asf.centuria.modules.events.quests.QuestStartEvent;
import org.asf.centuria.modules.events.quests.QuestTaskCompletedEvent;
import org.asf.centuria.networking.gameserver.GameServer;
import org.asf.centuria.packets.xt.gameserver.inventory.InventoryItemDownloadPacket;
import org.asf.centuria.packets.xt.gameserver.inventory.InventoryItemPacket;
import org.asf.centuria.packets.xt.gameserver.inventory.InventoryItemRemovedPacket;
import org.asf.centuria.packets.xt.gameserver.quests.QuestCommandPacket;
import org.asf.centuria.packets.xt.gameserver.quests.QuestGenericLinearQuestCompletePacket;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class QuestManager extends InteractionModule {

	// The quest to refuse running
	// This will be the quest after the 3rd released each week
	// Ignored in debug mode
	public int questLock = 15106; // Ultimate Refreshment, locked to prevent broken quests breaking the server

	private static String firstQuest = "7537";
	private static LinkedHashMap<String, String> questMap = new LinkedHashMap<String, String>();
	private static HashMap<String, QuestDefinition> questDefinitions = new HashMap<String, QuestDefinition>();
	private static ArrayList<AbstractQuestPlugin> plugins = new ArrayList<AbstractQuestPlugin>();

	/**
	 * Retrieves all registered quest plugins
	 * 
	 * @since Beta 1.8
	 * @return Array of AbstractQuestPlugin instances
	 */
	public static AbstractQuestPlugin[] getPlugins() {
		return plugins.toArray(t -> new AbstractQuestPlugin[t]);
	}

	static {
		try {
			// Load the quest map
			InputStream strm = InventoryItemDownloadPacket.class.getClassLoader()
					.getResourceAsStream("content/quests/questline.json");
			JsonObject helper = JsonParser.parseString(new String(strm.readAllBytes(), "UTF-8")).getAsJsonObject();
			JsonObject quests = helper.get("QuestMap").getAsJsonObject();
			for (String key : quests.keySet()) {
				questMap.put(key, quests.get(key).getAsString());
			}
			strm.close();

			// Load quest definitions
			strm = InventoryItemDownloadPacket.class.getClassLoader().getResourceAsStream("content/quests/quests.json");
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

		// Default plugins
		plugins.add(new UltimateRefreshmentPlugin());
	}

	/**
	 * Registers a quest plugin
	 * 
	 * @since Beta 1.8
	 * @param plugin Quest plugin to register
	 */
	public static void registerPlugin(AbstractQuestPlugin plugin) {
		if (!plugins.contains(plugin))
			plugins.add(plugin);
	}

	/**
	 * Retrieves the active quest ID of a player
	 * 
	 * @param player Account to retrieve the active quest from
	 * @return Quest defID string or null if all quests are completed
	 */
	public static String getActiveQuest(CenturiaAccount player) {
		if (player.isPlayerNew()
				|| (player.getOnlinePlayerInstance() != null && player.getOnlinePlayerInstance().levelID == 25280)) {
			return "25287";
		}
		for (int i = 0; i < 2; i++) {
			try {
				JsonObject progressionMap = player.getSaveSpecificInventory().getAccessor()
						.findInventoryObject("311", 22781).get("components").getAsJsonObject()
						.get("SocialExpanseLinearGenericQuestsCompletion").getAsJsonObject();
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
				JsonObject oldObj = player.getSaveSpecificInventory().getAccessor().removeInventoryObject("311", 22781);

				// Build entry
				JsonObject questObject = new JsonObject();
				questObject.add("completedQuests", new JsonArray());

				// Save and send to the client
				player.getSaveSpecificInventory().getAccessor().createInventoryObject("311", 22781,
						new ItemComponent("SocialExpanseLinearGenericQuestsCompletion", questObject));
				var plr = player.getOnlinePlayerInstance();
				if (plr != null) {
					if (oldObj != null) {
						InventoryItemRemovedPacket pkR = new InventoryItemRemovedPacket();
						pkR.items = new String[] { oldObj.get("id").getAsString() };
						plr.client.sendPacket(pkR);
					}
					InventoryItemPacket pk = new InventoryItemPacket();
					pk.item = player.getSaveSpecificInventory().getAccessor().findInventoryObject("311", 22781);
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
		String active = getActiveQuest(player);
		if (active == null)
			return null;
		return questMap.get(active);
	}

	@Override
	public void prepareWorld(int levelID, List<String> ids, Player player) {
		String activeQuest = getActiveQuest(player.account);
		if (activeQuest != null) {
			QuestDefinition quest = questDefinitions.get(activeQuest);

			// Check location
			// -1 = tutorial
			// 0 = mugmyre
			// 1 = lakeroot
			// 2 = blood tundra
			if ((levelID == 2147 && quest.questLocation == 0) || (levelID == 9687 && quest.questLocation == 1)
					|| (levelID == 2364 && quest.questLocation == 2)
					|| (levelID == 25280 && quest.questLocation == -1)) {
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
	public static boolean isQuestNPC(NetworkedObject object) {
		// Check commands, command 3 is dialogue so then its a NPC
		for (ArrayList<StateInfo> states : object.stateInfo.values()) {
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
			// -1 = tutorial
			// 0 = mugmyre
			// 1 = lakeroot
			// 2 = blood tundra
			if ((player.levelID == 2147 && quest.questLocation == 0)
					|| (player.levelID == 9687 && quest.questLocation == 1)
					|| (player.levelID == 2364 && quest.questLocation == 2)
					|| (player.levelID == 25280 && quest.questLocation == -1)) {
				if (player.levelID == 25280) {
					// Tutorial handler
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
			// -1 = tutorial
			// 0 = mugmyre
			// 1 = lakeroot
			// 2 = blood tundra
			if ((player.levelID == 2147 && quest.questLocation == 0)
					|| (player.levelID == 9687 && quest.questLocation == 1)
					|| (player.levelID == 2364 && quest.questLocation == 2)
					|| (player.levelID == 25280 && quest.questLocation == -1)) {
				if (player.levelID == 25280) {
					// Tutorial
					// Check if its a queenstone
					if (object.primaryObjectInfo != null && object.primaryObjectInfo.type == 31
							&& object.subObjectInfo != null && object.subObjectInfo.defId == 3432) {
						// Ignore unless there have been 3 harvests
						int harvested = player.account.getSaveSpecificInventory().getInteractionMemory()
								.getLastHarvestCount(player.levelID, id);
						if (harvested + 1 < 3) {
							return false;
						}
					}
				}

				// Check if its a npc and if the quest is locked
				if (quest.defID == questLock && isQuestNPC(object) && !Centuria.debugMode) {
					// Inform the user
					Centuria.systemMessage(player,
							"Quest not implemented yet\nRead the private message sent by the server for more info\n"
									+ "\n" + "You have finished all quests that are currently in working order.\n"
									+ "If development goes well, hopefully this quest and the two following it will become playable next week!\n\n"
									+ "" + "Apologies for the inconvenience.\n" + " - Centuria Development Team",
							true);

					// Block running the interaction
					int oState = player.states.getOrDefault(id, 0);
					player.states.put(id, -1);
					for (InteractionModule mod : InteractionManager.getModules()) {
						mod.onStateChange(player, id, NetworkedObjects.getObject(id), oState, -1);
					}
					return true;
				}

				if (object.primaryObjectInfo != null && object.primaryObjectInfo.type == 31
						&& object.subObjectInfo != null && player.questStarted) {
					// Check for harvest trackers
					QuestObjective objective = quest.objectives.get(player.questObjective);
					for (QuestTask task : objective.tasks) {
						if (task.harvestTrackers.containsKey(Integer.toString(object.subObjectInfo.defId))) {
							// Harvest tracker
							String trackerID = task.harvestTrackers.get(Integer.toString(object.subObjectInfo.defId));

							// Find tracker and state
							NetworkedObject tracker = NetworkedObjects.getObject(trackerID);
							int stateId = player.states.getOrDefault(trackerID, 1);
							if (!tracker.stateInfo.containsKey(Integer.toString(stateId)))
								stateId = Integer.parseInt(tracker.stateInfo.keySet().toArray(t -> new String[t])[0]);
							InteractionManager.runBranches(player, tracker.stateInfo, Integer.toString(stateId),
									trackerID, tracker, null);
						}
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
			StateInfo parent) {
		if (canHandle(player, id, object) && parent != null) {
			String activeQuest = getActiveQuest(player.account);
			if (activeQuest != null) {
				QuestDefinition quest = questDefinitions.get(activeQuest);

				// Check if its a npc and if the quest is locked
				if (quest.defID == questLock && isQuestNPC(object) && !Centuria.debugMode) {
					return true;
				}

				// Find quest objective
				int objectiveID = player.questObjective;
				QuestObjective objective = quest.objectives.get(objectiveID);

				// Check location
				// -1 = tutorial
				// 0 = mugmyre
				// 1 = lakeroot
				// 2 = blood tundra
				if ((player.levelID == 2147 && quest.questLocation == 0)
						|| (player.levelID == 9687 && quest.questLocation == 1)
						|| (player.levelID == 2364 && quest.questLocation == 2)
						|| (player.levelID == 25280 && quest.questLocation == -1)) {
					switch (stateInfo.command) {

					case "67": {
						// Quest start
						if (!player.questStarted) {
							player.questStarted = true;
							player.questProgress = 0;
							player.taskProgress.clear();

							// Update objects
							reloadObjects(player, quest);

							// Log start
							Centuria.logger.info(MarkerManager.getMarker("QUESTS"), "Quest started: '" + quest.name
									+ "', started by " + player.account.getDisplayName());

							// Dispatch event
							EventBus.getInstance()
									.dispatchEvent(new QuestStartEvent(((GameServer) player.client.getServer()), player,
											player.account, player.client, activeQuest, quest));

							// Call all plugins that match the quest
							for (AbstractQuestPlugin plugin : getPlugins()) {
								if (plugin.questDefID().equals(Integer.toString(quest.defID))) {
									plugin.onStartQuest(player, quest);
									plugin.onStartObjective(player, quest, objective);
									for (QuestTask task : objective.tasks)
										plugin.onStartTask(player, quest, objective, task);
								}
							}
						}
						return true;
					}

					case "20": {
						// Quest task update
						if (player.questStarted) {
							// Find task
							int taskID = Integer.parseInt(stateInfo.params[2]);
							QuestTask task = objective.tasks.get(taskID);

							// Update
							updateQuest(task, objective, quest, player, taskID, objectiveID, id, object, stateInfo);
						}
						return true;
					}

					}
				}
			}
		}
		return false;
	}

	private void updateQuest(QuestTask task, QuestObjective objective, QuestDefinition quest, Player player, int taskID,
			int objectiveID, String id, NetworkedObject object, StateInfo stateInfo) {
		// Get max
		int max = 0;
		for (QuestTask tsk : objective.tasks) {
			max += tsk.targetProgress;
		}

		// Check progress
		int taskProgress = player.taskProgress.getOrDefault(taskID, 0) + 1;
		if (taskProgress <= task.targetProgress) {
			// Progress
			player.taskProgress.put(taskID, taskProgress);
			player.questProgress++;

			// Call all plugins that match the quest
			for (AbstractQuestPlugin plugin : getPlugins()) {
				if (plugin.questDefID().equals(Integer.toString(quest.defID))) {
					plugin.onTaskProgression(player, quest, objective, task, taskProgress);
				}
			}

			// Check task completion
			if (taskProgress == task.targetProgress) {
				// Dispatch event
				EventBus.getInstance()
						.dispatchEvent(new QuestTaskCompletedEvent(((GameServer) player.client.getServer()), player,
								player.account, player.client, Integer.toString(quest.defID), quest, objective, task));

				// Call all plugins that match the quest
				for (AbstractQuestPlugin plugin : getPlugins()) {
					if (plugin.questDefID().equals(Integer.toString(quest.defID))) {
						plugin.onTaskCompleted(player, quest, objective, task);
					}
				}
			}
		}

		// Send progress update
		QuestCommandPacket qcmd = new QuestCommandPacket();
		qcmd.type = 20;
		qcmd.id = "-1";
		qcmd.params.add(Integer.toString(taskID));
		qcmd.params.add(Integer.toString(player.taskProgress.get(taskID)));
		player.client.sendPacket(qcmd);

		// Check objective
		if (player.questProgress >= max) {
			// Next objective
			player.questProgress = 0;
			player.questObjective++;
			player.taskProgress.clear();
			objectiveID++;

			// Send packet
			qcmd = new QuestCommandPacket();
			qcmd.type = 81;
			qcmd.id = "-1";
			qcmd.params.add(Integer.toString(player.questObjective));
			qcmd.params.add("0");
			player.client.sendPacket(qcmd);

			// Log completion
			Centuria.logger.info(MarkerManager.getMarker("QUESTS"), "Quest objective completed: '" + objective.title
					+ "', quest: '" + quest.name + "', completed by " + player.account.getDisplayName());

			// Update
			QuestObjective objectiveLast = objective;
			objective = quest.objectives.get(objectiveID);

			// Call all plugins that match the quest
			for (AbstractQuestPlugin plugin : getPlugins()) {
				if (plugin.questDefID().equals(Integer.toString(quest.defID))) {
					// Call complete
					plugin.onObjectiveCompleted(player, quest, objectiveLast,
							objective.isLastObjective ? null : objective);

					// Call start if needed
					if (!objective.isLastObjective) {
						// Start
						plugin.onStartObjective(player, quest, objective);
						for (QuestTask t : objective.tasks)
							plugin.onStartTask(player, quest, objective, t);
					}
				}
			}

			// Dispatch event
			EventBus.getInstance()
					.dispatchEvent(new QuestObjectiveCompletedEvent(((GameServer) player.client.getServer()), player,
							player.account, player.client, Integer.toString(quest.defID), quest, objective));

			// Update objects
			reloadObjects(player, quest);

			// Check if it is the last objective
			if (objective.isLastObjective) {
				// Quest finished
				player.questStarted = false;

				// Log completion
				Centuria.logger.info(MarkerManager.getMarker("QUESTS"),
						"Quest completed: '" + quest.name + "', completed by " + player.account.getDisplayName());

				// Dispatch event
				EventBus.getInstance().dispatchEvent(new QuestCompleteEvent(((GameServer) player.client.getServer()),
						player, player.account, player.client, Integer.toString(quest.defID), quest));

				// Call all plugins that match the quest
				for (AbstractQuestPlugin plugin : getPlugins()) {
					if (plugin.questDefID().equals(Integer.toString(quest.defID))) {
						// Call complete
						plugin.onQuestCompleted(player, quest);
					}
				}

				// Finish quest
				if (player.levelID != 25280) {
					finishQuest(player, quest.defID);
				} else {
					// Send completion
					QuestGenericLinearQuestCompletePacket comp = new QuestGenericLinearQuestCompletePacket();
					comp.questID = quest.defID;
					player.client.sendPacket(comp);
				}
			}
		}
	}

	public static boolean finishQuest(Player player, int quest) {
		JsonObject obj = player.account.getSaveSpecificInventory().getAccessor().findInventoryObject("311", 22781);
		JsonObject progressionMap = obj.get("components").getAsJsonObject()
				.get("SocialExpanseLinearGenericQuestsCompletion").getAsJsonObject();
		JsonArray arr = progressionMap.get("completedQuests").getAsJsonArray();
		arr.add(quest);

		// Save and create inventory update
		player.account.getSaveSpecificInventory().setItem("311",
				player.account.getSaveSpecificInventory().getItem("311"));
		JsonArray update = new JsonArray();
		update.add(obj);

		// Send packet
		InventoryItemPacket pkt = new InventoryItemPacket();
		pkt.item = update;
		player.client.sendPacket(pkt);

		// Send completion
		QuestGenericLinearQuestCompletePacket comp = new QuestGenericLinearQuestCompletePacket();
		comp.questID = quest;
		player.client.sendPacket(comp);
		return getActiveQuest(player.account) != null;
	}

	@Override
	public int isDataRequestValid(Player player, String id, NetworkedObject object, int state) {
		if (canHandle(player, id, object)) {
			if (canHandle(player, id, object)) {
				String activeQuest = getActiveQuest(player.account);
				if (activeQuest != null) {
					QuestDefinition quest = questDefinitions.get(activeQuest);

					// Check location
					// -1 = tutorial
					// 0 = mugmyre
					// 1 = lakeroot
					// 2 = blood tundra
					if ((player.levelID == 2147 && quest.questLocation == 0)
							|| (player.levelID == 9687 && quest.questLocation == 1)
							|| (player.levelID == 2364 && quest.questLocation == 2)
							|| (player.levelID == 25280 && quest.questLocation == -1)) {
						if (quest.defID == questLock && isQuestNPC(object) && !Centuria.debugMode) {
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
				if (isQuestNPC(obj)) {
					// Update
					QuestCommandPacket cmd = new QuestCommandPacket();
					cmd.type = 1;
					cmd.id = key;
					cmd.params.add(Integer.toString(player.questObjective));
					cmd.params.add("1");
					cmd.params.add("1");
					player.client.sendPacket(cmd);
				}
			}
		}
	}

	@Override
	public void onStateChange(Player player, String objectID, NetworkedObject object, int oldState, int newState) {
		// Find quest
		String questID = getActiveQuest(player.account);
		if (questID != null) {
			// Get quest object
			QuestDefinition quest = getQuest(questID);

			// Check object
			String[] collections = NetworkedObjects
					.getCollectionIdsForOverride(Integer.toString(quest.levelOverrideID));
			for (String colID : collections) {
				ObjectCollection objs = NetworkedObjects.getObjects(colID);
				for (String uuid : objs.objects.keySet()) {
					if (uuid.equals(objectID)) {
						// Valid
						// Call all plugins that match the quest
						for (AbstractQuestPlugin plugin : getPlugins()) {
							if (plugin.questDefID().equals(questID))
								plugin.onStateChange(player, quest, objectID, object, oldState, newState);
						}
						break;
					}
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
