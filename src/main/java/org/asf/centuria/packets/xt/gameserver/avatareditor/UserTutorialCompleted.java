package org.asf.centuria.packets.xt.gameserver.avatareditor;

import java.io.IOException;

import org.asf.centuria.data.XtReader;
import org.asf.centuria.data.XtWriter;
import org.asf.centuria.entities.players.Player;
import org.asf.centuria.networking.smartfox.SmartfoxClient;
import org.asf.centuria.packets.xt.IXtPacket;
import org.asf.centuria.packets.xt.gameserver.inventory.InventoryItemPacket;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class UserTutorialCompleted implements IXtPacket<UserTutorialCompleted> {

	private static final String PACKET_ID = "utc";

	private String lookName;
	private String lookData;

	@Override
	public UserTutorialCompleted instantiate() {
		return new UserTutorialCompleted();
	}

	@Override
	public String id() {
		return PACKET_ID;
	}

	@Override
	public void parse(XtReader reader) throws IOException {
		lookName = reader.read();
		reader.read();
		lookData = reader.read();
	}

	@Override
	public void build(XtWriter writer) throws IOException {
	}

	@Override
	public boolean handle(SmartfoxClient client) throws IOException {
		// Tutorial completed
		Player plr = (Player) client.container;

		// Parse avatar
		JsonObject lookData = JsonParser.parseString(this.lookData).getAsJsonObject();

		// Save look file to look database
		plr.activeLook = plr.pendingLookID;

		// Mark tutorial as finished
		plr.account.finishedTutorial();

		// Save avatar to inventory
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

		// Send response
		client.sendPacket("%xt%utc%-1%true%" + plr.activeLook + "%%");

		return true;
	}

}
