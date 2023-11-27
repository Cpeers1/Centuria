package org.asf.centuria.modules.events.interactions;

import org.asf.centuria.entities.players.Player;
import org.asf.centuria.interactions.dataobjects.NetworkedObject;
import org.asf.centuria.modules.eventbus.EventObject;

/**
 * 
 * Interaction Data Request Event - called when a client requests interaction
 * data
 * 
 * @author Sky Swimmer - AerialWorks Software Foundation
 *
 */
public class InteractionDataRequestEvent extends EventObject {

	private String objectId;
	private NetworkedObject object;
	private Player player;
	private int state;

	public InteractionDataRequestEvent(Player player, String objectId, NetworkedObject object, int state) {
		this.player = player;
		this.object = object;
		this.objectId = objectId;
		this.state = state;
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
	 * Retrieves the object that is being interacted with
	 * 
	 * @return NetworkedObject instance
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

	/**
	 * Retrieves the interaction state
	 * 
	 * @return Interaction state
	 */
	public int getState() {
		return state;
	}

}
