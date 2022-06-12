package org.asf.emuferal.modules.events.interactions;

import org.asf.emuferal.interactions.dataobjects.NetworkedObject;
import org.asf.emuferal.modules.eventbus.EventObject;
import org.asf.emuferal.modules.eventbus.EventPath;
import org.asf.emuferal.players.Player;

/**
 * 
 * Interaction Success Event - called when a player completes a interaction
 * 
 * @author Sky Swimmer - AerialWorks Software Foundation
 *
 */
@EventPath("interaction.success")
public class InteractionSuccessEvent extends EventObject {

	private String objectId;
	private NetworkedObject object;
	private Player player;
	private int state;

	public InteractionSuccessEvent(Player player, String objectId, NetworkedObject object, int state) {
		this.player = player;
		this.object = object;
		this.objectId = objectId;
		this.state = state;
	}

	@Override
	public String eventPath() {
		return "interaction.success";
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
