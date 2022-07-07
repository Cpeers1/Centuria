package org.asf.emuferal.accounts.highlevel;

import org.asf.emuferal.accounts.PlayerInventory;
import org.asf.emuferal.accounts.highlevel.itemdata.item.ItemComponent;
import org.asf.emuferal.networking.smartfox.SmartfoxClient;
import org.asf.emuferal.packets.xt.gameserver.inventory.InventoryItemPacket;

import com.google.gson.JsonObject;

public class CurrencyAccessor {
	private PlayerInventory inventory;

	public CurrencyAccessor(PlayerInventory inventory) {
		this.inventory = inventory;
	}

	/**
	 * Retrieves the likes count a player has
	 * 
	 * @return Amount of likes
	 */
	public int getLikes() {
		if (!inventory.getAccessor().hasInventoryObject("104", 2327))
			return 2500;

		return inventory.getAccessor().findInventoryObject("104", 2327).get("components").getAsJsonObject()
				.get("Quantity").getAsJsonObject().get("quantity").getAsInt();
	}

	/**
	 * Retrieves the amount of star fragments a player has
	 * 
	 * @return Amount of star fragments
	 */
	public int getStarFragments() {
		if (!inventory.getAccessor().hasInventoryObject("104", 14500))
			return 0;

		return inventory.getAccessor().findInventoryObject("104", 14500).get("components").getAsJsonObject()
				.get("Quantity").getAsJsonObject().get("quantity").getAsInt();
	}

	/**
	 * Retrieves the amount of lockpicks a player has
	 * 
	 * @return Amount of lockpicks
	 */
	public int getLockpicks() {
		if (!inventory.getAccessor().hasInventoryObject("104", 8372))
			return 0;

		return inventory.getAccessor().findInventoryObject("104", 8372).get("components").getAsJsonObject()
				.get("Quantity").getAsJsonObject().get("quantity").getAsInt();
	}

	/**
	 * Sets the likes count
	 * 
	 * @param client Client to update
	 * @param likes  New amount of likes
	 */
	public void setLikes(SmartfoxClient client, int likes) {
		setLikesDirectly(likes);
		updatePlayer(client);
	}

	/**
	 * Sets the star fragment count
	 * 
	 * @param client    Client to update
	 * @param starFrags New amount of star fragments
	 */
	public void setStarFragments(SmartfoxClient client, int starFrags) {
		setStarFragmentsDirectly(starFrags);
		updatePlayer(client);
	}

	/**
	 * Sets the lockpick count
	 * 
	 * @param client    Client to update
	 * @param lockpicks New amount of lockpicks
	 */
	public void setLockpicks(SmartfoxClient client, int lockpicks) {
		setLockpicksDirectly(lockpicks);
		updatePlayer(client);
	}

	/**
	 * Sets the likes count (directly)
	 * 
	 * @param likes New amount of likes
	 */
	public void setLikesDirectly(int likes) {
		if (likes <= 0) {
			inventory.getAccessor().removeInventoryObject("104", 2327);
			return;
		}
		if (!inventory.getAccessor().hasInventoryObject("104", 2327)) {
			// Create object
			JsonObject dq = new JsonObject();
			dq.addProperty("quantity", 0);
			inventory.getAccessor().createInventoryObject("104", 2327, new ItemComponent("Quantity", dq));
		}
		JsonObject q = inventory.getAccessor().findInventoryObject("104", 2327).get("components").getAsJsonObject()
				.get("Quantity").getAsJsonObject();
		q.remove("quantity");
		q.addProperty("quantity", likes);
		inventory.setItem("104", inventory.getItem("104"));
	}

	/**
	 * Sets the star fragment count (directly)
	 * 
	 * @param starFrags New amount of star fragments
	 */
	public void setStarFragmentsDirectly(int starFrags) {
		if (starFrags <= 0) {
			inventory.getAccessor().removeInventoryObject("104", 14500);
			return;
		}
		if (!inventory.getAccessor().hasInventoryObject("104", 14500)) {
			// Create object
			JsonObject dq = new JsonObject();
			dq.addProperty("quantity", 0);
			inventory.getAccessor().createInventoryObject("104", 14500, new ItemComponent("Quantity", dq));
		}
		JsonObject q = inventory.getAccessor().findInventoryObject("104", 14500).get("components").getAsJsonObject()
				.get("Quantity").getAsJsonObject();
		q.remove("quantity");
		q.addProperty("quantity", starFrags);
		inventory.setItem("104", inventory.getItem("104"));
	}

