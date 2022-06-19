package org.asf.emuferal.packets.xt.gameserver.sanctuaries;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.asf.emuferal.data.XtReader;
import org.asf.emuferal.data.XtWriter;
import org.asf.emuferal.entities.objects.sanctuaries.RoomInfoObject;
import org.asf.emuferal.entities.objects.sanctuaries.SancObjectInfo;
import org.asf.emuferal.networking.gameserver.GameServer;
import org.asf.emuferal.networking.smartfox.SmartfoxClient;
import org.asf.emuferal.packets.xt.IXtPacket;
import org.asf.emuferal.packets.xt.gameserver.inventory.InventoryItemPacket;
import org.asf.emuferal.players.Player;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class SanctuaryUpdatePacket implements IXtPacket<SanctuaryUpdatePacket> {

	public class UpdateSancObjectItem {
		public String objectId;
		public SancObjectInfo objectInfo;
		public JsonObject furnitureObject;
	}

	public boolean success;
	public List<UpdateSancObjectItem> additions = new ArrayList<UpdateSancObjectItem>();
	public List<RoomInfoObject> roomChanges = new ArrayList<RoomInfoObject>();
	public List<String> removals = new ArrayList<String>();

	@Override
	public String id() {
		return "ssu";
	}

	@Override
	public SanctuaryUpdatePacket instantiate() {
		return new SanctuaryUpdatePacket();
	}

	@Override
	public void parse(XtReader reader) throws IOException {

		// first number is how many ssu's there really are
		int numOfAdditions = reader.readInt();
		for (int i = 0; i < numOfAdditions; i++) {
			UpdateSancObjectItem item = new UpdateSancObjectItem();

			// next line is placementId
			item.objectId = reader.read();

			// then its x y z
			SancObjectInfo info = new SancObjectInfo();
			info.x = reader.readDouble();
			info.y = reader.readDouble();
			info.z = reader.readDouble();

			// then rot x y z w
			info.rotX = reader.readDouble();
			info.rotY = reader.readDouble();
			info.rotZ = reader.readDouble();
			info.rotW = reader.readDouble();

			// then it's uhm.. state
			info.gridId = reader.readInt();

			// OH
			// THIS AFTERWARDS IS PARENT ID LIKELY
			info.parentId = reader.read(); // PARENT ID (probably important)

			// THEN AFTER IS GRID ID
			info.state = reader.readInt();

			item.objectInfo = info;
			additions.add(item);
		}

		// Log
		if (System.getProperty("debugMode") != null) {
			if (numOfAdditions > 0) {
				System.out.println(
						"[SANCTUARY] [UPDATE] Client to server: " + numOfAdditions + " furniture additions...");
			}
		}

		int numOfRemovals = reader.readInt();
		for (int i = 0; i < numOfRemovals; i++) {
			// removals
			removals.add(reader.read());
		}

		// Log
		if (System.getProperty("debugMode") != null) {
			if (numOfRemovals > 0) {
				System.out
						.println("[SANCTUARY] [UPDATE] Client to server: " + numOfRemovals + " furniture removals...");
			}
		}

		int numOfRoomChanges = reader.readInt();
		for (int i = 0; i < numOfRoomChanges; i++) {
			// room changes
			roomChanges.add(new RoomInfoObject(JsonParser.parseString(reader.read()).getAsJsonObject()));
		}

		// Log
		if (System.getProperty("debugMode") != null) {
			if (numOfRoomChanges > 0) {
				System.out.println("[SANCTUARY] [UPDATE] Client to server: " + numOfRoomChanges + " room updates...");
			}
		}
	}

	@Override
	public void build(XtWriter writer) throws IOException {
		writer.writeInt(-1); // Data prefix

		// Success
		writer.writeBoolean(true);

		writer.writeInt(additions.size()); // number of item additions
		if (additions.size() != 0) {
			for (var item : additions) {
				writer.writeString(item.objectId);
			}
		}

		writer.writeInt(removals.size()); // number of removals
		if (removals.size() != 0) {
			for (var item : removals) {
				writer.writeString(item);
			}
		}

		writer.writeString(""); // Data suffix
	}

	@Override
	public boolean handle(SmartfoxClient client) throws IOException {
		// Switch sanctuary look
		Player plr = (Player) client.container;

		// updates
		for (var item : additions) {
			plr.account.getPlayerInventory().getSanctuaryAccessor().addSanctuaryObject(item.objectId, item.objectInfo,
					plr.activeSanctuaryLook);
		}

		// removals
		for (var item : removals) {
			plr.account.getPlayerInventory().getSanctuaryAccessor().removeSanctuaryObject(item,
					plr.activeSanctuaryLook);
		}

		// room updates

		plr.account.getPlayerInventory().getSanctuaryAccessor().updateSanctuaryRoomData(plr.activeSanctuaryLook,
				roomChanges.toArray(new RoomInfoObject[0]));

		// uhh yeah ok
		// send il and sanctuaryUpdatePacket response for the main player

		XtWriter writer = null;

		if (additions.size() > 0 || removals.size() > 0) {
			var il = plr.account.getPlayerInventory().getItem("201");
			var ilPacket = new InventoryItemPacket();
			ilPacket.item = il;

			writer = new XtWriter();
			ilPacket.build(writer);

			// send IL
			plr.client.sendPacket(writer.encode());
		}

		if (roomChanges.size() > 0) {
			var il = plr.account.getPlayerInventory().getItem("5");
			var ilPacket = new InventoryItemPacket();
			ilPacket.item = il;

			writer = new XtWriter();
			ilPacket.build(writer);

			// send IL
			plr.client.sendPacket(writer.encode());
		}

		// then do this packet

		writer = new XtWriter();
		this.build(writer);

		plr.client.sendPacket(writer.encode());

		sendObjectUpdatePackets(client);

		return true;
	}

	public void sendObjectUpdatePackets(SmartfoxClient client) {

		for (var update : additions) {
			var owner = (Player) client.container;
			var furnItem = owner.account.getPlayerInventory().getFurnitureAccessor().getFurnitureData(update.objectId);

			// now do an OI packet
			for (Player player : ((GameServer) client.getServer()).getPlayers()) {
				if (player.room.equals("sanctuary_" + owner.account.getAccountID())) {
					// Send packet
					XtWriter wr = new XtWriter();
					wr.writeString("oi");
					wr.writeInt(-1); // data prefix

					// Object creation parameters
					wr.writeString(update.objectId); // World object ID
					wr.writeInt(1751);
					wr.writeString(player.room.substring("sanctuary_".length())); // Owner ID

					// Object info
					wr.writeInt(0);
					wr.writeLong(System.currentTimeMillis() / 1000);
					wr.writeDouble(update.objectInfo.x);
					wr.writeDouble(update.objectInfo.y);
					wr.writeDouble(update.objectInfo.z);
					wr.writeDouble(update.objectInfo.rotX);
					wr.writeDouble(update.objectInfo.rotY);
					wr.writeDouble(update.objectInfo.rotZ);
					wr.writeDouble(update.objectInfo.rotW);
					wr.writeString("0%0%0%0.0%0%2"); // idk tbh

					// Only send json if its not the owner

					if (!player.account.getAccountID().equals(owner.account.getAccountID()))
						wr.writeString(furnItem.toString());
					wr.writeString(String.valueOf(update.objectInfo.gridId)); // grid
					wr.writeString(update.objectInfo.parentId); // parent item ??
					wr.writeInt(update.objectInfo.state); // state
					wr.writeString(""); // data suffix
					String pk = wr.encode();
					player.client.sendPacket(pk);

					// Log
					if (System.getProperty("debugMode") != null) {
						System.out.println("[SANCTUARY] [UPDATE] Server to client: load object (" + pk + ")");
					}
				}
			}
		}

		for (var removedItemId : removals) {
			var owner = (Player) client.container;

			// now do an OD packet
			for (Player player : ((GameServer) client.getServer()).getPlayers()) {
				if (player.room.equals("sanctuary_" + owner.account.getAccountID())) {
					// Send packet
					XtWriter wr = new XtWriter();
					wr.writeString("od");
					wr.writeInt(-1); // data prefix

					// Object creation parameters
					wr.writeString(removedItemId); // World object ID
					wr.writeString(""); // data suffix
					String pk = wr.encode();
					player.client.sendPacket(pk);

					// Log
					if (System.getProperty("debugMode") != null) {
						System.out.println("[SANCTUARY] [UPDATE] Server to client: Delete object (" + pk + ")");
					}
				}
			}
		}

		// TODO: What do I send for room changes to the client?

		for (var roomUpdate : this.roomChanges) {
			var owner = (Player) client.container;

			// now do an OI packet
			for (Player player : ((GameServer) client.getServer()).getPlayers()) {
				if (player.room.equals("sanctuary_" + owner.account.getAccountID())) {

				}
			}
		}
	}

}
