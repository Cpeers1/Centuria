package org.asf.centuria.entities.sanctuaries;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.asf.centuria.accounts.AccountManager;
import org.asf.centuria.accounts.CenturiaAccount;
import org.asf.centuria.entities.inventoryitems.sanctuaries.HouseItem;
import org.asf.centuria.entities.inventoryitems.sanctuaries.IslandItem;
import org.asf.centuria.entities.inventoryitems.sanctuaries.SanctuaryClassItem;
import org.asf.centuria.entities.inventoryitems.sanctuaries.SanctuaryLookItem;
import org.asf.centuria.entities.players.Player;

public class PlayerSanctuaryController {

	public static Map<String, PlayerSanctuaryController> openSanctuaryInstances = new HashMap<String, PlayerSanctuaryController>();

	public static boolean instanceExists(String ownerId) {
		if (openSanctuaryInstances.containsKey(ownerId)) {
			return true;
		} else {
			return false;
		}
	}

	public static PlayerSanctuaryController getInstance(String ownerId) {
		if (instanceExists(ownerId)) {
			return openSanctuaryInstances.get(ownerId);
		} else {
			return null;
		}
	}

	public static PlayerSanctuaryController newInstance(String ownerId) {
		try {
			PlayerSanctuaryController playerSanctuaryController = new PlayerSanctuaryController();
			playerSanctuaryController.owner = AccountManager.getInstance().getAccount(ownerId);

			var lookId = playerSanctuaryController.owner.getActiveSanctuaryLook();
			var accessor = playerSanctuaryController.owner.getPlayerInventory().getSanctuaryAccessor();

			playerSanctuaryController.sanctuaryLookInventoryItem = new SanctuaryLookItem();
			playerSanctuaryController.sanctuaryLookInventoryItem.fromJsonObject(accessor.getSanctuaryLook(lookId));

			var classId = playerSanctuaryController.sanctuaryLookInventoryItem
					.getSanctuaryLookComponent().info.classInvId;
			var islandId = playerSanctuaryController.sanctuaryLookInventoryItem
					.getSanctuaryLookComponent().info.islandInvId;
			var houseId = playerSanctuaryController.sanctuaryLookInventoryItem
					.getSanctuaryLookComponent().info.houseInvId;

			playerSanctuaryController.sanctuaryClassInventoryItem = new SanctuaryClassItem();
			playerSanctuaryController.sanctuaryClassInventoryItem
					.fromJsonObject(accessor.getSanctuaryClassObject(classId));

			playerSanctuaryController.islandInventoryItem = new IslandItem();
			playerSanctuaryController.islandInventoryItem.fromJsonObject(accessor.getIslandTypeObject(islandId));

			playerSanctuaryController.houseInventoryItem = new HouseItem();
			playerSanctuaryController.houseInventoryItem.fromJsonObject(accessor.getHouseTypeObject(houseId));

			return playerSanctuaryController;

		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	// TODO: Ban system for sanctuaries.
	// TODO: Kicking players in your sanctuary.
	// TODO: Funiture Tracking / List
	public SanctuaryLookItem sanctuaryLookInventoryItem;
	public SanctuaryClassItem sanctuaryClassInventoryItem;
	public IslandItem islandInventoryItem;
	public HouseItem houseInventoryItem;

	public CenturiaAccount owner;
	public List<Player> playersInSanctuary = new ArrayList<Player>();

	private PlayerSanctuaryController() {

	}

	private void playerJoined(Player player) {
		if (!playersInSanctuary.contains(player)) {
			playersInSanctuary.add(player);
		}
	}

	private void playerLeft(Player player) {
		if (playersInSanctuary.contains(player)) {
			playersInSanctuary.remove(player);
		}
	}

	public boolean expandRoom() {
		return false;
	}

	public boolean expandHouse() {
		return false;
	}

	public boolean toggleRoomExpand() {
		return false;
	}

	public boolean placeFuniture() {
		return false;
	}

	public boolean removeFuniture() {
		return false;
	}

	public boolean updateFuniture() {
		return false;
	}

	public boolean dyeFuniture() {
		return false;
	}

	public boolean updateRoom() {
		return false;
	}

	public boolean saveLook() {
		return false;
	}

	public boolean loadLook() {
		return false;
	}

}
