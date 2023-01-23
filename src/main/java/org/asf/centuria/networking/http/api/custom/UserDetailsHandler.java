package org.asf.centuria.networking.http.api.custom;

import java.io.ByteArrayOutputStream;
import java.net.Socket;
import org.asf.centuria.Centuria;
import org.asf.centuria.accounts.AccountManager;
import org.asf.centuria.accounts.CenturiaAccount;
import org.asf.centuria.accounts.SaveMode;
import org.asf.rats.ConnectiveHTTPServer;
import org.asf.rats.processors.HttpUploadProcessor;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class UserDetailsHandler extends HttpUploadProcessor {
	@Override
	public void process(String contentType, Socket client, String method) {
		try {
			Centuria.logger.info("API CALL: " + getRequest().path);
			if (!getRequest().method.equalsIgnoreCase("post")) {
				this.setResponseCode(400);
				this.setResponseMessage("Bad request");
				return;
			}

			// Parse body
			ByteArrayOutputStream strm = new ByteArrayOutputStream();
			ConnectiveHTTPServer.transferRequestBody(getHeaders(), getRequestBodyStream(), strm);
			byte[] body = strm.toByteArray();
			strm.close();

			// Load manager
			AccountManager manager = AccountManager.getInstance();

			// Parse body
			JsonObject request = JsonParser.parseString(new String(body, "UTF-8")).getAsJsonObject();
			if (!request.has("id") && !request.has("name")) {
				this.setResponseCode(400);
				this.setResponseMessage("Bad request");
				return;
			}
			String id = null;
			if (request.has("id"))
				id = request.get("id").getAsString();
			else
				id = manager.getUserByDisplayName(request.get("name").getAsString());

			// Find account
			if (id == null) {
				this.setResponseCode(404);
				this.setResponseMessage("Account not found");
				return;
			}
			CenturiaAccount acc = manager.getAccount(id);
			if (acc == null) {
				this.setResponseCode(404);
				this.setResponseMessage("Account not found");
				return;
			}

			// Send response
			JsonObject response = new JsonObject();
			response.addProperty("uuid", id);
			response.addProperty("display_name", acc.getDisplayName());
			response.addProperty("current_level", acc.getLevel().getLevel());
			response.addProperty("save_mode", acc.getSaveMode().toString());
			if (acc.getSaveMode() == SaveMode.MANAGED)
				response.addProperty("active_save", acc.getSaveManager().getCurrentActiveSave());
			response.add("current_save_settings", acc.getSaveSpecificInventory().getSaveSettings().writeToObject());
			response.add("active_look",
					acc.getSaveSpecificInventory().getAccessor().findInventoryObject("avatars", acc.getActiveLook())
							.get("components").getAsJsonObject().get("AvatarLook").getAsJsonObject());
			setBody(response.toString());
		} catch (Exception e) {
			setResponseCode(500);
			setResponseMessage("Internal Server Error");
			Centuria.logger.error(getRequest().path + " failed: 500: Internal Server Error", e);
		}
	}

	@Override
	public boolean supportsGet() {
		return true;
	}

	@Override
	public HttpUploadProcessor createNewInstance() {
		return new UserDetailsHandler();
	}

	@Override
	public String path() {
		return "/centuria/getuser";
	}

}
