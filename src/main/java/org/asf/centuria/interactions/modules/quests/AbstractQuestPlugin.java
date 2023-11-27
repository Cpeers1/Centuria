package org.asf.centuria.interactions.modules.quests;

import org.asf.centuria.entities.players.Player;
import org.asf.centuria.interactions.InteractionManager;
import org.asf.centuria.interactions.NetworkedObjects;
import org.asf.centuria.interactions.dataobjects.NetworkedObject;
import org.asf.centuria.interactions.modules.InteractionModule;
import org.asf.centuria.packets.xt.gameserver.quests.QuestCommandPacket;
import org.asf.centuria.packets.xt.gameserver.quests.QuestCommandVTPacket;

/**
 * 
 * Quest plugin system, server-sided quest logic
 * 
 * @since Beta 1.8
 * @author Sky Swimmer
 * 
 */
public abstract class AbstractQuestPlugin {

	/**
	 * Defines the quest def ID this plugin is applied to
	 * 
	 * @return Quest def ID
	 */
	public abstract String questDefID();

	/**
	 * Retrieves the state of networked objects
	 * 
	 * @param player   Player instance
	 * @param objectID Object ID
	 * @return Object state or -1
	 */
	public int getState(Player player, String objectID) {
		if (!player.states.containsKey(objectID))
			return -1;
		return player.states.get(objectID);
	}

	/**
	 * Updates the state of a specific networked object
	 * 
	 * @param player   Player instance
	 * @param objectID Object ID
	 * @param newState New state
	 */
	public void setState(Player player, String objectID, int newState) {
		NetworkedObject obj = getObject(objectID);
		if (obj != null) {
			// Check validity
			if (obj.primaryObjectInfo.type == 7) {
				// Variable
				int oState = player.states.getOrDefault(objectID, 0);
				player.states.put(objectID, newState);
				for (InteractionModule mod : InteractionManager.getModules()) {
					mod.onStateChange(player, objectID, obj, oState, newState);
				}

				// Build quest command
				QuestCommandVTPacket packet = new QuestCommandVTPacket();
				packet.id = objectID;
				packet.type = 1;
				packet.params.add(Integer.toString(newState));
				player.client.sendPacket(packet);
			} else if (obj.stateInfo.containsKey(Integer.toString(newState))) {
				// Regular
				int oState = player.states.getOrDefault(objectID, 0);
				player.states.put(objectID, newState);
				for (InteractionModule mod : InteractionManager.getModules()) {
					mod.onStateChange(player, objectID, obj, oState, newState);
				}

				// Build quest command
				QuestCommandPacket packet = new QuestCommandPacket();
				packet.id = objectID;
				packet.type = 1;
				packet.params.add(Integer.toString(newState));
				player.client.sendPacket(packet);
			}
		}
	}

	/**
	 * Runs object states
	 * 
	 * @param player   Player instance
	 * @param objectID Object ID
	 * @param state    Object state to run
	 */
	public void runState(Player player, String objectID, int state) {
		runState(player, objectID, state, false);
	}

	/**
	 * Runs object states
	 * 
	 * @param player          Player instance
	 * @param objectID        Object ID
	 * @param state           Object state to run
	 * @param branchElevation True for branch-level permission elevation, false for
	 *                        regular state processing
	 */
	public void runState(Player player, String objectID, int state, boolean branchElevation) {
		NetworkedObject obj = getObject(objectID);
		if (obj != null && obj.stateInfo.containsKey(Integer.toString(state))) {
			// Run states
			if (!branchElevation)
				InteractionManager.runStates(obj.stateInfo.get(Integer.toString(state)), player, obj, objectID);
			else
				InteractionManager.runBranches(player, obj.stateInfo, Integer.toString(state), objectID, obj, null);
		}
	}

	/**
	 * Retrieves networked objects by ID
	 * 
	 * @param objectID Object ID
	 * @return NetworkedObject instance or null
	 */
	public NetworkedObject getObject(String objectID) {
		return NetworkedObjects.getObject(objectID);
	}

	/**
	 * Called when the quest is started
	 * 
	 * @param player Player instance
	 * @param quest  Quest definition that was started
	 */
	public void onStartQuest(Player player, QuestDefinition quest) {
	}

	/**
	 * Called when the quest objective is started
	 * 
	 * @param player    Player instance
	 * @param quest     Quest definition
	 * @param objective Quest objective that was started
	 */
	public void onStartObjective(Player player, QuestDefinition quest, QuestObjective objective) {
	}

	/**
	 * Called when the quest task is started
	 * 
	 * @param player    Player instance
	 * @param quest     Quest definition
	 * @param objective Quest objective
	 * @param task      Quest task that was started
	 */
	public void onStartTask(Player player, QuestDefinition quest, QuestObjective objective, QuestTask task) {
	}

	/**
	 * Called when the quest task is progressed
	 * 
	 * @param player    Player instance
	 * @param quest     Quest definition
	 * @param objective Current objective
	 * @param task      Current task
	 * @param progress  Task progress
	 */
	public void onTaskProgression(Player player, QuestDefinition quest, QuestObjective objective, QuestTask task,
			int progress) {
	}

	/**
	 * Called when the quest task is completed
	 * 
	 * @param player    Player instance
	 * @param quest     Quest definition
	 * @param objective Current objective
	 * @param task      Task that was completed
	 */
	public void onTaskCompleted(Player player, QuestDefinition quest, QuestObjective objective, QuestTask task) {
	}

	/**
	 * Called when the quest objective is completed
	 * 
	 * @param player        Player instance
	 * @param quest         Quest definition
	 * @param objective     Objective that was completed
	 * @param nextObjective Next objective or null if it was the last
	 */
	public void onObjectiveCompleted(Player player, QuestDefinition quest, QuestObjective objective,
			QuestObjective nextObjective) {
	}

	/**
	 * Called when the quest is completed
	 * 
	 * @param player Player instance
	 * @param quest  Quest definition that was completed
	 */
	public void onQuestCompleted(Player player, QuestDefinition quest) {
	}

	/**
	 * Called when the state of a quest object is changed
	 * 
	 * @param player   Player instance
	 * @param quest    Quest definition
	 * @param objectID ID of the object
	 * @param object   Object that the state was changed of
	 * @param oldState Old object state
	 * @param newState New object state
	 */
	public void onStateChange(Player player, QuestDefinition quest, String objectID, NetworkedObject object,
			int oldState, int newState) {
	}

}
