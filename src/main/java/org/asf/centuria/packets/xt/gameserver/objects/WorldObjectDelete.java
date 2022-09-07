package org.asf.centuria.packets.xt.gameserver.objects;

import java.io.IOException;

import org.asf.centuria.data.XtReader;
import org.asf.centuria.data.XtWriter;
import org.asf.centuria.networking.smartfox.SmartfoxClient;
import org.asf.centuria.packets.xt.IXtPacket;

public class WorldObjectDelete implements IXtPacket<WorldObjectDelete> {

	private static final String PACKET_ID = "od";

	public String objectId;

	@Override
	public WorldObjectDelete instantiate() {
		return new WorldObjectDelete(objectId);
	}

	@Override
	public String id() {
		return PACKET_ID;
	}

	@Override
	public void parse(XtReader reader) throws IOException {
		// There is no inbound for this packet type.
	}

	@Override
	public void build(XtWriter writer) throws IOException {
		writer.writeInt(DATA_PREFIX);

		writer.writeString(objectId); //Simple packet, just sends a delete cmd with an ID

		writer.writeString(DATA_SUFFIX);
	}

	@Override
	public boolean handle(SmartfoxClient client) throws IOException {
		// There is no inbound for this packet type.
		return true;
	}

    public WorldObjectDelete(String objectId)
    {
        super();
        this.objectId = objectId;
    }

}
