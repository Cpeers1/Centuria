package org.asf.centuria.networking.http.api;

import java.net.Socket;
import java.net.URLDecoder;

import org.asf.centuria.Centuria;
import org.asf.centuria.accounts.AccountManager;
import org.asf.rats.processors.HttpUploadProcessor;

import com.google.gson.JsonObject;

public class DisplayNameValidationHandler extends HttpUploadProcessor {

	@Override
	public void process(String contentType, Socket client, String method) {
		try {
			Centuria.logger.info("API CALL: " + getRequest().path);

			// Get name
			String name = URLDecoder.decode(getRequest().path.substring(path().length() + 1), "UTF-8");
			AccountManager manager = AccountManager.getInstance();

			// Check validity
			JsonObject response = new JsonObject();
			if (manager.getUserByDisplayName(name) != null) {
				response.addProperty("error", "display_name_already_taken");
				setBody(response.toString());
				return;
			}

			if (!name.matches("^[0-9A-Za-z\\-_. ]+") || name.length() > 16 || name.length() < 2) {
				response.addProperty("error", "display_name_invalid_format");
				setBody(response.toString());
				return;
			}

			// Send response
			response.addProperty("success", true);
			setBody(response.toString());
		} catch (Exception e) {
			setResponseCode(500);
			setResponseMessage("Internal Server Error");
			Centuria.logger.error(getRequest().path + " failed: 500: Internal Server Error", e);
		}
	}

	@Override
	public HttpUploadProcessor createNewInstance() {
		return new DisplayNameValidationHandler();
	}

	@Override
	public String path() {
		return "/dn/validate";
	}

	@Override
	public boolean supportsChildPaths() {
		return true;
	}

}
