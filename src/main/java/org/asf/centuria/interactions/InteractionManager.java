package org.asf.centuria.interactions;

import java.util.ArrayList;
import java.util.HashMap;
import org.apache.logging.log4j.MarkerManager;
import org.asf.centuria.Centuria;
import org.asf.centuria.data.XtWriter;
import org.asf.centuria.entities.players.Player;
import org.asf.centuria.interactions.dataobjects.NetworkedObject;
import org.asf.centuria.interactions.dataobjects.StateInfo;
import org.asf.centuria.interactions.modules.InspirationCollectionModule;
import org.asf.centuria.interactions.modules.InteractionModule;
import org.asf.centuria.interactions.modules.QuestManager;
import org.asf.centuria.interactions.modules.ResourceCollectionModule;
import org.asf.centuria.interactions.modules.ShopkeeperModule;
import org.asf.centuria.networking.smartfox.SmartfoxClient;

public class InteractionManager {

	private static ArrayList<InteractionModule> modules = new ArrayList<InteractionModule>();

	static {
		// Add modules
		// TODO: quest module (needs to be loaded BEFORE resource collection)
		modules.add(new QuestManager());
		modules.add(new ShopkeeperModule());
		modules.add(new InspirationCollectionModule());
		modules.add(new ResourceCollectionModule());
	}

	/**
	 * Initializes the interactions for a specific level
	 * 
	 * @param player  Player to send the packets to
	 * @param levelID Level to find interactions for
	 */
	public static void initInteractionsFor(Player player, int levelID) {
		// Load object ids
		NetworkedObjects.init();
		ArrayList<String> ids = new ArrayList<String>();

		// Find level objects
		for (String id : NetworkedObjects.getCollectionIdsForLevel(Integer.toString(levelID))) {
			NetworkedObjects.getObjects(id).objects.keySet().forEach(t -> ids.add(t));
		}

		// Initialize modules
		modules.forEach(t -> t.prepareWorld(levelID, ids, player));

		// Initialize objects
		initializeNetworkedObjects(player.client, ids.toArray(t -> new String[t]));
		player.interactions.addAll(ids);
	}

	/**
	 * Initializes networked objects (eg. npcs)
	 * 
	 * @param client Client to send the packets to
	 * @param ids    Object UUIDs to initialize
	 */
	public static void initializeNetworkedObjects(SmartfoxClient client, String[] ids) {
		HashMap<String, NetworkedObject> data = new HashMap<String, NetworkedObject>();

		// Add objects
		for (String id : ids) {
			data.put(id, NetworkedObjects.getObject(id));
		}

		// Send init packet
		XtWriter packet = new XtWriter();
		packet.writeString("qs");
		packet.writeString("-1"); // data prefix
		packet.writeString("-1036"); // unknown
		packet.writeString("24"); // unknown
		packet.writeInt(data.size()); // count
		for (String id : data.keySet()) {
			NetworkedObject ent = data.get(id);
			packet.writeString(id);
			packet.writeInt(ent.primaryObjectInfo.type);
			packet.writeInt(ent.primaryObjectInfo.defId);
		}
		packet.writeString(""); // data suffix
		client.sendPacket(packet.encode());

		// Send qcmd packets
		for (String id : data.keySet()) {
			NetworkedObject ent = data.get(id);
			if (ent.stateInfo.size() == 0) {
				// Set states
				packet = new XtWriter();
				packet.writeString("qcmd");
				packet.writeString("-1"); // data prefix
				packet.writeString("1"); // command
				packet.writeString(id); // interaction ID
				packet.writeString("0"); // unknown
				packet.writeString("0"); // unknown
				packet.writeString("1"); // unknown
				packet.writeString(""); // data suffix
				client.sendPacket(packet.encode());
			}
		}

		// Initialize objects
		for (String id : data.keySet()) {
			NetworkedObject ent = data.get(id);
			boolean handled = false;
			for (InteractionModule mod : modules) {
				if (mod.initializeWorldObjects(client, id, ent)) {
					handled = true;
					break;
				}
			}
			if (handled)
				continue;

			// Fallback handler
			// Spawn object
			XtWriter wr = new XtWriter();
			wr.writeString("oi");
			wr.writeInt(-1); // data prefix

			// Object creation parameters
			wr.writeString(id); // World object ID
			wr.writeInt(978);
			wr.writeString(""); // Owner ID

			// Object info
			wr.writeInt(0);
			wr.writeLong(System.currentTimeMillis() / 1000);
			wr.writeDouble(ent.locationInfo.position.x);
			wr.writeDouble(ent.locationInfo.position.y);
			wr.writeDouble(ent.locationInfo.position.z);
			wr.writeDouble(ent.locationInfo.rotation.x);
			wr.writeDouble(ent.locationInfo.rotation.y);
			wr.writeDouble(ent.locationInfo.rotation.z);
			wr.writeDouble(ent.locationInfo.rotation.w);
			wr.add("0%0%0%0.0%0%0%0");
			wr.writeString(""); // data suffix
			client.sendPacket(wr.encode());
		}
	}

