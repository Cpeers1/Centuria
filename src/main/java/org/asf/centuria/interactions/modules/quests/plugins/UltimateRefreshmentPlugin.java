package org.asf.centuria.interactions.modules.quests.plugins;

import org.asf.centuria.Centuria;
import org.asf.centuria.entities.players.Player;
import org.asf.centuria.interactions.NetworkedObjects;
import org.asf.centuria.interactions.dataobjects.NetworkedObject;
import org.asf.centuria.interactions.modules.quests.AbstractQuestPlugin;
import org.asf.centuria.interactions.modules.quests.QuestDefinition;
import org.asf.centuria.interactions.modules.quests.plugins.constants.UltimateRefreshmentConstants;
import org.asf.centuria.interactions.modules.quests.plugins.questobjects.UltimateRefreshmentPlayerVars;
import org.asf.connective.tasks.AsyncTaskManager;

/**
 * 
 * Plugin for the quest "Ultimate Refreshment"
 * 
 * @author Sky Swimmer
 * 
 */
public class UltimateRefreshmentPlugin extends AbstractQuestPlugin {

	@Override
	public String questDefID() {
		return "15106";
	}

	public UltimateRefreshmentPlugin() {
		// Timer for all players
		AsyncTaskManager.runAsync(() -> {
			while (true) {
				// Go through players
				for (Player player : Centuria.gameServer.getPlayers()) {
					// Check variables
					UltimateRefreshmentPlayerVars vars = player.getObject(UltimateRefreshmentPlayerVars.class);
					if (vars == null
							|| !player.states.containsKey(UltimateRefreshmentConstants.QUEST_WON_VARIABLE_OBJECT_ID)
							|| !vars.timerStarted)
						continue;

					// Check timer
					if (System.currentTimeMillis() - vars.timerStartTime >= (5 * 60 * 60 * 1000) - 1000) {
						// Fail
						failPlayer(player);
						vars.timerStarted = false;
					}
				}

				// Wait
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
				}
			}
		});
	}

	public void failPlayer(Player player) {
		// Player failed

		// Get object
		String objectID = UltimateRefreshmentConstants.QUEST_TIMER_OBJECT_ID;
		NetworkedObject object = NetworkedObjects.getObject(objectID);

		// Schedule 'fail' actions for when the client requests failure d
		player.stateObjects.put(objectID, object.stateInfo.get("1").get(0).branches.get("0"));
	}

	@Override
	public void onStateChange(Player player, QuestDefinition quest, String objectID, NetworkedObject object,
			int oldState, int newState) {
		// Check object
		if (objectID.equals(UltimateRefreshmentConstants.QUEST_TIMER_OBJECT_ID)) {
			// Quest timer command

			// Check requested state
			if (newState == 1) {
				// Check won variable state
				int wonVarState = getState(player, UltimateRefreshmentConstants.QUEST_WON_VARIABLE_OBJECT_ID);
				if (wonVarState < 1) {
					// Restart timer call
					UltimateRefreshmentPlayerVars vars = player.getObject(UltimateRefreshmentPlayerVars.class);

					// Check if the quest won state is present, if not, even if vars is not null,
					// its a world switch and needs to be restarted
					if (vars == null
							|| !player.states.containsKey(UltimateRefreshmentConstants.QUEST_WON_VARIABLE_OBJECT_ID)) {
						// Create variables and mark started
						vars = new UltimateRefreshmentPlayerVars();
						player.addObject(vars);
						player.states.put(UltimateRefreshmentConstants.QUEST_WON_VARIABLE_OBJECT_ID, 0);
					} else if (vars.timerStarted
							&& System.currentTimeMillis() - vars.timerStartTime < (5 * 60 * 60 * 1000)) {
						// Ignore
						return;
					}

					// Restart timer
					vars.timerStartTime = System.currentTimeMillis();
					vars.timerStarted = true;

					// Make client start timer
					// Command 3 in this case runs dialogue after 5 minutes unless the WON variable
					// changes to true while the timer is running
					runState(player, objectID, 1);
				}
			}
		} else if (objectID.equals(UltimateRefreshmentConstants.QUEST_WON_VARIABLE_OBJECT_ID)) {
			// Won variable

			// Check requested state
			if (newState == 1) {
				// Check old variable state, if its already 1, someone is attempting to exploit
				if (oldState != 1) {
					// Successfully finished in time

					// End timer call
					UltimateRefreshmentPlayerVars vars = player.getObject(UltimateRefreshmentPlayerVars.class);
					if (vars == null || !player.states.containsKey(objectID) || !vars.timerStarted)
						return;

					// End timer
					vars.timerStarted = false;

					// Use 'won' actions for when the client requests won dialogue
					objectID = UltimateRefreshmentConstants.QUEST_TIMER_OBJECT_ID;
					object = NetworkedObjects.getObject(objectID);
					player.stateObjects.put(objectID, object.stateInfo.get("1").get(0).branches.get("1"));
				}
			}
		}
	}

}
