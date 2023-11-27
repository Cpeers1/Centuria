package org.asf.centuria.modules.events.interactions;

import org.asf.centuria.entities.players.Player;
import org.asf.centuria.interactions.dataobjects.NetworkedObject;
import org.asf.centuria.modules.eventbus.EventObject;

/**
 * 
 * Interaction Cancel Event - called when a player stops interacting with a
 * Interactable NetworkedObject before completion
 * 
 * @author Sky Swimmer - AerialWorks Software Foundation
 *
 */
public class InteractionCancelEvent extends EventObject {

	private String objectId;
	private NetworkedObject object;
	private Player player;

	public InteractionCancelEvent(Player player, String objectId, NetworkedObject object) {
		this.player = player;
		this.object = object;
		this.objectId = objectId;
	}

	/**
	 * Retrieves the player that made the interaction
	 * 
	 * @return Player instance
	 */
	public Player getPlayer() {
		return player;
	}

	/**
	 * Checks if the object that is being interacted with is actually defined
	 * 
	 * @return True if the object definition is recognized, false otherwise
	 */
	public boolean isObjectDefined() {
		return object != null;
	}

	/**
	 * Retrieves the object that is being interacted with (null if undefined)
	 * 
	 * @return NetworkedObject instance or null
	 */
	public NetworkedObject getObject() {
		return object;
	}

	/**
	 * Retrieves the UUID of the object that is being interacted with
	 * 
	 * @return Object ID
	 */
	public String getObjectId() {
		return objectId;
	}

}