	/**
	 * Called to handle interactions
	 * 
	 * @param player              Player making the interaction
	 * @param interactableId      Interactable object ID
	 * @param object              NetworkedObject associated with the interactable
	 *                            ID
	 * @param state               Interaction state
	 * @param destroyOnCompletion Defines whether or not the resource will be
	 *                            destroyed on interaction completion (resources
	 *                            only)
	 * @return New destroyOnCompletion state
	 */
	public static boolean handleInteraction(Player player, String interactableId, NetworkedObject object, int state,
			boolean destroyOnCompletion) {
		// Find module
		for (InteractionModule mod : modules) {
			if (mod.canHandle(player, interactableId, object)) {
				// Handle interaction
				destroyOnCompletion = mod.shouldDestroyResource(player, interactableId, object, state,
						destroyOnCompletion);
				if (mod.handleInteractionSuccess(player, interactableId, object, state))
					return destroyOnCompletion;
			}
		}
		return destroyOnCompletion;
	}

	/**
	 * Called to handle interaction data requests
	 * 
	 * @param player         Player making the interaction
	 * @param interactableId Interactable object ID
	 * @param object         NetworkedObject associated with the interactable ID
	 * @param state          Interaction state
	 */
	public static void handleInteractionDataRequest(Player player, String interactableId, NetworkedObject object,
			int state) {
		// Find module
		boolean handled = false;
		for (InteractionModule mod : modules) {
			// Check if the interaction is not blocked
			int v = mod.isDataRequestValid(player, interactableId, object, state);
			if (v != -1)
				handled = true;
			if (v == 0)
				return;
		}
		if (!handled) {
			if (Centuria.debugMode)
				Centuria.logger.warn(MarkerManager.getMarker("INTERACTIONS"), "OASKR for " + interactableId
						+ " did not have its validity checked by any interaction module!");
		}

		// Find state
		if (!player.stateObjects.containsKey(interactableId)) {
			int tState = 0;
			int nState = player.states.getOrDefault(interactableId, 0);
			if (object.stateInfo.containsKey(Integer.toString(nState)))
				tState = nState;

			// Select state
			if (object.stateInfo.containsKey(Integer.toString(tState)))
				player.stateObjects.put(interactableId, object.stateInfo.get(Integer.toString(tState)));
		}
		if (player.stateObjects.containsKey(interactableId)) {
			// Run branches
			ArrayList<StateInfo> states = player.stateObjects.get(interactableId);
			for (StateInfo st : states) {
				runBranches(player, st.branches, Integer.toString(state), interactableId, object, st);
			}
		}
	}

	/**
	 * Runs a branch list
	 * 
	 * @param plr      Player to run the commands for
	 * @param branches Branch map
	 * @param id       Branch ID
	 * @param target   Interaction ID
	 * @param object   Object interacted with
	 * @param parent   Parent state
	 */
	public static void runBranches(Player plr, HashMap<String, ArrayList<StateInfo>> branches, String id, String target,
			NetworkedObject object, StateInfo parent) {
		// Handle branch commands
		// TODO: implement more, hopefully switch everything over
		// and use the modules for security checks
		if (branches.containsKey(id)) {
			HashMap<String, Object> memory = new HashMap<String, Object>();
			var states = branches.get(id);
			plr.stateObjects.put(target, states);
			for (StateInfo state : states) {
				switch (state.command) {
				case "1": {
					// Switch state
					String t = target;
					if (!state.actorId.equals("0"))
						t = state.actorId;
					Centuria.logger.debug(MarkerManager.getMarker("INTERACTION COMMANDS"),
							"Running command: 1 (set state), SET " + t + " TO " + state.params[0]);
					// Check state
					NetworkedObject obj = NetworkedObjects.getObject(t);
					if (obj.stateInfo.containsKey(state.params[0]))
						plr.states.put(t, Integer.parseInt(state.params[0]));
					break;
				}
				case "41": {
					// Give table
					Centuria.logger.debug(MarkerManager.getMarker("INTERACTION COMMANDS"),
							"Running command: 41 (give loot), GIVE TABLE " + state.params[0]);
					ResourceCollectionModule.giveLootReward(plr, state.params[0], object.primaryObjectInfo.defId);
					break;
				}
				default: {
					// Find module
					boolean warn = true;
					for (InteractionModule mod : modules) {
						// Run interaction
						if (mod.handleCommand(plr, target, object, state, parent, memory)) {
							warn = false;
							break;
						}
					}

					// Unhandled if true
					if (warn)
						Centuria.logger.debug(MarkerManager.getMarker("INTERACTION COMMANDS"),
								"Unhandled command: " + state.command);
					break;
				}
				}
			}
		}
	}

}
