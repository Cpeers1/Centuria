package org.asf.centuria.packets.xt.gameserver.sanctuary;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.asf.centuria.Centuria;
import org.asf.centuria.data.XtReader;
import org.asf.centuria.data.XtWriter;
import org.asf.centuria.entities.inventoryitems.twiggles.TwiggleItem;
import org.asf.centuria.entities.players.Player;
import org.asf.centuria.entities.twiggles.TwiggleWorkParameters;
import org.asf.centuria.enums.twiggles.TwiggleState;
import org.asf.centuria.networking.smartfox.SmartfoxClient;
import org.asf.centuria.packets.xt.IXtPacket;
import org.asf.centuria.packets.xt.gameserver.inventory.InventoryItemPacket;
import org.asf.centuria.util.SanctuaryWorkCalculator;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;

public class SanctuaryUpgradeStartPacket implements IXtPacket<SanctuaryUpgradeStartPacket> {

	private static final String PACKET_ID = "sus";

	public int stage;
	public List<Integer> enlargedAreaIndexes;
	private int expansionIndex;

	public boolean success;

	@Override
	public String id() {
		return PACKET_ID;
	}

	@Override
	public SanctuaryUpgradeStartPacket instantiate() {
		return new SanctuaryUpgradeStartPacket();
	}

	@Override
	public void parse(XtReader reader) throws IOException {
		stage = reader.readInt();

		int arraySize = reader.readInt();
		enlargedAreaIndexes = new ArrayList<Integer>();

		for (int i = 0; i < arraySize; i++) {
			enlargedAreaIndexes.add(reader.readInt());
		}
	}

	@Override
	public void build(XtWriter writer) throws IOException {
		writer.writeInt(DATA_PREFIX); // Data prefix

		writer.writeBoolean(success);

		writer.writeString(DATA_SUFFIX); // Data suffix
	}

	@Override
	public boolean handle(SmartfoxClient client) throws IOException {

		try {
			// Assign work to twiggle
			// Work is of type 'WorkingSanctuary'

			var player = (Player) client.container;
			var twiggleAccessor = player.account.getSaveSpecificInventory().getTwiggleAccesor();
			boolean isStageUpgrade = false;
			boolean isRoomUpgrade = false;
			boolean didDisablingSucceed = false;

			TwiggleItem updatedTwiggle = null;
			var sanctuaryLookInfo = player.account.getSaveSpecificInventory().getSanctuaryAccessor()
					.getSanctuaryLook(player.account.getActiveSanctuaryLook()).get("components").getAsJsonObject()
					.get("SanctuaryLook").getAsJsonObject().get("info");
			var classItemInvId = sanctuaryLookInfo.getAsJsonObject().get("classInvId").getAsString();
			var houseItemInvId = sanctuaryLookInfo.getAsJsonObject().get("houseInvId").getAsString();

			// If the stage has gone up, its a stage upgrade
			if (stage > player.account.getSaveSpecificInventory().getSanctuaryAccessor()
					.getCurrentSanctuaryStage(classItemInvId)) {
				TwiggleWorkParameters workParams = new TwiggleWorkParameters();
				workParams.classItemInvId = classItemInvId;
				workParams.stage = stage;
				updatedTwiggle = twiggleAccessor.setTwiggleWork(TwiggleState.WorkingSanctuary,
						System.currentTimeMillis() + SanctuaryWorkCalculator.getTimeForStageUp(stage), workParams);
				Centuria.logger.debug("Stage upgrade");
				isStageUpgrade = true;
			}
			// disabling/enabling room
			else if (stage <= player.account.getSaveSpecificInventory().getSanctuaryAccessor()
					.getCurrentSanctuaryStage(classItemInvId)) {
				didDisablingSucceed = player.account.getSaveSpecificInventory().getSanctuaryAccessor()
						.upgradeSanctuaryToStage(classItemInvId, stage);
				Centuria.logger.debug("Room disabling");
			}

			// get the og expansion arrays (house inv. array is temporary, while class inv.
			// array is
			// permanent)
			JsonArray houseInvExpansionArray = player.account.getSaveSpecificInventory().getSanctuaryAccessor()
					.getHouseExpandedRoomsArray(houseItemInvId);
			JsonArray classInvExpansionArray = player.account.getSaveSpecificInventory().getSanctuaryAccessor()
					.getClassExpandedRoomsArray(classItemInvId);

			// room expansion (when class inv. array differs from the one in packet by a
			// spot)
			for (int i = 0; i < enlargedAreaIndexes.size() && i < classInvExpansionArray.size(); i++) {
				if (enlargedAreaIndexes.get(i) == 1 && classInvExpansionArray.get(i).getAsInt() == 0) {
					expansionIndex = i;

					TwiggleWorkParameters workParams = new TwiggleWorkParameters();
					workParams.classItemInvId = classItemInvId;
					workParams.enlargedAreaIndex = expansionIndex;

					updatedTwiggle = twiggleAccessor.setTwiggleWork(TwiggleState.WorkingSanctuary,
							System.currentTimeMillis()
									+ SanctuaryWorkCalculator.getTimeForExpand(expansionIndex),
							workParams);
					isRoomUpgrade = true;
					Centuria.logger.debug("Room expansion");
					break;
				}
			}

			// enabling/disabling expansion (when house inv. array differs from the one in
			// packet)
			for (int i = 0; i < enlargedAreaIndexes.size() && i < houseInvExpansionArray.size(); i++) {
				if (enlargedAreaIndexes.get(i) != houseInvExpansionArray.get(i).getAsInt()) {
					didDisablingSucceed = player.account.getSaveSpecificInventory().getSanctuaryAccessor()
							.expandManySanctuaryRooms(classItemInvId,
									new GsonBuilder().create().toJsonTree(enlargedAreaIndexes).getAsJsonArray());
					Centuria.logger.debug("Expansion disabling");
					break;
				}
			}
			
			// send IL packet from another packet's class to reload world after disabling/enabling
			SanctuaryUpgradeCompletePacket SanctuaryUpgradeCompletePacketObject = new SanctuaryUpgradeCompletePacket();
			if (didDisablingSucceed && !isStageUpgrade && !isRoomUpgrade) {
				SanctuaryUpgradeCompletePacketObject.sendIlPacket(player);
				SanctuaryUpgradeCompletePacketObject.JoinSanctuary(client, player.account.getAccountID());
			}

			if (updatedTwiggle == null) {
				// failed to expand or room was disabled
				this.success = didDisablingSucceed;
				client.sendPacket(this);
			} else {
				// remove resources from player
				if (isStageUpgrade) {
					var map = SanctuaryWorkCalculator.getCostForStageUp(stage);

					for (var item : map.entrySet()) {
						player.account.getSaveSpecificInventory().getItemAccessor(player).remove(item.getKey(),
								item.getValue());
					}
				}
				if (isRoomUpgrade) {
					var map = SanctuaryWorkCalculator.getCostForEnlargen(expansionIndex);

					for (var item : map.entrySet()) {
						player.account.getSaveSpecificInventory().getItemAccessor(player).remove(item.getKey(),
								item.getValue());
					}
				}

				InventoryItemPacket packet = new InventoryItemPacket();
				var array = new JsonArray();
				array.add(updatedTwiggle.toJsonObject());
				packet.item = array;
				client.sendPacket(packet);

				this.success = true;
				client.sendPacket(this);
			}

		} catch (

		Exception e) {
			e.printStackTrace();

			// failed to expand
			this.success = false;
			client.sendPacket(this);
		}

		return true;
	}

}
