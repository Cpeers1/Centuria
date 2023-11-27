package org.asf.centuria.modules.events.interactions;

import org.asf.centuria.entities.players.Player;
import org.asf.centuria.interactions.dataobjects.NetworkedObject;
import org.asf.centuria.modules.eventbus.EventObject;

/**
 * 
 * Interaction State Change Event - called when the state of a networked object
 * is changed
 * 
 * @since Beta 1.8
 * @author Sky Swimmer - AerialWorks Software Foundation
 *
 */
public class InteractionStateChangeEvent extends EventObject {

	private String objectId;
	private NetworkedObject object;
	private Player player;

	private int oldState;
	private int state;

	public InteractionStateChangeEvent(Player player, String objectId, NetworkedObject object, int oldState,
			int state) {
		this.player = player;
		this.object = object;
		this.objectId = objectId;
		this.oldState = oldState;
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
	 * Retrieves the object that had its state changed
	 * 
	 * @return NetworkedObject instance
	 */
	public NetworkedObject getObject() {
		return object;
	}

	/**
	 * Retrieves the UUID of the object that had its state changed
	 * 
	 * @return Object ID
	 */
	public String getObjectId() {
		return objectId;
	}

	/**
	 * Retrieves the current interaction state
	 * 
	 * @return Interaction state
	 */
	public int getState() {
		return state;
	}

	/**
	 * Retrieves the previous interaction state
	 * 
	 * @return Interaction state
	 */
	public int getPreviousState() {
		return oldState;
	}

}
