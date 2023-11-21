package org.asf.centuria.networking.http.api;

import java.io.IOException;
import java.net.URLDecoder;

import org.asf.centuria.Centuria;
import org.asf.centuria.accounts.AccountManager;
import org.asf.centuria.textfilter.TextFilterService;
import org.asf.connective.RemoteClient;
import org.asf.connective.processors.HttpPushProcessor;

import com.google.gson.JsonObject;

public class DisplayNameValidationHandler extends HttpPushProcessor {

	@Override
	public void process(String path, String method, RemoteClient client, String contentType) throws IOException {
		try {
			// Get name
			String name = URLDecoder.decode(getRequest().getRequestPath().substring(path().length() + 1), "UTF-8");
			AccountManager manager = AccountManager.getInstance();

			// Check validity
			JsonObject response = new JsonObject();
			if (manager.getUserByDisplayName(name) != null) {
				response.addProperty("error", "display_name_already_taken");
				setResponseContent("text/json", response.toString());
				return;
			}

			if (!name.matches("^[0-9A-Za-z\\-_. ]+") || name.length() > 16 || name.length() < 2) {
				response.addProperty("error", "display_name_invalid_format");
				setResponseContent("text/json", response.toString());
				return;
			}

			// Verify name blacklist
			if (TextFilterService.getInstance().isFiltered(name, true, "USERNAMEFILTER")) {
				// Reply with error
				response.addProperty("error", "display_name_sift_rejected");
				setResponseContent("text/json", response.toString());
				this.setResponseStatus(400, "Bad request");
				return;
			}

			// Send response
			response.addProperty("success", true);
			setResponseContent("text/json", response.toString());
		} catch (Exception e) {
			setResponseStatus(500, "Internal Server Error");
			Centuria.logger.error(getRequest().getRequestPath() + " failed: 500: Internal Server Error", e);
		}
	}

	@Override
	public HttpPushProcessor createNewInstance() {
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
