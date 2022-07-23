package org.asf.emuferal.players;

import java.util.HashMap;

import org.asf.emuferal.accounts.EmuFeralAccount;
import org.asf.emuferal.entities.generic.Quaternion;
import org.asf.emuferal.entities.generic.Vector3;
import org.asf.emuferal.entities.generic.Velocity;
import org.asf.emuferal.entities.objects.WorldObjectMoveNodeData;
import org.asf.emuferal.entities.objects.WorldObjectPositionInfo;
import org.asf.emuferal.enums.actors.ActorActionType;
import org.asf.emuferal.enums.objects.WorldObjectMoverNodeType;
import org.asf.emuferal.networking.smartfox.SmartfoxClient;
import org.asf.emuferal.packets.xt.gameserver.objects.WorldObjectDelete;
import org.asf.emuferal.packets.xt.gameserver.players.PlayerWorldObjectInfo;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class Player {

	public SmartfoxClient client;
	public EmuFeralAccount account;

	public String activeLook;
	public String activeSanctuaryLook;
	public boolean sanctuaryPreloadCompleted = false;

	public HashMap<String, Long> respawnItems = new HashMap<String, Long>();

	public int pendingLookDefID = 8254;
	public String pendingLookID = null;

	public boolean roomReady = false;
	public boolean wasInChat = false;
	public int levelType = 0;
	public int previousLevelType = 0;
	public int levelID = 0;
	public int pendingLevelID = 0;
	public int previousLevelID = 0;

	public String pendingRoom = "0";
	public String room = null;

	public String respawn = null;
	public String lastLocation = null;

	//TODO: Clean up into vector3 type.
	public double lastPosX = 0;
	public double lastPosY = -1000;
	public double lastPosZ = 0;

	//TODO: Clean up into quaternion type.
	public double lastRotW = 0;
	public double lastRotX = 0;
	public double lastRotY = 0;
	public double lastRotZ = 0;

	public int lastAction = 0;
	
	// Teleports
	public String teleportDestination;
	public Vector3 targetPos;
	public Quaternion targetRot;

	public void destroyAt(Player player) {
		// Delete character
		WorldObjectDelete packet = new WorldObjectDelete(account.getAccountID());
		player.client.sendPacket(packet);
		lastAction = 0;
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
			PlayerWorldObjectInfo packet = new PlayerWorldObjectInfo();

			// Object creation parameters
			packet.id = account.getAccountID();
			packet.defId = 852; //TODO: Move to static final (const)
			packet.ownerId = account.getAccountID();

			packet.lastMove = new WorldObjectMoveNodeData();
			packet.lastMove.actorActionType = ActorActionType.Respawn; //TODO: Is this the right actor action type for a player that's spawning in?
			packet.lastMove.serverTime = System.currentTimeMillis() / 1000;
			packet.lastMove.positionInfo = new WorldObjectPositionInfo(lastPosX, lastPosY, lastPosZ, lastRotX, lastRotY, lastRotZ, lastRotW);
			packet.lastMove.velocity = new Velocity();
			packet.lastMove.nodeType = WorldObjectMoverNodeType.InitPosition;

			// Look and name
			packet.look = lookObj.get("components").getAsJsonObject().get("AvatarLook").getAsJsonObject().get("info").getAsJsonObject();
			packet.displayName = account.getDisplayName();
			packet.unknownValue = 0; //TODO: What is this??

			player.client.sendPacket(packet);
		}
	}
}
