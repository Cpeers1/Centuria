package org.asf.centuria.packets.xt.gameserver.avatareditor;

import java.io.IOException;

import org.asf.centuria.data.XtReader;
import org.asf.centuria.data.XtWriter;
import org.asf.centuria.networking.gameserver.GameServer;
import org.asf.centuria.networking.smartfox.SmartfoxClient;
import org.asf.centuria.packets.xt.IXtPacket;
import org.asf.centuria.players.Player;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class AvatarEditorSelectLook implements IXtPacket<AvatarEditorSelectLook> {
	
	private static final String PACKET_ID = "als";

	private String lookID;

	@Override
	public AvatarEditorSelectLook instantiate() {
		return new AvatarEditorSelectLook();
	}

	@Override
	public String id() {
		return PACKET_ID;
	}

	@Override
	public void parse(XtReader reader) throws IOException {
		lookID = reader.read();
	}

	@Override
	public void build(XtWriter writer) throws IOException {
		writer.writeInt(-1); // Data prefix

		writer.writeString(lookID);

		writer.writeString(""); // Data suffix
	}

	@Override
	public boolean handle(SmartfoxClient client) throws IOException {
		// Switch looks
		Player plr = (Player) client.container;

		// Log
		if (System.getProperty("debugMode") != null) {
			System.out.println("[AVATAREDITOR] [SELECTLOOK]  Client to server (look: " + lookID + ")");
		}

		// Save the pending look ID
		plr.pendingLookID = lookID;
		plr.activeLook = plr.pendingLookID;

		// Respond with switch packet
		plr.client.sendPacket(this);

		// Save active look
		plr.account.setActiveLook(plr.activeLook);

		// Assign the defID
		JsonArray items = plr.account.getPlayerInventory().getItem("avatars").getAsJsonArray();
		JsonObject lookObj = null;
		for (JsonElement itm : items) {
			if (itm.isJsonObject()) {
				JsonObject obj = itm.getAsJsonObject();
				if (obj.get("id").getAsString().equals(plr.activeLook)) {
					lookObj = obj;
					break;
				}
			}
		}

		plr.pendingLookDefID = 8254;
		if (lookObj != null) {
			plr.pendingLookDefID = lookObj.get("defId").getAsInt();
		}

		// Sync
		GameServer srv = (GameServer) client.getServer();
		for (Player player : srv.getPlayers()) {
			if (plr.room != null && player.room != null && player.room.equals(plr.room) && player != plr) {
				plr.syncTo(player);
			}
		}

		return true;
	}

}
