package org.asf.emuferal.packets.xt.gameserver.minigames;

import java.io.IOException;

import org.asf.emuferal.data.XtReader;
import org.asf.emuferal.data.XtWriter;
import org.asf.emuferal.networking.gameserver.GameServer;
import org.asf.emuferal.networking.smartfox.SmartfoxClient;
import org.asf.emuferal.packets.xt.IXtPacket;
import org.asf.emuferal.players.Player;

public class MinigameMessage implements IXtPacket<MinigameMessage> {

	private static final String PACKET_ID = "mm";

    public String command;
    public int ARG1;
    public int ARG2;

	@Override
	public MinigameMessage instantiate() {
		return new MinigameMessage();
	}

	@Override
	public String id() {
		return PACKET_ID;
	}

	@Override
	public void parse(XtReader reader) throws IOException {
        command = reader.read();

        if (command.equals("startLevel")){
            ARG1 = reader.readInt();
            ARG2 = reader.readInt();
        }
	}

	@Override
	public void build(XtWriter writer) throws IOException {
            writer.writeInt(-1); //padding
            writer.writeString(command);
            writer.writeInt(ARG1);
            writer.writeString(""); // Data suffix
	}

	@Override
	public boolean handle(SmartfoxClient client) throws IOException {

		// Log
		if (System.getProperty("debugMode") != null) {
			System.out.println(
					"[MINIGAME] [MESSAGE] Client to server (command: " + command + ")");
		}

		if (command.equals("startLevel")) {
            // Send response
            MinigameMessage message = new MinigameMessage();
            message.command = command;
            message.ARG1 = ARG1;
            client.sendPacket(message);
        }

		if (command.equals("endLevel")) {
			//fake prize
			MinigamePrize prize = new MinigamePrize();
			prize.ItemDefId = "2327";
			prize.ItemCount = 15;
			prize.Given = true;
			prize.PrizeIndex1 = 6566;
			prize.PrizeIndex2 = 0;
			client.sendPacket(prize);

			MinigamePrize prize1 = new MinigamePrize();
			prize1.ItemDefId = "2327";
			prize1.ItemCount = 15;
			prize1.Given = true;
			prize1.PrizeIndex1 = 6567;
			prize1.PrizeIndex2 = 1;
			client.sendPacket(prize1);

			MinigamePrize prize2 = new MinigamePrize();
			prize2.ItemDefId = "2327";
			prize2.ItemCount = 15;
			prize2.Given = true;
			prize2.PrizeIndex1 = 6572;
			prize2.PrizeIndex2 = 2;
			client.sendPacket(prize2);

			// Send response
            MinigameMessage message = new MinigameMessage();
            message.command = command;
            message.ARG1 = 30;
            client.sendPacket(message);
        }

		return true;
	}

}
