package org.asf.emuferal.players;

import org.asf.emuferal.accounts.EmuFeralAccount;
import org.asf.emuferal.data.XtWriter;
import org.asf.emuferal.enums.players.OnlineStatus;
import org.asf.emuferal.networking.smartfox.SmartfoxClient;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class Player {

	public SmartfoxClient client;
	public EmuFeralAccount account;

	public String activeLook;
	public String activeSanctuaryLook;
	public boolean sanctuaryPreloadCompleted = false;

	public int pendingLookDefID = 8254;
	public String pendingLookID = null;

	public boolean roomReady = false;
	public boolean wasInChat = false;
	public int levelType = 0;
	public int levelID = 0;
	public int pendingLevelID = 0;

	public String pendingRoom = "0";
	public String room = null;

	public String respawn = null;
	public String lastLocation = null;

	public double lastPosX = 0;
	public double lastPosY = -1000;
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
			wr.writeInt(-1); // data prefix

			// Object creation parameters
			wr.writeString(account.getAccountID()); // World object ID
			wr.writeInt(852);
			wr.writeString(account.getAccountID()); // Owner ID

			// Object info
			wr.writeInt(0);
			wr.writeLong(System.currentTimeMillis() / 1000);
			wr.writeDouble(lastPosX);
			wr.writeDouble(lastPosY);
			wr.writeDouble(lastPosZ);
			wr.writeInt(0);
			wr.writeDouble(lastRotX);
			wr.writeDouble(lastRotY);
			wr.writeDouble(lastRotZ);
			wr.writeInt(0);
			wr.writeInt(0);
			wr.writeInt(0);
			wr.writeDouble(lastRotW);
			wr.writeInt(lastAction);

			// Look and name
			wr.writeString(lookObj.get("components").getAsJsonObject().get("AvatarLook").getAsJsonObject().get("info")
					.toString());
			wr.writeString(account.getDisplayName());
			wr.writeInt(0);
			wr.writeString(""); // data suffix

			player.client.sendPacket(wr.encode());
		}
	}
}
