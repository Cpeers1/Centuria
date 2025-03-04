package org.asf.centuria.networking.http.api;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

import org.asf.centuria.Centuria;
import org.asf.centuria.accounts.AccountManager;
import org.asf.centuria.accounts.CenturiaAccount;
import org.asf.connective.RemoteClient;
import org.asf.connective.processors.HttpPushProcessor;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class XPDetailsHandler extends HttpPushProcessor {

	@Override
	public void process(String path, String method, RemoteClient client, String contentType) throws IOException {
		try {
			// Parse body
			ByteArrayOutputStream strm = new ByteArrayOutputStream();
			getRequest().transferRequestBody(strm);
			byte[] body = strm.toByteArray();
			strm.close();

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
			JsonArray req = JsonParser.parseString(new String(body, "UTF-8")).getAsJsonArray();

			// Build response
			JsonObject response = new JsonObject();
			JsonArray found = new JsonArray();
			for (JsonElement ele : req) {
				String uuid = ele.getAsString();

				// Locate account
				CenturiaAccount acc = AccountManager.getInstance().getAccount(uuid);
				if (acc.getLevel().isLevelAvailable()) {
					JsonObject lvD = new JsonObject();
					JsonObject cLv = new JsonObject();
					cLv.addProperty("level", acc.getLevel().getLevel());
					cLv.addProperty("required", acc.getLevel().getLevelupXPCount());
					cLv.addProperty("xp", acc.getLevel().getCurrentXP());
					lvD.add("current_level", cLv);
					lvD.addProperty("total_xp", acc.getLevel().getTotalXP());
					lvD.addProperty("uuid", uuid);
					found.add(lvD);
				}
			}

			// Send response
			response.add("found", found);
			response.add("not_found", req);
			setResponseContent("text/json", response.toString());
		} catch (Exception e) {
			setResponseStatus(500, "Internal Server Error");
			Centuria.logger.error(getRequest().getRequestPath() + " failed: 500: Internal Server Error", e);
		}
	}

	@Override
	public HttpPushProcessor createNewInstance() {
		return new XPDetailsHandler();
	}

	@Override
	public String path() {
		return "/xp/xp-details";
	}

}
