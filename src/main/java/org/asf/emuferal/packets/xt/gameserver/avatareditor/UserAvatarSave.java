package org.asf.emuferal.packets.xt.gameserver.avatareditor;

import java.io.IOException;

import org.asf.emuferal.data.XtReader;
import org.asf.emuferal.data.XtWriter;
import org.asf.emuferal.networking.gameserver.GameServer;
import org.asf.emuferal.networking.smartfox.SmartfoxClient;
import org.asf.emuferal.packets.xt.IXtPacket;
import org.asf.emuferal.packets.xt.gameserver.inventory.InventoryItemPacket;
import org.asf.emuferal.players.Player;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class UserAvatarSave implements IXtPacket<UserAvatarSave> {

	private String lookID;
	private String lookName;
	private String lookData;

	@Override
	public UserAvatarSave instantiate() {
		return new UserAvatarSave();
	}

	@Override
	public String id() {
		return "alz";
	}

	@Override
	public void parse(XtReader reader) throws IOException {
		lookID = reader.read();
		lookName = reader.read();
		reader.read();
		lookData = reader.read();
	}

	@Override
	public void build(XtWriter writer) throws IOException {
	}

	@Override
	public boolean handle(SmartfoxClient client) throws IOException {
		// Avatar save
		Player plr = (Player) client.container;

		// Parse avatar
		JsonObject lookData = JsonParser.parseString(this.lookData).getAsJsonObject();

		// Save look file to look database
		plr.activeLook = plr.pendingLookID;

		// Save avatar to inventory
		JsonArray items = plr.account.getPlayerInventory().getItem("avatars").getAsJsonArray();
		JsonObject lookObj = null;
		boolean isPrimary = false;
		for (JsonElement itm : items) {
			if (itm.isJsonObject()) {
				JsonObject obj = itm.getAsJsonObject();
				if (obj.get("id").getAsString().equals(plr.activeLook)) {
					lookObj = obj;
					isPrimary = lookObj.has("PrimaryLook");
					break;
				}
			}
		}
		if (lookObj != null) {
			lookObj.remove("components");
			JsonObject ts = new JsonObject();
			ts.addProperty("ts", System.currentTimeMillis());
			JsonObject nm = new JsonObject();
			nm.addProperty("name", lookName);
			JsonObject al = new JsonObject();
			al.addProperty("gender", 0);
			al.add("info", lookData);
			JsonObject components = new JsonObject();
			if (isPrimary)
				components.add("PrimaryLook", new JsonObject());
			components.add("Timestamp", ts);
			components.add("AvatarLook", al);
			components.add("Name", nm);
			lookObj.add("components", components);
		}
		plr.account.getPlayerInventory().setItem("avatars", items);

		// Prevent double save
		plr.pendingLookID = null;
		plr.pendingLookDefID = 8254;

		// Update avatar object in client inventory
		InventoryItemPacket pkt = new InventoryItemPacket();
		pkt.item = items;
		client.sendPacket(pkt);

		// Sync
		GameServer srv = (GameServer) client.getServer();
		for (Player player : srv.getPlayers()) {
			if (plr.room != null && player.room != null && player.room.equals(plr.room) && player != plr) {
				plr.destroyAt(player);
				plr.syncTo(player);
			}
		}

		return true;
	}

}
