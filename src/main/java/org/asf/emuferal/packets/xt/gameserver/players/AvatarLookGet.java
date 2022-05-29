package org.asf.emuferal.packets.xt.gameserver.players;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.zip.GZIPOutputStream;

import org.asf.emuferal.accounts.AccountManager;
import org.asf.emuferal.accounts.EmuFeralAccount;
import org.asf.emuferal.data.XtReader;
import org.asf.emuferal.data.XtWriter;
import org.asf.emuferal.networking.smartfox.SmartfoxClient;
import org.asf.emuferal.packets.xt.IXtPacket;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class AvatarLookGet implements IXtPacket<AvatarLookGet> {

	private String accountID;

	@Override
	public AvatarLookGet instantiate() {
		return new AvatarLookGet();
	}

	@Override
	public String id() {
		return "alg";
	}

	@Override
	public void parse(XtReader reader) throws IOException {
		accountID = reader.readRemaining();
	}

	@Override
	public void build(XtWriter writer) throws IOException {
	}

	@Override
	public boolean handle(SmartfoxClient client) throws IOException {
		// Find avatar
		EmuFeralAccount account = AccountManager.getInstance().getAccount(accountID);
		if (account == null || !account.getPlayerInventory().containsItem("200"))
			return true; // Account not found

		JsonArray items = account.getPlayerInventory().getItem("avatars").getAsJsonArray();
		JsonObject lookObj = null;
		for (JsonElement itm : items) {
			if (itm.isJsonObject()) {
				JsonObject obj = itm.getAsJsonObject();
				if (obj.get("id").getAsString().equals(account.getActiveLook())) {
					lookObj = obj;
					break;
				}
			}
		}

		if (lookObj != null) {
			try {
				// Send response
				XtWriter writer = new XtWriter();
				writer.writeString("alg");
				writer.writeInt(-1); // data prefix
				// Compress and send look
				JsonObject look = lookObj.get("components").getAsJsonObject().get("AvatarLook").getAsJsonObject();
				ByteArrayOutputStream op = new ByteArrayOutputStream();
				GZIPOutputStream gz = new GZIPOutputStream(op);
				gz.write(look.toString().getBytes("UTF-8"));
				gz.close();
				op.close();
				writer.writeString(Base64.getEncoder().encodeToString(op.toByteArray()));
				writer.writeString(""); // data suffix
				client.sendPacket(writer.encode());
			} catch (IOException e) {
			}
		}

		return true;
	}

}
