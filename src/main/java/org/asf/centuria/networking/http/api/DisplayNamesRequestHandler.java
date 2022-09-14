package org.asf.centuria.networking.http.api;

import java.io.ByteArrayOutputStream;
import java.net.Socket;
import java.util.Base64;
import java.util.UUID;

import org.asf.centuria.Centuria;
import org.asf.centuria.accounts.AccountManager;
import org.asf.centuria.accounts.CenturiaAccount;
import org.asf.rats.ConnectiveHTTPServer;
import org.asf.rats.processors.HttpUploadProcessor;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class DisplayNamesRequestHandler extends HttpUploadProcessor {

	private static String NIL_UUID = new UUID(0, 0).toString();

	@Override
	public void process(String contentType, Socket client, String method) {
		try {
			Centuria.logger.info("API CALL: " + getRequest().path);
			// Parse body
			ByteArrayOutputStream strm = new ByteArrayOutputStream();
			ConnectiveHTTPServer.transferRequestBody(getHeaders(), getRequestBodyStream(), strm);
			byte[] body = strm.toByteArray();
			strm.close();

			// Load manager
			AccountManager manager = AccountManager.getInstance();

			// Parse JWT payload
			String token = this.getHeader("Authorization").substring("Bearer ".length());

			// Verify signature
			String verifyD = token.split("\\.")[0] + "." + token.split("\\.")[1];
			String sig = token.split("\\.")[2];
			if (!Centuria.verify(verifyD.getBytes("UTF-8"), Base64.getUrlDecoder().decode(sig))) {
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
			JsonObject req = JsonParser.parseString(new String(body, "UTF-8")).getAsJsonObject();

			// Send response
			JsonObject response = new JsonObject();
			JsonArray found = new JsonArray();
			JsonArray unrecognized = new JsonArray();
			for (JsonElement uuid : req.get("uuids").getAsJsonArray()) {
				// Find account
				String id = uuid.getAsString();
				if (id.equals(NIL_UUID)) {
					JsonObject d = new JsonObject();
					d.addProperty("display_name", "[Centuria Server]");
					d.addProperty("uuid", id);
					found.add(d);
					continue;
				}
				CenturiaAccount acc = manager.getAccount(id);
				if (acc != null) {
					JsonObject d = new JsonObject();
					d.addProperty("display_name", acc.getDisplayName());
					d.addProperty("uuid", id);
					found.add(d);
				} else {
					unrecognized.add(id);
				}
			}

			response.add("found", found);
			response.add("not_found", (unrecognized.size() == 0 ? null : unrecognized));
			setBody(response.toString());
		} catch (Exception e) {
			setResponseCode(500);
			setResponseMessage("Internal Server Error");
			Centuria.logger.error(getRequest().path + " failed: 500: Internal Server Error", e);
		}
	}

	@Override
	public HttpUploadProcessor createNewInstance() {
		return new DisplayNamesRequestHandler();
	}

	@Override
	public String path() {
		return "/i/display_names";
	}

}
