package org.asf.emuferal.interactions.modules;

import java.util.ArrayList;
import java.util.List;
import org.asf.emuferal.interactions.dataobjects.NetworkedObject;
import org.asf.emuferal.interactions.dataobjects.StateInfo;
import org.asf.emuferal.packets.xt.gameserver.inventory.InventoryItemPacket;
import org.asf.emuferal.players.Player;

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
								if (branch.command.equals("84") && stateInfo.params.length == 3) {

									// if param 1 is 1 and param 2 is 4, it means 'give inspiration'
									// I think...
									if (stateInfo.params[0].equals("1") && stateInfo.params[1].equals("4")) {
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
	public boolean handleInteractionDataRequest(Player player, String id, NetworkedObject object, int state) {
		// add inspiration to inventory?
		// get inspiration ID from commands

		for (ArrayList<StateInfo> states : object.stateInfo.values()) {
			for (StateInfo stateInfo : states) {

				// check if stateInfo has a command of 84
				if (stateInfo.command.equals("84")) {
					// get the third argument

					String defId = stateInfo.params[2];

					var inspirationAccessor = player.account.getPlayerInventory().getInspirationAccessor();

					if (!inspirationAccessor.hasInspiration(Integer.valueOf(defId))) {
						inspirationAccessor.addInspiration(Integer.valueOf(defId));
					}

					// send IL packet

					var il = player.account.getPlayerInventory().getItem("8");
					var ilPacket = new InventoryItemPacket();
					ilPacket.item = il;

					// send IL
					player.client.sendPacket(ilPacket);

					return true;
				} else {
					// check if any branches do

					if (!stateInfo.branches.isEmpty()) {
						for (ArrayList<StateInfo> branches : stateInfo.branches.values()) {
							for (StateInfo branch : branches) {
								if (branch.command.equals("84")) {
									// get the third argument

									String defId = branch.params[2];

									var inspirationAccessor = player.account.getPlayerInventory()
											.getInspirationAccessor();

									if (!inspirationAccessor.hasInspiration(Integer.valueOf(defId))) {
										inspirationAccessor.addInspiration(Integer.valueOf(defId));
									}

									// send IL packet

									var il = player.account.getPlayerInventory().getItem("8");
									var ilPacket = new InventoryItemPacket();
									ilPacket.item = il;

									// send IL
									player.client.sendPacket(ilPacket);

									return true;
								}
							}
						}
					}
				}
			}
		}

		return false;
	}

}
