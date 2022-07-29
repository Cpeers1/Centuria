package org.asf.emuferal.packets.xt.gameserver.minigames;

import java.io.IOException;

import org.asf.emuferal.data.XtReader;
import org.asf.emuferal.data.XtWriter;
import org.asf.emuferal.minigames.TwiggleBuilders;
import org.asf.emuferal.networking.smartfox.SmartfoxClient;
import org.asf.emuferal.packets.xt.IXtPacket;
import org.asf.emuferal.players.Player;
import org.asf.emuferal.packets.xt.gameserver.world.JoinRoom;

public class MinigameJoin implements IXtPacket<MinigameJoin> {

	private static final String PACKET_ID = "mj";
	
    public int MinigameID;
	private boolean isMinigameSupported;
	
	@Override
	public MinigameJoin instantiate() {
		return new MinigameJoin();
	}

	@Override
	public String id() {
		return PACKET_ID;
	}

	@Override
	public void parse(XtReader reader) throws IOException {
        MinigameID = reader.readInt();
	}

	@Override
	public void build(XtWriter writer) throws IOException {
	}

	@Override
	public boolean handle(SmartfoxClient client) throws IOException {
		
		// Log
		if (System.getProperty("debugMode") != null) {
			System.out.println("[MINIGAME] [JOIN]  Client to server (MinigameID: " + MinigameID + ")");
		}

		Player plr = (Player) client.container;
		
		switch (MinigameID){
			case 4111: {
				isMinigameSupported = true;
				TwiggleBuilders.OnJoin(plr);
				break;
			}
			default: {
				isMinigameSupported = false;
				break;
			}
		}
		
		if (isMinigameSupported == true){
			//Set previous
			plr.previousLevelID = plr.levelID;
			plr.previousLevelType = plr.levelType;
			
			// Assign room
			plr.roomReady = true;
			plr.levelID = MinigameID;
			plr.room = "room_" + MinigameID;
			plr.levelType = 1;
		}
		

		// Send response
		JoinRoom join = new JoinRoom();
		join.success = isMinigameSupported;
		join.levelType = 1;
		join.levelID = MinigameID;
		client.sendPacket(join);

		MinigameStart start = new MinigameStart();
		client.sendPacket(start);

		return true;
	}

}
