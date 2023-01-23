package org.asf.centuria.interactions.modules;

import java.util.ArrayList;
import java.util.List;

import org.asf.centuria.entities.players.Player;
import org.asf.centuria.interactions.dataobjects.NetworkedObject;
import org.asf.centuria.interactions.dataobjects.StateInfo;
import org.asf.centuria.packets.xt.gameserver.inventory.InventoryItemPacket;
import org.asf.centuria.packets.xt.gameserver.shop.ShopItemUnlockPacket;
import org.asf.centuria.shops.ShopManager;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class ShopkeeperModule extends InteractionModule {

	@Override
	public void prepareWorld(int levelID, List<String> ids, Player player) {
	}

	@Override
	public boolean canHandle(Player player, String id, NetworkedObject object) {
		// Check if this NPC is a shopkeeper
		for (ArrayList<StateInfo> states : object.stateInfo.values()) {
			for (StateInfo state : states) {
				if (!state.branches.isEmpty()) {
					for (ArrayList<StateInfo> branches : state.branches.values()) {
						for (StateInfo branch : branches)
							if (branch.command.equals("84") && branch.params.length == 3
									&& ShopManager.isShop(branch.params[2]))
								return true;
					}
				}
			}
		}
		return false;
	}

	@Override
	public boolean handleInteractionSuccess(Player player, String id, NetworkedObject object, int state) {
		return false;
	}

	@Override
	public int isDataRequestValid(Player player, String id, NetworkedObject object, int state) {
		if (canHandle(player, id, object)) {
			// Check enigmas

			// Find shop
			String shop = "";
			for (ArrayList<StateInfo> states : object.stateInfo.values()) {
				for (StateInfo st : states) {
					if (!st.branches.isEmpty()) {
						for (ArrayList<StateInfo> branches : st.branches.values()) {
							for (StateInfo branch : branches)
								if (branch.command.equals("84") && branch.params.length == 3
										&& ShopManager.isShop(branch.params[2]))
									shop = branch.params[2];
						}
					}
				}
			}

			// Get enigmas
			String[] enigmas = ShopManager.getEnigmaItems(player.account, shop);
			if (enigmas.length == 0) {
				// No enigmas
				ShopItemUnlockPacket pk = new ShopItemUnlockPacket();
				pk.success = false;
				player.client.sendPacket(pk);
			} else {
				// Enigmas were found

				// Unlock enigmas
				for (String enigma : enigmas) {
					JsonObject enigmaData = player.account.getSaveSpecificInventory().getAccessor()
							.findInventoryObject("7", enigma);

					// Set as unraveled
					JsonObject data = enigmaData.get("components").getAsJsonObject().get("Enigma").getAsJsonObject();
					data.remove("activated");
					data.addProperty("activated", true);

					// Save
					player.account.getSaveSpecificInventory().setItem("7",
							player.account.getSaveSpecificInventory().getItem("7"));

					// Send to player
					JsonArray lst = new JsonArray();
					lst.add(enigmaData);
					InventoryItemPacket pk = new InventoryItemPacket();
					pk.item = lst;
					player.client.sendPacket(pk);
				}

				// Send enigma unlock packets
				ShopItemUnlockPacket pk = new ShopItemUnlockPacket();
				pk.success = true;
				pk.shopId = shop;
				for (String enigma : enigmas)
					pk.items.add(enigma);
				player.client.sendPacket(pk);
			}

			return 1;
		}
		return -1;
	}

}
