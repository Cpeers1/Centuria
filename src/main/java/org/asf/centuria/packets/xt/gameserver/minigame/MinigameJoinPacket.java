package org.asf.centuria.packets.xt.gameserver.minigame;

import java.io.IOException;

import org.asf.centuria.Centuria;
import org.asf.centuria.data.XtReader;
import org.asf.centuria.data.XtWriter;
import org.asf.centuria.entities.players.Player;
import org.asf.centuria.minigames.AbstractMinigame;
import org.asf.centuria.minigames.MinigameManager;
import org.asf.centuria.networking.smartfox.SmartfoxClient;
import org.asf.centuria.packets.xt.IXtPacket;
import org.asf.centuria.packets.xt.gameserver.room.RoomJoinPacket;

public class MinigameJoinPacket implements IXtPacket<MinigameJoinPacket> {

	private static final String PACKET_ID = "mj";

	public int minigameID;
	private boolean isMinigameSupported = false;

	@Override
	public MinigameJoinPacket instantiate() {
		return new MinigameJoinPacket();
	}

	@Override
	public String id() {
		return PACKET_ID;
	}

	@Override
	public void parse(XtReader reader) throws IOException {
		minigameID = reader.readInt();
	}

	@Override
	public void build(XtWriter writer) throws IOException {
	}

	@Override
	public boolean handle(SmartfoxClient client) throws IOException {

		// Log
		if (Centuria.debugMode) {
			System.out.println("[MINIGAME] [JOIN]  Client to server (MinigameID: " + minigameID + ")");
		}

		// Find and join minigame
		Player plr = (Player) client.container;
		AbstractMinigame game = MinigameManager.getGameFor(minigameID);
		if (game != null) {
			game = game.instantiate();
			plr.currentGame = game;
			isMinigameSupported = true;
			game.onJoin(plr);

			// Set previous
			plr.previousLevelID = plr.levelID;
			plr.previousLevelType = plr.levelType;

			// Assign room
			plr.roomReady = true;
			plr.levelID = minigameID;
			plr.room = "room_" + minigameID;
			plr.levelType = 1;
		}

		// Send response
		RoomJoinPacket join = new RoomJoinPacket();
		join.success = isMinigameSupported;
		join.levelType = 1;
		join.levelID = minigameID;
		client.sendPacket(join);

		// Start game
		MinigameStartPacket start = new MinigameStartPacket();
		client.sendPacket(start);

		return true;
	}

}
