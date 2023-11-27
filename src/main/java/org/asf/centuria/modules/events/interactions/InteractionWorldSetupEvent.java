package org.asf.centuria.modules.events.interactions;

import java.util.List;

import org.asf.centuria.entities.players.Player;
import org.asf.centuria.modules.eventbus.EventObject;

/**
 * 
 * Interaction World Setup Event - called when the server is setting up world
 * interactions
 * 
 * @since Beta 1.8
 * @author Sky Swimmer - AerialWorks Software Foundation
 *
 */
public class InteractionWorldSetupEvent extends EventObject {

	private Player player;
	private int levelID;
	private List<String> objectIDs;

	public InteractionWorldSetupEvent(Player player, int levelID, List<String> objectIDs) {
		this.player = player;
		this.levelID = levelID;
		this.objectIDs = objectIDs;
	}

	/**
	 * Retrieves the player instance
	 * 
	 * @return Player instance
	 */
	public Player getPlayer() {
		return player;
	}

	/**
	 * Retrieves the level ID the player is joining
	 * 
	 * @return NetworkedObject instance
	 */
	public int getLevelID() {
		return levelID;
	}

	/**
	 * Registers interactable objects
	 * 
	 * @param objectID Interactable object ID
	 */
	public void registerInteractable(String objectID) {
		objectIDs.add(objectID);
	}

}
