package org.asf.emuferal.packets.xt.gameserver.sanctuaries;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.asf.emuferal.accounts.highlevel.TwiggleAccessor;
import org.asf.emuferal.data.XtReader;
import org.asf.emuferal.data.XtWriter;
import org.asf.emuferal.entities.inventoryitems.InventoryItem;
import org.asf.emuferal.entities.inventoryitems.twiggles.TwiggleItem;
import org.asf.emuferal.entities.twiggles.TwiggleWorkParameters;
import org.asf.emuferal.enums.twiggles.TwiggleState;
import org.asf.emuferal.networking.gameserver.GameServer;
import org.asf.emuferal.networking.smartfox.SmartfoxClient;
import org.asf.emuferal.packets.xt.IXtPacket;
import org.asf.emuferal.packets.xt.gameserver.inventory.InventoryItemPacket;
import org.asf.emuferal.packets.xt.gameserver.world.JoinRoom;
import org.asf.emuferal.players.Player;
import org.asf.emuferal.util.SanctuaryWorkCalculator;

import com.google.gson.JsonArray;

public class SanctuaryUpgradeCompletePacket implements IXtPacket<SanctuaryUpgradeCompletePacket> {

	private static final String PACKET_ID = "suc";

	public String twiggleInvId;

	public boolean success;

	@Override
	public String id() {
		return PACKET_ID;
	}

	@Override
	public SanctuaryUpgradeCompletePacket instantiate() {
		return new SanctuaryUpgradeCompletePacket();
	}

	@Override
	public void parse(XtReader reader) throws IOException {
		twiggleInvId = reader.read();
	}

	@Override
	public void build(XtWriter writer) throws IOException {
		writer.writeInt(-1); // Data prefix

		writer.writeBoolean(success);

		writer.writeString(""); // Data suffix
	}

	@Override
	public boolean handle(SmartfoxClient client) throws IOException {

		try {

			// need to use the twiggle to find out what was worked on

			var player = (Player) client.container;
			var twiggleAccessor = player.account.getPlayerInventory().getTwiggleAccesor();

			var twiggleItem = twiggleAccessor.getTwiggle(twiggleInvId);

			switch (twiggleItem.getTwiggleComponent().workType) {
				case WorkingOtherSanctuary:
				case WorkingSanctuary:
					// we are ok
					break;
				case FinishedOtherSanctuary:
				case FinishedSanctuary:
				case None:
				default: {
					// failed to complete expansion
					this.success = false;
					client.sendPacket(this);
					return true;
				}
			}

			if (twiggleItem.getTwiggleComponent().twiggleWorkParams.stage != null) {
				// its a stage upgrade
				player.account.getPlayerInventory().getSanctuaryAccessor().upgradeSanctuaryToStage(
						twiggleItem.getTwiggleComponent().twiggleWorkParams.classItemInvId,
						twiggleItem.getTwiggleComponent().twiggleWorkParams.stage);
				
				var il = player.account.getPlayerInventory().getItem("201");
				var ilPacket = new InventoryItemPacket();
				ilPacket.item = il;

				// send IL
				player.client.sendPacket(ilPacket);
				
				il = player.account.getPlayerInventory().getItem("5");
				ilPacket = new InventoryItemPacket();
				ilPacket.item = il;

				// send IL
				player.client.sendPacket(ilPacket);
				
				il = player.account.getPlayerInventory().getItem("10");
				ilPacket = new InventoryItemPacket();
				ilPacket.item = il;

				// send IL
				player.client.sendPacket(ilPacket);
			}
			else if(twiggleItem.getTwiggleComponent().twiggleWorkParams.enlargedAreaIndex != null)
			{
				//its a expansion
				//TODO: Room expands
			}
			
			twiggleAccessor.clearTwiggleWork(twiggleInvId);
			this.success = true;
			player.client.sendPacket(this);

		} catch (Exception e) {
			e.printStackTrace();

			// failed to complete expansion
			this.success = false;
			client.sendPacket(this);
		}

		return true;
	}

}
