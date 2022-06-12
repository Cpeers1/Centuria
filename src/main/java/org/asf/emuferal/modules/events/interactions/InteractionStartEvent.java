package org.asf.emuferal.modules.events.interactions;

import org.asf.emuferal.interactions.dataobjects.NetworkedObject;
import org.asf.emuferal.modules.eventbus.EventObject;
import org.asf.emuferal.modules.eventbus.EventPath;
import org.asf.emuferal.players.Player;

/**
 * 
 * Interaction Start Event - called when a player starts interacting with a
 * Interactable NetworkedObject
 * 
 * @author Sky Swimmer - AerialWorks Software Foundation
 *
 */
@EventPath("interaction.start")
public class InteractionStartEvent extends EventObject {

	private String objectId;
	private NetworkedObject object;
	private Player player;

	public InteractionStartEvent(Player player, String objectId, NetworkedObject object) {
		this.player = player;
		this.object = object;
		this.objectId = objectId;
	}

	@Override
	public String eventPath() {
		return "interaction.start";
	}

	/**
	 * Retrieves the player making the interaction
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
