package org.asf.centuria.interactions.modules.linearobjects;

import org.apache.logging.log4j.MarkerManager;
import org.asf.centuria.Centuria;
import org.asf.centuria.entities.players.Player;
import org.asf.centuria.interactions.InteractionManager;
import org.asf.centuria.interactions.NetworkedObjects;
import org.asf.centuria.interactions.dataobjects.NetworkedObject;
import org.asf.centuria.interactions.dataobjects.StateInfo;
import org.asf.centuria.interactions.modules.InteractionModule;
import org.asf.centuria.packets.xt.gameserver.quests.QuestCommandPacket;

import java.util.ArrayList;
import java.util.List;

public class LinearObjectHandler extends InteractionModule {

	// TODO: rotation spawning (og game spawning mechanics)
	// TODO: new rewards for chests etc

	@Override
	public void prepareWorld(int levelID, List<String> ids, Player player) {
	}

	@Override
	public boolean canHandle(Player player, String id, NetworkedObject object) {
		return player.groupOjects.stream().anyMatch(t -> t.id.equals(id)); // Check if its a valid object
	}

	@Override
	public boolean handleCommand(Player player, String id, NetworkedObject object, StateInfo st, StateInfo parent) {
		if (canHandle(player, id, object)) {
			// Log commands
			String args = "";
			for (String arg : st.params) {
				args += ", " + arg;
			}
			if (!args.isEmpty())
				args = args.substring(2);
			// Handle commands
			switch (st.command) {
			case "84": {
				String command = st.params[0];
				switch (command) {
				case "1": {
					// Save
					Centuria.logger.debug("Group object collect: " + st.params[2]);
					InteractionManager.getActiveSpawnBehaviour().onCollect(player, st.params[2]);

					// Log
					Centuria.logger.info(MarkerManager.getMarker("EXPANSE OBJECTS"),
							"Player " + player.account.getDisplayName() + " unlocked object " + object.objectName + " ("
									+ id + ")");
					break;
				}
				case "3": {
					// Use (eg. lockpicks breaking)
					Centuria.logger.debug("Group object use: " + st.params[1]);
					switch (st.params[1]) {
					case "2": {
						Centuria.logger.debug("Group object use lockpick");
						player.account.getSaveSpecificInventory().getCurrencyAccessor().removeLockpicks(player.client,
								1);

						// Log
						Centuria.logger.info(MarkerManager.getMarker("EXPANSE OBJECTS"),
								"Player " + player.account.getDisplayName()
										+ " broke a lockpick while interacting with a locked chest.");
						break;
					}
					default: {
						Centuria.logger.debug("Unhandled group object use: " + st.params[1]);
						break;
					}
					}
					break;
				}
				default: {
					Centuria.logger.debug("Unhandled group object SUB command: " + id + ", command: " + st.command
							+ ", args: " + args + " (subcommand: " + command + ")");
					break;
				}
				}
				break;
			}
			case "35": {
				// Branches & progress
				Centuria.logger.debug("Call branches: " + st.params[1]);
				NetworkedObject obj = NetworkedObjects.getObject(st.params[1]);
				InteractionManager.runBranches(player, st.branches, "0", id, object, st);

				// Progress
				int stateId = player.states.getOrDefault(st.params[1], 1) + 1;
				if (obj.stateInfo.containsKey(Integer.toString(stateId - 1))) {
					// Run states
					InteractionManager.runBranches(player, obj.stateInfo, Integer.toString(stateId - 1), st.params[1],
							obj, st);
				}

				if (obj.stateInfo.containsKey(Integer.toString(stateId))
						|| obj.stateInfo.containsKey(Integer.toString(stateId - 1))) {
					// Update
					player.states.put(st.params[1], stateId);

					// Send state info
					QuestCommandPacket qcmd = new QuestCommandPacket();
					qcmd.id = st.params[1];
					qcmd.type = 1;
					qcmd.params.add(Integer.toString(stateId - 1));
					qcmd.params.add("0");
					qcmd.params.add("0");
					player.client.sendPacket(qcmd);
				}
				break;
			}
			default: {
				Centuria.logger.debug("Unhandled group object interaction command: " + id + ", command: " + st.command
						+ ", args: " + args);
				break;
			}
			}
			return true;
		}
		return false;
	}

	@Override
	public boolean handleInteractionSuccess(Player player, String id, NetworkedObject object, int state) {
		Centuria.logger.debug("Group object interaction success: " + id + ", state: " + state);

		// Yeah ik it might sound like its better
		// to use a shared state processor but... i dont
		// want to risk hackers duplicating linear interaction
		// rewards. This way there is no way for it to happen.

		// Find state
		int nState = player.states.getOrDefault(id, state);
		if (object.stateInfo.containsKey(Integer.toString(nState)) || nState == -1)
			state = nState;

		// Find states to run
		ArrayList<StateInfo> states = object.stateInfo.get(Integer.toString(state));
		if (states == null)
			return false;
		player.stateObjects.put(id, states);
		for (StateInfo st : states) {
			// Log commands
			String args = "";
			for (String arg : st.params) {
				args += ", " + arg;
			}
			if (!args.isEmpty())
				args = args.substring(2);
			Centuria.logger.debug("Group object interaction command: " + id + ", state: " + state + ", command: "
					+ st.command + ", args: " + args);
			handleCommand(player, id, object, st, null);
		}

		return false;
	}

}
