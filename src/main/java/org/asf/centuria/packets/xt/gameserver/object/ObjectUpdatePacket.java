package org.asf.centuria.packets.xt.gameserver.object;

import java.io.IOException;

import org.asf.centuria.Centuria;
import org.asf.centuria.data.XtReader;
import org.asf.centuria.data.XtWriter;
import org.asf.centuria.entities.generic.Quaternion;
import org.asf.centuria.entities.generic.Vector3;
import org.asf.centuria.entities.players.Player;
import org.asf.centuria.networking.gameserver.GameServer;
import org.asf.centuria.networking.smartfox.SmartfoxClient;
import org.asf.centuria.packets.xt.IXtPacket;

public class ObjectUpdatePacket implements IXtPacket<ObjectUpdatePacket> {

	private static final String PACKET_ID = "ou";

	// Main fields
	public String id;
	public long time;
	public int mode;

	// UUID-mode
	public String targetUUID;

	// Coordiante-mode
	public Vector3 position = new Vector3();
	public Vector3 heading = new Vector3();
	public Quaternion rotation = new Quaternion();
	public float speed;

	// Action
	public int action;

	@Override
	public ObjectUpdatePacket instantiate() {
		return new ObjectUpdatePacket();
	}

	@Override
	public String id() {
		return PACKET_ID;
	}

	@Override
	public void parse(XtReader reader) throws IOException {
		mode = reader.readInt();

		// Check mode
		switch (mode) {
		case 0:
			// Initial
		case 2: {
			// Move
			// Same as initial

			// Read position
			position.x = reader.readDouble();
			position.y = reader.readDouble();
			position.z = reader.readDouble();

			// Read heading
			heading.x = reader.readDouble();
			heading.y = reader.readDouble();
			heading.z = reader.readDouble();

			// Read rotation
			rotation.x = reader.readDouble();
			rotation.y = reader.readDouble();
			rotation.z = reader.readDouble();
			rotation.w = reader.readDouble();

			// Read speed
			speed = reader.readFloat();
			break;
		}
		case 4: {
			// Action

			// Read position
			position.x = reader.readDouble();
			position.y = reader.readDouble();
			position.z = reader.readDouble();

			// Read heading
			heading.x = reader.readDouble();
			heading.y = reader.readDouble();
			heading.z = reader.readDouble();

			// Read rotation
			rotation.x = reader.readDouble();
			rotation.y = reader.readDouble();
			rotation.z = reader.readDouble();
			rotation.w = reader.readDouble();

			// Read speed
			speed = reader.readFloat();

			// Read action
			action = reader.readInt();
			break;
		}
		case 5: {
			// TP
			targetUUID = reader.read();
			break;
		}
		default:
			// Print out world object update call..
			if (Centuria.debugMode) {
				System.out.println("[OBJECTS] [OU] Unhandled Mode " + mode + ": " + reader.readRemaining());
			}
		}

	}

	@Override
	public void build(XtWriter writer) throws IOException {
		writer.writeInt(DATA_PREFIX); // Data prefix

		// Main fields
		writer.writeString(id);
		writer.writeInt(mode);
		writer.writeLong(time);

		// Write position
		writer.writeDouble(position.x);
		writer.writeDouble(position.y);
		writer.writeDouble(position.z);

		// Then WW's fucking nightmare: DONT ASK BC IDK HOW BUT IT WORKS
		writer.writeDouble(heading.x);
		writer.writeDouble(rotation.y);
		writer.writeDouble(rotation.z);
		writer.writeDouble(rotation.w);
		writer.writeDouble(heading.y);
		writer.writeDouble(heading.z);
		writer.writeDouble(rotation.x);
		writer.writeFloat(speed);

		// Write action
		writer.writeInt(action);

		writer.writeString(DATA_SUFFIX);
	}

	@Override
	public boolean handle(SmartfoxClient client) throws IOException {
		// Object update
		Player plr = (Player) client.container;
		if (plr.room == null)
			return true;

		// Add fields
		id = plr.account.getAccountID();
		time = System.currentTimeMillis() / 1000;

		// UUID-based teleport
		if (mode == 5) {
			boolean success = false;

			// First attempt to find a player with the ID
			for (Player player : ((GameServer) client.getServer()).getPlayers()) {
				if (player.account.getAccountID().equals(targetUUID)) {
					// Load coordinates
					success = true;
					position.x = player.lastPos.x;
					position.y = player.lastPos.y;
					position.z = player.lastPos.z;
					rotation.w = player.lastRot.w;
					rotation.x = player.lastRot.x;
					rotation.y = player.lastRot.y;
					rotation.z = player.lastRot.z;
					break;
				}
			}

			// Cancel if not found
			if (!success)
				return true;
		}

		// Save position
		plr.lastHeading = heading;
		plr.lastPos = position;
		plr.lastRot = rotation;
		plr.lastAction = action;

		// Broadcast sync
		GameServer srv = (GameServer) client.getServer();
		for (Player player : srv.getPlayers()) {
			if (player != plr && player.room != null && player.room.equals(plr.room)
					&& (!plr.ghostMode || player.hasModPerms) && !player.disableSync
					&& (!plr.syncBlockedPlayers.contains(player.account.getAccountID()) || player.hasModPerms)) {
				player.client.sendPacket(this);
			}
		}

		return true;
	}

}
