package org.asf.centuria.networking.http.api;

import java.net.Socket;
import java.net.URLDecoder;
import java.util.stream.Stream;

import org.asf.centuria.Centuria;
import org.asf.centuria.accounts.AccountManager;
import org.asf.centuria.networking.chatserver.networking.SendMessage;
import org.asf.rats.processors.HttpUploadProcessor;

import com.google.gson.JsonObject;

public class DisplayNameValidationHandler extends HttpUploadProcessor {

	private static String[] nameBlacklist = new String[] { "kit", "kitsendragn", "kitsendragon", "fera", "fero",
			"wwadmin", "ayli", "komodorihero", "wwsam", "blinky", "fer.ocity" };

	@Override
	public void process(String contentType, Socket client, String method) {
		try {
			// Get name
			String name = URLDecoder.decode(getRequest().path.substring(path().length() + 1), "UTF-8");
			AccountManager manager = AccountManager.getInstance();

			// Check validity
			JsonObject response = new JsonObject();
			if (manager.getUserByDisplayName(name) != null) {
				response.addProperty("error", "display_name_already_taken");
				setBody("text/json", response.toString());
				return;
			}

			if (!name.matches("^[0-9A-Za-z\\-_. ]+") || name.length() > 16 || name.length() < 2) {
				response.addProperty("error", "display_name_invalid_format");
				setBody("text/json", response.toString());
				return;
			}

			// Prevent blacklisted names from being used
			for (String nm : nameBlacklist) {
				if (name.equalsIgnoreCase(nm)) {
					response.addProperty("error", "display_name_sift_rejected");
					setBody("text/json", response.toString());
					return;
				}
			}

			// Prevent banned and filtered words
			for (String word : name.split(" ")) {
				if (Stream.of(SendMessage.getInvalidWords())
						.anyMatch(t -> t.toLowerCase().equals(word.replaceAll("[^A-Za-z0-9]", "").toLowerCase()))) {
					response.addProperty("error", "display_name_sift_rejected");
					setBody("text/json", response.toString());
					return;
				}
			}

			// Send response
			response.addProperty("success", true);
			setBody("text/json", response.toString());
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
