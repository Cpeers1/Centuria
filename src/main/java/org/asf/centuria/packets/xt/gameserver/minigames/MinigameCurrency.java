package org.asf.centuria.packets.xt.gameserver.minigames;

import java.io.IOException;

import org.asf.centuria.data.XtReader;
import org.asf.centuria.data.XtWriter;
import org.asf.centuria.networking.gameserver.GameServer;
import org.asf.centuria.networking.smartfox.SmartfoxClient;
import org.asf.centuria.packets.xt.IXtPacket;
import org.asf.centuria.players.Player;

public class MinigameCurrency implements IXtPacket<MinigameCurrency> {

	private static final String PACKET_ID = "mg";

    public int Currency;
    public int UNK1 = 1;
    public String UNK2 = "null";

	@Override
	public MinigameCurrency instantiate() {
		return new MinigameCurrency();
	}

	@Override
	public String id() {
		return PACKET_ID;
	}

	@Override
	public void parse(XtReader reader) throws IOException {
	}

	@Override
	public void build(XtWriter writer) throws IOException {
            writer.writeInt(-1); //padding
            writer.writeInt(Currency);
            writer.writeInt(UNK1);
            writer.writeString(UNK2);
            writer.writeString(""); // Data suffix
	}

	@Override
	public boolean handle(SmartfoxClient client) throws IOException {
		return true;
	}

}