	/**
	 * Sets the lockpick count (directly)
	 * 
	 * @param lockpicks New amount of lockpicks
	 */
	public void setLockpicksDirectly(int lockpicks) {
		if (lockpicks <= 0) {
			inventory.getAccessor().removeInventoryObject("104", 8372);
			return;
		}
		if (!inventory.getAccessor().hasInventoryObject("104", 8372)) {
			// Create object
			JsonObject dq = new JsonObject();
			dq.addProperty("quantity", 0);
			inventory.getAccessor().createInventoryObject("104", 8372, new ItemComponent("Quantity", dq));
		}
		JsonObject q = inventory.getAccessor().findInventoryObject("104", 8372).get("components").getAsJsonObject()
				.get("Quantity").getAsJsonObject();
		q.remove("quantity");
		q.addProperty("quantity", lockpicks);
		inventory.setItem("104", inventory.getItem("104"));
	}

	/**
	 * Adds a amount of likes to the player
	 * 
	 * @param client Client to update
	 * @param amount Amount of likes to add
	 */
	public void addLikes(SmartfoxClient client, int amount) {
		addLikesDirectly(amount);
		updatePlayer(client);
	}

	/**
	 * Adds a amount of star fragments to the player
	 * 
	 * @param client Client to update
	 * @param amount Amount of star fragments to add
	 */
	public void addStarFragments(SmartfoxClient client, int amount) {
		addStarFragmentsDirectly(amount);
		updatePlayer(client);
	}

	/**
	 * Adds a amount of lockpicks to the player
	 * 
	 * @param client Client to update
	 * @param amount Amount of lockpicks to add
	 */
	public void addLockpicks(SmartfoxClient client, int amount) {
		addLockpicksDirectly(amount);
		updatePlayer(client);
	}

	/**
	 * Adds a amount of likes to the player (directly)
	 * 
	 * @param amount Amount of likes to add
	 */
	public void addLikesDirectly(int amount) {
		setLikesDirectly(getLikes() + amount);
	}

	/**
	 * Adds a amount of star fragments to the player (directly)
	 * 
	 * @param amount Amount of star fragments to add
	 */
	public void addStarFragmentsDirectly(int amount) {
		setStarFragmentsDirectly(getStarFragments() + amount);
	}

	/**
	 * Adds a amount of lockpicks to the player (directly)
	 * 
	 * @param amount Amount of lockpicks to add
	 */
	public void addLockpicksDirectly(int amount) {
		setLockpicksDirectly(getLockpicks() + amount);
	}

	/**
	 * Removes a amount of likes from the player
	 * 
	 * @param client Client to update
	 * @param amount Amount of likes to remove
	 * @return True if successful, false otherwise
	 */
	public boolean removeLikes(SmartfoxClient client, int amount) {
		if (amount <= getLikes()) {
			setLikesDirectly(getLikes() - amount);
			updatePlayer(client);
			return true;
		}
		return false;
	}

	/**
	 * Removes a amount of star fragments from the player
	 * 
	 * @param client Client to update
	 * @param amount Amount of star fragments to remove
	 * @return True if successful, false otherwise
	 */
	public boolean removeStarFragments(SmartfoxClient client, int amount) {
		if (amount <= getStarFragments()) {
			setStarFragmentsDirectly(getStarFragments() - amount);
			updatePlayer(client);
			return true;
		}
		return false;
	}

	/**
	 * Removes a amount of lockpicks from the player
	 * 
	 * @param client Client to update
	 * @param amount Amount of lockpicks to remove
	 * @return True if successful, false otherwise
	 */
	public boolean removeLockpicks(SmartfoxClient client, int amount) {
		if (amount <= getLockpicks()) {
			setLockpicksDirectly(getLockpicks() - amount);
			updatePlayer(client);
			return true;
		}
		return false;
	}

	/**
	 * Removes a amount of likes from the player (directly)
	 * 
	 * @param amount Amount of likes to remove
	 * @return True if successful, false otherwise
	 */
	public boolean removeLikesDirectly(int amount) {
		if (amount <= getLikes()) {
			setLikesDirectly(getLikes() - amount);
			return true;
		}
		return false;
	}

	/**
	 * Removes a amount of star fragments from the player (directly)
	 * 
	 * @param amount Amount of star fragments to remove
	 * @return True if successful, false otherwise
	 */
	public boolean removeStarFragmentsDirectly(int amount) {
		if (amount <= getStarFragments()) {
			setStarFragmentsDirectly(getStarFragments() - amount);
			return true;
		}
		return false;
	}

	/**
	 * Removes a amount of lockpicks from the player (directly)
	 * 
	 * @param amount Amount of lockpicks to remove
	 * @return True if successful, false otherwise
	 */
	public boolean removeLockpicksDirectly(int amount) {
		if (amount <= getLockpicks()) {
			setLockpicksDirectly(getLockpicks() - amount);
			return true;
		}
		return false;
	}

	// Called to update the player
	private void updatePlayer(SmartfoxClient client) {
		// Update currency object in client inventory
		InventoryItemPacket pkt = new InventoryItemPacket();
		pkt.item = inventory.getItem("104");
		client.sendPacket(pkt);
	}

}