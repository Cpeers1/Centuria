package org.asf.centuria.packets.xt.gameserver.inventory;

import java.io.IOException;

import org.asf.centuria.Centuria;
import org.asf.centuria.data.XtReader;
import org.asf.centuria.data.XtWriter;
import org.asf.centuria.entities.inspiration.InspirationCombineResult;
import org.asf.centuria.entities.players.Player;
import org.asf.centuria.networking.smartfox.SmartfoxClient;
import org.asf.centuria.packets.xt.IXtPacket;

public class InventoryItemInspirationCombinePacket implements IXtPacket<InventoryItemInspirationCombinePacket> {

	private static final String PACKET_ID = "iic";

	public int[] inspirationIds;
	public InspirationCombineResult result;

	@Override
	public InventoryItemInspirationCombinePacket instantiate() {
		return new InventoryItemInspirationCombinePacket();
	}

	@Override
	public String id() {
		return PACKET_ID;
	}

	@Override
	public void parse(XtReader reader) {
		int inspirationCount = reader.readInt();

		inspirationIds = new int[inspirationCount];
		for (int i = 0; i < inspirationCount; i++) {
			inspirationIds[i] = reader.readInt();
		}
	}

	@Override
	public void build(XtWriter writer) throws IOException {
		writer.writeInt(-1); // Data prefix

		writer.writeInt(result.combineStatus.value);
		writer.writeInt(result.enigmaDefId);

		writer.writeString(""); // Empty suffix
	}

	@Override
	public boolean handle(SmartfoxClient client) throws IOException {
		Player plr = (Player) client.container;

		// Log
		if (Centuria.debugMode) {
			String ids = "";

			for (var id : inspirationIds) {
				ids += id + ", ";
			}

			ids = ids.substring(0, ids.length() - 2);
			System.out.println("[INVENTORY] [UPDATE]  Client to server: Combine Inspirations using ids" + ids);
		}

		result = plr.account.getPlayerInventory().getInspirationAccessor().combineInspirations(inspirationIds, plr);

		// build packet
		plr.client.sendPacket(this);

		if (Centuria.debugMode) {
			System.out.println("[INVENTORY] [UPDATE]  Server to client: " + this.build());
		}

		return true;
	}
}
