package org.asf.emuferal.networking.http.api;

import java.io.ByteArrayOutputStream;
import java.net.Socket;
import java.util.Base64;

import org.asf.emuferal.EmuFeral;
import org.asf.emuferal.accounts.AccountManager;
import org.asf.emuferal.accounts.EmuFeralAccount;
import org.asf.rats.ConnectiveHTTPServer;
import org.asf.rats.processors.HttpUploadProcessor;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class XPDetailsHandler extends HttpUploadProcessor {

	@Override
	public void process(String contentType, Socket client, String method) {
		try {
			// Parse body
			ByteArrayOutputStream strm = new ByteArrayOutputStream();
			ConnectiveHTTPServer.transferRequestBody(getHeaders(), getRequestBodyStream(), strm);
			byte[] body = strm.toByteArray();
			strm.close();

			// Parse JWT payload
			String token = this.getHeader("Authorization").substring("Bearer ".length());

			// Verify signature
			String verifyD = token.split("\\.")[0] + "." + token.split("\\.")[1];
			String sig = token.split("\\.")[2];
			if (!EmuFeral.verify(verifyD.getBytes("UTF-8"), Base64.getUrlDecoder().decode(sig))) {
				this.setResponseCode(401);
				this.setResponseMessage("Access denied");
				return;
			}

			// Verify expiry
			JsonObject jwtPl = JsonParser
					.parseString(new String(Base64.getUrlDecoder().decode(token.split("\\.")[1]), "UTF-8"))
					.getAsJsonObject();
			if (!jwtPl.has("exp") || jwtPl.get("exp").getAsLong() < System.currentTimeMillis() / 1000) {
				this.setResponseCode(401);
				this.setResponseMessage("Access denied");
				return;
			}

			// Parse body
			JsonArray req = JsonParser.parseString(new String(body, "UTF-8")).getAsJsonArray();

			// Build response
			JsonObject response = new JsonObject();
			JsonArray found = new JsonArray();
			for (JsonElement ele : req) {
				String uuid = ele.getAsString();

				// Locate account
				EmuFeralAccount acc = AccountManager.getInstance().getAccount(uuid);
				if (acc.getLevel().isLevelAvailable()) {
					JsonObject lvD = new JsonObject();
					JsonObject cLv = new JsonObject();
					cLv.addProperty("level", acc.getLevel().getCurrentXP());
					cLv.addProperty("required", acc.getLevel().getLevelupXPCount());
					cLv.addProperty("xp", acc.getLevel().getCurrentXP());
					lvD.add("current_level", lvD);
					lvD.addProperty("total_xp", acc.getLevel().getTotalXP());
					lvD.addProperty("uuid", uuid);
					found.add(lvD);
				}
			}

			// Send response
			response.add("found", found);
			response.add("not_found", req);
			setBody(response.toString());
		} catch (Exception e) {
			setResponseCode(500);
			setResponseMessage("Internal Server Error");
		}
	}

	@Override
	public HttpUploadProcessor createNewInstance() {
		return new XPDetailsHandler();
	}

	@Override
	public String path() {
		return "/xp/xp-details";
	}

}
