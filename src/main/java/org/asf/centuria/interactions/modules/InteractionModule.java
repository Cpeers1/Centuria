package org.asf.centuria.interactions.modules;

import java.util.List;

import org.asf.centuria.entities.players.Player;
import org.asf.centuria.interactions.dataobjects.NetworkedObject;
import org.asf.centuria.networking.smartfox.SmartfoxClient;

public abstract class InteractionModule {

	/**
	 * 
	 * @param player              Player making the interaction
	 * @param id                  Interaction ID
	 * @param object              Object that was interacted with
	 * @param state               Interaction state
	 * @param destroyOnCompletion Defines whether or not the resource will be
	 *                            destroyed on interaction completion (resources
	 *                            only)
	 * @return New destroyOnCompletion state
	 */
	public boolean shouldDestroyResource(Player player, String id, NetworkedObject object, int state,
			boolean destroyOnCompletion) {
		return destroyOnCompletion;
	}

	/**
	 * Called to prepare world objects
	 * 
	 * @param levelID Level ID
	 * @param ids     List of interactable IDs in the world
	 * @param player  Player to prepare this module for
	 */
	public abstract void prepareWorld(int levelID, List<String> ids, Player player);

	/**
	 * Checks if this module can handle the given interaction
	 * 
	 * @param player Player making the interaction
	 * @param id     Interaction ID
	 * @param object Object that was interacted with
	 * @return True if the given interaction can be processed by this module, false
	 *         otherwise
	 */
	public abstract boolean canHandle(Player player, String id, NetworkedObject object);

	/**
	 * Handles interaction success packets
	 * 
	 * @param player Player making the interaction
	 * @param id     Interaction ID
	 * @param object Object that was interacted with
	 * @param state  Interaction state
	 * @return True if the given interaction was handled, false otherwise
	 */
	public abstract boolean handleInteractionSuccess(Player player, String id, NetworkedObject object, int state);

	/**
	 * Handles interaction data requests
	 * 
	 * @param player Player making the interaction
	 * @param id     Interaction ID
	 * @param object Object that was interacted with
	 * @param state  Interaction state
	 * @return True if the given interaction request was handled, false otherwise
	 */
	public abstract boolean handleInteractionDataRequest(Player player, String id, NetworkedObject object, int state);

	/**
	 * Initializes world objects
	 * 
	 * @param client Client to send the packets to
	 * @param id     World object ID
	 * @param obj    World object
	 * @return True if handled, false otherwise
	 */
	public boolean initializeWorldObjects(SmartfoxClient client, String id, NetworkedObject obj) {
		return false;
	}

}
