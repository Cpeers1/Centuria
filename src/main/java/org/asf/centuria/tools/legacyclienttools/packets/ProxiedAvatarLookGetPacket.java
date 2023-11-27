package org.asf.centuria.tools.legacyclienttools.packets;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.asf.centuria.data.XtReader;
import org.asf.centuria.data.XtWriter;
import org.asf.centuria.networking.smartfox.SmartfoxClient;
import org.asf.centuria.packets.xt.IXtPacket;
import org.asf.centuria.tools.legacyclienttools.servers.TranslatorGameServer;
import org.asf.centuria.tools.legacyclienttools.translation.AvatarTranslators;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class ProxiedAvatarLookGetPacket implements IXtPacket<ProxiedAvatarLookGetPacket> {

	private String msg;

	@Override
	public ProxiedAvatarLookGetPacket instantiate() {
		return new ProxiedAvatarLookGetPacket();
	}

	@Override
	public String id() {
		return "alg";
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
		if (!server.isLocalClient(client)) {
			XtReader rd = new XtReader(msg);
			rd.read();
			String compressed = rd.read();

			// Read look
			GZIPInputStream gzipIn = new GZIPInputStream(
					new ByteArrayInputStream(Base64.getDecoder().decode(compressed)));
			String lookData = new String(gzipIn.readAllBytes(), "UTF-8");
			gzipIn.close();

			// Downgrade format
			JsonObject look = JsonParser.parseString(lookData).getAsJsonObject();
			AvatarTranslators.translateAvatarInfoToBeta(look.get("info").getAsJsonObject());
			lookData = look.toString();

			// Compress and send
			ByteArrayOutputStream op = new ByteArrayOutputStream();
			GZIPOutputStream gz = new GZIPOutputStream(op);
			gz.write(lookData.getBytes("UTF-8"));
			gz.close();
			op.close();
			compressed = Base64.getEncoder().encodeToString(op.toByteArray());
			((SmartfoxClient) client.container).sendPacket("%xt%alg%-1%" + compressed + "%");
			return true;
		}
		return false;
	}

}
