package org.asf.centuria.tools.legacyclienttools;

import java.io.IOException;

import org.asf.centuria.data.XtReader;
import org.asf.centuria.data.XtWriter;
import org.asf.centuria.networking.smartfox.SmartfoxClient;
import org.asf.centuria.packets.xt.IXtPacket;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class ProxiedObjectInfoPacket implements IXtPacket<ProxiedObjectInfoPacket> {

	private String msg;

	@Override
	public ProxiedObjectInfoPacket instantiate() {
		return new ProxiedObjectInfoPacket();
	}

	@Override
	public String id() {
		return "oi";
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
		XtReader rd = new XtReader(msg);
		TranslatorGameServer server = (TranslatorGameServer) client.getServer();
		if (!server.isLocalClient(client)) {
			rd.read();

			// UUID
			String uuid = rd.read();
			int defID = rd.readInt();
			String owner = rd.read();

			// Mode
			int mode = rd.readInt();

			// Timestamp
			long time = rd.readLong();

			// Coordinates
			double x = rd.readDouble();
			double y = rd.readDouble();
			double z = rd.readDouble();

			// Rotation
			double rx = rd.readDouble();
			double ry = rd.readDouble();
			double rz = rd.readDouble();
			double rw = rd.readDouble();

			// Direction
			double dx = rd.readDouble();
			double dy = rd.readDouble();
			double dz = rd.readDouble();
			float speed = rd.readFloat();

			// Action
			int action = rd.readInt();

			// Look
			String look = rd.read();
			String name = rd.read();
			int unk = rd.readInt();

			// Check avatar
			if (defID == 852) {
				// Avatar
				JsonObject avaInfo = JsonParser.parseString(look).getAsJsonObject();
				avaInfo.keySet().forEach(t -> {
					if (t.contains("Color") && t.endsWith("HSV")) {
						translateColor(avaInfo, t);
					}
				});
				if (avaInfo.has("bodyParts") && avaInfo.get("bodyParts").isJsonArray()) {
					JsonArray parts = avaInfo.get("bodyParts").getAsJsonArray();
					for (JsonElement part : parts) {
						if (part.isJsonObject()) {
							JsonObject partO = part.getAsJsonObject();
							if (partO.has("_decalEntries") && partO.get("_decalEntries").isJsonArray()) {
								JsonArray decals = partO.get("_decalEntries").getAsJsonArray();
								for (JsonElement decal : decals) {
									if (decal.isJsonObject()) {
										JsonObject decalO = decal.getAsJsonObject();
										decalO.keySet().forEach(t -> {
											if (t.contains("color") && t.endsWith("HSV")) {
												translateColor(decalO, t);
											}
										});
									}
								}
							}
						}
					}
				}
				look = avaInfo.toString();
			}

			// Write basics
			XtWriter wr = new XtWriter();
			wr.writeString("oi");
			wr.writeInt(-1);
			wr.writeString(uuid);
			wr.writeInt(defID);
			wr.writeString(owner);
			wr.writeInt(mode);
			wr.writeLong(time);

			// Coordinates
			wr.writeDouble(x);
			wr.writeDouble(y);
			wr.writeDouble(z);

			// Rotation
			wr.writeDouble(rx - 180);
			wr.writeDouble(ry);
			wr.writeDouble(rz);
			wr.writeDouble(rw + 180);

			// Direction
			wr.writeDouble(dx);
			wr.writeDouble(dy);
			wr.writeDouble(dz);

			// Speed
			wr.writeFloat(speed);

			// Action
			wr.writeInt(action);

			// Data
			wr.writeString(look);
			wr.writeString(name);
			wr.writeInt(unk);

			wr.writeString("");
			((SmartfoxClient) client.container).sendPacket(wr.encode());

			return true;
		}
		return false;
	}

	private void translateColor(JsonObject obj, String key) {
		if (obj.has(key) && obj.get(key).isJsonObject()) {
			JsonObject colorInfo = obj.get(key).getAsJsonObject();
			if (colorInfo.has("_hsv")) {
				String hsv = colorInfo.get("_hsv").getAsString();
				String[] hsvs = hsv.split(",");
				if (hsvs.length == 3) {
					if (hsvs[0].matches("^[0-9]+$") && hsvs[1].matches("^[0-9]+$") && hsvs[2].matches("^[0-9]+$")) {
						if (colorInfo.has("_h"))
							colorInfo.remove("_h");
						if (colorInfo.has("_s"))
							colorInfo.remove("_s");
						if (colorInfo.has("_v"))
							colorInfo.remove("_v");
						colorInfo.addProperty("_h", Double.toString((double)Integer.parseInt(hsvs[0]) / 10000d));
						colorInfo.addProperty("_s", Double.toString((double)Integer.parseInt(hsvs[1]) / 10000d));
						colorInfo.addProperty("_v", Double.toString((double)Integer.parseInt(hsvs[2]) / 10000d));
						colorInfo.remove("_hsv");
					}
				}
			}
		}
	}

}
