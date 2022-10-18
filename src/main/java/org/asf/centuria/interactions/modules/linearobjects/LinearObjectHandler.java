package org.asf.centuria.interactions.modules.linearobjects;

import org.asf.centuria.Centuria;
import org.asf.centuria.data.XtWriter;
import org.asf.centuria.entities.players.Player;
import org.asf.centuria.interactions.InteractionManager;
import org.asf.centuria.interactions.dataobjects.NetworkedObject;
import org.asf.centuria.interactions.dataobjects.StateInfo;
import org.asf.centuria.interactions.modules.InteractionModule;

import java.util.ArrayList;
import java.util.List;

public class LinearObjectHandler extends InteractionModule {

	// TODO: rotation spawning (og game spawning mechanics)
	// TODO: random spawning (emuferal spawning mechanics, from server config)
	// TODO: rotation locks for respawn (prevent respawning til next rotation)
	// TODO: locked chests rewards
	// TODO: dig spots
	// TODO: waystones
	// TODO: enigmas?

	@Override
	public void prepareWorld(int levelID, List<String> ids, Player player) {
	}

	@Override
	public boolean canHandle(Player player, String id, NetworkedObject object) {
		return player.groupOjects.stream().anyMatch(t -> t.id.equals(id)); // Check if its a valid object
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
			Centuria.logger.debug("Group object interaction command: " + id + ", state: " + state + ", command: " + st.command + ", args: " + args);

			// Handle commands
			switch (st.command) {
				case "84": {
					String command = st.params[0];
					switch (command) {
						case "1": {
							// Save
							Centuria.logger.debug("Group object collect: " + st.params[2]);
							InteractionManager.getActiveSpawnBehaviour().onCollect(player, st.params[2]);
							break;
						}
						case "3": {
							// Use (eg. lockpicks breaking)
							Centuria.logger.debug("Group object use: " + st.params[1]);
							switch (st.params[1]) {
								case "2": {
									Centuria.logger.debug("Group object use lockpick");
									player.account.getPlayerInventory().getCurrencyAccessor().removeLockpicks(player.client, 1);
									break;
								}
								default:{
									Centuria.logger.debug("Unhandled group object use: " + st.params[1]);
									break;
								}
							}
							break;
						}
						default: {
							Centuria.logger.debug("Unhandled group object SUB command: " + id + ", state: " + state + ", command: " + st.command + ", args: " + args + " (subcommand: " + command + ")");
							break;
						}
					}
					break;
				}
				default: {
					Centuria.logger.debug("Unhandled group object interaction command: " + id + ", state: " + state + ", command: " + st.command + ", args: " + args);
					break;
				}
			}
		}

		return false;
	}

}
