package org.asf.emuferal.players;

import org.asf.emuferal.accounts.EmuFeralAccount;
import org.asf.emuferal.data.XtWriter;
import org.asf.emuferal.networking.smartfox.SmartfoxClient;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class Player {

	public SmartfoxClient client;
	public EmuFeralAccount account;

	public String activeLook;
	public String activeSanctuaryLook;

	public int pendingLookDefID = 8254;
	public String pendingLookID = null;
	public String room = null;

	public String respawn = null;
	public String lastLocation = null;

	public double lastPosX = 0;
	public double lastPosY = 0;
	public double lastPosZ = 0;

	public double lastRotW = 0;
	public double lastRotX = 0;
	public double lastRotY = 0;
	public double lastRotZ = 0;
	public int lastAction = 0;

	public void destroyAt(Player player) {
		// Delete character
		XtWriter wr = new XtWriter();
		wr.writeString("od");
		wr.writeInt(-1);
		wr.writeString(account.getAccountID());
		wr.writeString("");
		lastAction = 0;
		player.client.sendPacket(wr.encode());
	}

	public void syncTo(Player player) {
		// Find avatar
		JsonArray items = account.getPlayerInventory().getItem("avatars").getAsJsonArray();
		JsonObject lookObj = null;
		for (JsonElement itm : items) {
			if (itm.isJsonObject()) {
				JsonObject obj = itm.getAsJsonObject();
				if (obj.get("id").getAsString().equals(activeLook)) {
					lookObj = obj;
					break;
				}
			}
		}

		if (lookObj != null) {
			// Spawn player
			XtWriter wr = new XtWriter();
			wr.writeString("oi");
			wr.writeInt(-1);
			wr.writeString(account.getAccountID());
			wr.writeInt(852);
			wr.writeString(activeLook);
			wr.writeInt(account.getAccountNumericID());
			wr.writeLong(System.currentTimeMillis() / 1000);
			wr.writeString((lastLocation == null ? respawn : lastLocation));
			wr.writeString("0%0%0%0.0%0");
			wr.writeString(lookObj.get("components").getAsJsonObject().get("AvatarLook").getAsJsonObject().get("info")
					.toString());
			wr.writeString(account.getDisplayName());
			wr.writeInt(0);
			wr.writeString("");
			player.client.sendPacket(wr.encode());

			// Sync
			XtWriter pk = new XtWriter();
			pk.writeString("ou");
			pk.writeInt(-1); // Data prefix
			pk.writeString(account.getAccountID());
			pk.writeInt(4);
			pk.writeLong(System.currentTimeMillis() / 1000);
			pk.writeDouble(lastPosX);
			pk.writeDouble(lastPosY);
			pk.writeDouble(lastPosZ);
			pk.writeString("0");
			pk.writeDouble(lastRotX);
			pk.writeDouble(lastRotY);
			pk.writeDouble(lastRotZ);
			pk.writeString("0");
			pk.writeString("0");
			pk.writeString("0");
			pk.writeDouble(lastRotW);
			pk.writeInt(lastAction);
			pk.writeString(""); // Data suffix
			player.client.sendPacket(pk.encode());
		}
	}

}
