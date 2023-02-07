package org.asf.centuria.interactions.modules;

import java.util.ArrayList;
import java.util.List;

import org.asf.centuria.entities.players.Player;
import org.asf.centuria.interactions.dataobjects.NetworkedObject;
import org.asf.centuria.interactions.dataobjects.StateInfo;
import org.asf.centuria.interactions.modules.resourcecollection.levelhooks.EventInfo;
import org.asf.centuria.levelevents.LevelEvent;
import org.asf.centuria.levelevents.LevelEventBus;
import org.asf.centuria.packets.xt.gameserver.inventory.InventoryItemPacket;

public class InspirationCollectionModule extends InteractionModule {

	@Override
	public void prepareWorld(int levelID, List<String> ids, Player player) {
	}

	@Override
	public boolean canHandle(Player player, String id, NetworkedObject object) {
		// Check if this object is a inspiration source

		for (ArrayList<StateInfo> states : object.stateInfo.values()) {
			for (StateInfo stateInfo : states) {

				// check if stateInfo has a command of 84
				if (stateInfo.command.equals("84") && stateInfo.params.length == 3) {
					// if param 1 is 1 and param 2 is 4, it means 'give inspiration'
					// I think...
					if (stateInfo.params[0].equals("1") && stateInfo.params[1].equals("4")) {
						return true;
					}

				} else {
					// check if any branches do
					if (!stateInfo.branches.isEmpty()) {
						for (ArrayList<StateInfo> branches : stateInfo.branches.values()) {
							for (StateInfo branch : branches) {
								if (branch.command.equals("84") && branch.params.length == 3) {
									// if param 1 is 1 and param 2 is 4, it means 'give inspiration'
									// I think...
									if (branch.params[0].equals("1") && branch.params[1].equals("4")) {
										return true;
									}
								}
							}
						}
					}
				}
			}
		}

		return false;
	}

	@Override
	public boolean handleInteractionSuccess(Player player, String id, NetworkedObject object, int state) {
		return true;
	}

	@Override
	public int isDataRequestValid(Player player, String id, NetworkedObject object, int state) {
		if (canHandle(player, id, object)) {
			object.stateInfo.forEach((k, states) -> states.forEach(t -> handleCommand(player, id, object, t, null)));
			return 1; // Safe to run
		}
		return -1;

	}

	@Override
	public boolean handleCommand(Player player, String id, NetworkedObject object, StateInfo stateInfo,
			StateInfo parent) {
		// add inspiration to inventory?
		// get inspiration ID from commands

		if (stateInfo.command.equals("84") && canHandle(player, id, object)) {
			// get the third argument

			String defId = stateInfo.params[2];
			if (!defId.matches("^[0-9]+$"))
				return false;

			var inspirationAccessor = player.account.getSaveSpecificInventory().getInspirationAccessor();
			if (!inspirationAccessor.hasInspiration(Integer.valueOf(defId))) {
				// Add inspiration
				inspirationAccessor.addInspiration(Integer.valueOf(defId));

				// Add xp
				EventInfo ev = new EventInfo();
				ev.event = "levelevents.inspirations";

				// Find map name
				String map = "unknown";
				switch (player.levelID) {
				case 820:
					map = "cityfera";
					break;
				case 2364:
					map = "bloodtundra";
					break;
				case 9687:
					map = "lakeroot";
					break;
				case 2147:
					map = "mugmyre";
					break;
				case 1689:
					map = "sanctuary";
					break;
				case 3273:
					map = "sunkenthicket";
					break;
				case 1825:
					map = "shatteredbay";
					break;
				}

				// Add tags
				ev.tags.add("inspiration:" + defId);
				ev.tags.add("map:" + map);

				// Dispatch event
				LevelEventBus.dispatch(new LevelEvent(ev.event, ev.tags.toArray(new String[0]), player));
			}

			// Update inventory
			var il = player.account.getSaveSpecificInventory().getItem("8");
			var ilPacket = new InventoryItemPacket();
			ilPacket.item = il;

			// Send IL
			player.client.sendPacket(ilPacket);

			return true;
		}
		return false;
	}

}
