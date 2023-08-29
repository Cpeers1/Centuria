package org.asf.centuria.networking.http.api;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.UUID;

import org.asf.centuria.Centuria;
import org.asf.centuria.accounts.AccountManager;
import org.asf.centuria.accounts.CenturiaAccount;
import org.asf.connective.RemoteClient;
import org.asf.connective.processors.HttpPushProcessor;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class DisplayNamesRequestHandler extends HttpPushProcessor {

	private static String NIL_UUID = new UUID(0, 0).toString();

	@Override
	public void process(String path, String method, RemoteClient client, String contentType) throws IOException {
		try {
			// Parse body
			ByteArrayOutputStream strm = new ByteArrayOutputStream();
			getRequest().transferRequestBody(strm);
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
				this.setResponseStatus(401, "Unauthorized");
				return;
			}

			// Verify expiry
			JsonObject jwtPl = JsonParser
					.parseString(new String(Base64.getUrlDecoder().decode(token.split("\\.")[1]), "UTF-8"))
					.getAsJsonObject();
			if (!jwtPl.has("exp") || jwtPl.get("exp").getAsLong() < System.currentTimeMillis() / 1000) {
				this.setResponseStatus(401, "Unauthorized");
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
				} else if (id.startsWith("plaintext:")) {
					JsonObject d = new JsonObject();
					d.addProperty("display_name", id.substring("plaintext:".length()));
					d.addProperty("uuid", id);
					found.add(d);
				} else {
					unrecognized.add(id);
				}
			}

			response.add("found", found);
			response.add("not_found", (unrecognized.size() == 0 ? null : unrecognized));
			setResponseContent("text/json", response.toString());
		} catch (Exception e) {
			setResponseStatus(500, "Internal Server Error");
			Centuria.logger.error(getRequest().getRequestPath() + " failed: 500: Internal Server Error", e);
		}
	}

	@Override
	public HttpPushProcessor createNewInstance() {
		return new DisplayNamesRequestHandler();
	}

	@Override
	public String path() {
		return "/i/display_names";
	}

}
