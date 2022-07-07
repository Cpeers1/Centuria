package org.asf.emuferal.packets.xt.gameserver.objects;

import java.io.IOException;

import org.asf.emuferal.data.XtReader;
import org.asf.emuferal.data.XtWriter;
import org.asf.emuferal.networking.smartfox.SmartfoxClient;
import org.asf.emuferal.packets.xt.IXtPacket;

public class WorldObjectDelete implements IXtPacket<WorldObjectDelete> {

	public String objectId;

	@Override
	public WorldObjectDelete instantiate() {
		return new WorldObjectDelete(objectId);
	}

	@Override
	public String id() {
		return "od";
	}

	@Override
	public void parse(XtReader reader) throws IOException {
		// There is no inbound for this packet type.
	}

	@Override
	public void build(XtWriter writer) throws IOException {
		writer.writeInt(-1);

		writer.writeString(objectId); //Simple packet, just sends a delete cmd with an ID

		writer.writeString("");
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
