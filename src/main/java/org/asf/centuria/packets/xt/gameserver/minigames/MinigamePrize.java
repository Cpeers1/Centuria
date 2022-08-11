package org.asf.centuria.packets.xt.gameserver.minigames;

import java.io.IOException;

import org.asf.centuria.data.XtReader;
import org.asf.centuria.data.XtWriter;
import org.asf.centuria.networking.gameserver.GameServer;
import org.asf.centuria.networking.smartfox.SmartfoxClient;
import org.asf.centuria.packets.xt.IXtPacket;
import org.asf.centuria.players.Player;

public class MinigamePrize implements IXtPacket<MinigamePrize> {

	private static final String PACKET_ID = "mpz";

    public String ItemDefId;
	public int ItemCount;
	public boolean Given;
	public int PrizeIndex1;
	public int PrizeIndex2;

	@Override
	public MinigamePrize instantiate() {
		return new MinigamePrize();
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
            writer.writeString(ItemDefId);
            writer.writeInt(ItemCount);
            writer.writeInt(Given ? 1 : 0);
            writer.writeInt(PrizeIndex1);
            writer.writeInt(PrizeIndex2);
            writer.writeString(""); // Data suffix
	}

	@Override
	public boolean handle(SmartfoxClient client) throws IOException {
		return true;
	}

}
