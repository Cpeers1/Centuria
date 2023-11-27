package org.asf.centuria.modules.events.interactions;

import org.asf.centuria.entities.players.Player;
import org.asf.centuria.interactions.dataobjects.NetworkedObject;
import org.asf.centuria.modules.eventbus.EventObject;

/**
 * 
 * Interaction Success Event - called when a player completes a interaction
 * 
 * @author Sky Swimmer - AerialWorks Software Foundation
 *
 */
public class InteractionSuccessEvent extends EventObject {

	private String objectId;
	private NetworkedObject object;
	private Player player;
	private int state;
	private boolean destroyResource;

	public InteractionSuccessEvent(Player player, String objectId, NetworkedObject object, int state) {
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

	/**
	 * Checks if this resource will be destroyed after the interaction finishes
	 * (resources only)
	 * 
	 * @return True if the resource should be destroyed after interacting, false
	 *         otherwise
	 */
	public boolean shouldDestroyResource() {
		return destroyResource;
	}

	/**
	 * Defines whether or not the resource that was interacted with will be
	 * destroyed (resources only)
	 * 
	 * @param destroyResource True to destroy the resource, false otherwise
	 */
	public void setDestroyResource(boolean destroyResource) {
		this.destroyResource = destroyResource;
	}

}
