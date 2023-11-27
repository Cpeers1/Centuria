package org.asf.centuria.tools.legacyclienttools.packets;

import java.io.IOException;
import org.asf.centuria.data.XtReader;
import org.asf.centuria.data.XtWriter;
import org.asf.centuria.networking.smartfox.SmartfoxClient;
import org.asf.centuria.packets.xt.IXtPacket;
import org.asf.centuria.tools.legacyclienttools.servers.TranslatorGameServer;
import org.asf.centuria.tools.legacyclienttools.translation.AvatarTranslators;

import com.google.gson.JsonParser;

public class ProxiedAvatarLookSavePacket implements IXtPacket<ProxiedAvatarLookSavePacket> {

	private String msg;

	@Override
	public ProxiedAvatarLookSavePacket instantiate() {
		return new ProxiedAvatarLookSavePacket();
	}

	@Override
	public String id() {
		return "alz";
	}

	@Override
	public void parse(XtReader reader) throws IOException {
		msg = reader.readRemaining();
	}

	@Override
	public void build(XtWriter writer) throws IOException {
		writer.writeString(msg);
	}

	@Override
	public boolean handle(SmartfoxClient client) throws IOException {
		TranslatorGameServer server = (TranslatorGameServer) client.getServer();
		if (server.isLocalClient(client)) {
			XtReader rd = new XtReader(msg);
			String id = rd.read();
			String name = rd.read();
			String unidentified = rd.read();
			String lookData = rd.read();

			// Downgrade format
			lookData = AvatarTranslators.translateAvatarInfoToBeta(JsonParser.parseString(lookData).getAsJsonObject())
					.toString();

			// Send
			XtWriter wr = new XtWriter();
			wr.writeString("alz");
			wr.writeInt(-1);
			wr.writeString(id);
			wr.writeString(name);
			wr.writeString(unidentified);
			wr.writeString(lookData);
			wr.writeString("");
			((SmartfoxClient) client.container).sendPacket(wr.encode());
			return true;
		}
		return false;
	}

}
