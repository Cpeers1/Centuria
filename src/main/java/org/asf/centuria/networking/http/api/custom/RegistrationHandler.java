package org.asf.centuria.networking.http.api.custom;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.ArrayList;

import org.asf.centuria.Centuria;
import org.asf.centuria.accounts.AccountManager;
import org.asf.centuria.accounts.registration.RegistrationVerificationHelper;
import org.asf.centuria.accounts.registration.RegistrationVerificationResult;
import org.asf.centuria.accounts.registration.RegistrationVerificationStatus;
import org.asf.centuria.packets.xt.gameserver.inventory.InventoryItemDownloadPacket;
import org.asf.rats.ConnectiveHTTPServer;
import org.asf.rats.processors.HttpUploadProcessor;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class RegistrationHandler extends HttpUploadProcessor {

	private static String[] nameBlacklist = new String[] { "kit", "kitsendragn", "kitsendragon", "fera", "fero",
			"wwadmin", "ayli", "komodorihero", "wwsam", "blinky", "fer.ocity" };

	private static ArrayList<String> muteWords = new ArrayList<String>();
	private static ArrayList<String> filterWords = new ArrayList<String>();

	static {
		// Load filter
		try {
			InputStream strm = InventoryItemDownloadPacket.class.getClassLoader()
					.getResourceAsStream("textfilter/filter.txt");
			String lines = new String(strm.readAllBytes(), "UTF-8").replace("\r", "");
			for (String line : lines.split("\n")) {
				if (line.isEmpty() || line.startsWith("#"))
					continue;

				String data = line.trim();
				while (data.contains("  "))
					data = data.replace("  ", "");

				for (String word : data.split(" "))
					filterWords.add(word.toLowerCase());
			}
			strm.close();
		} catch (IOException e) {
		}

		// Load ban words
		try {
			InputStream strm = InventoryItemDownloadPacket.class.getClassLoader()
					.getResourceAsStream("textfilter/instamute.txt");
			String lines = new String(strm.readAllBytes(), "UTF-8").replace("\r", "");
			for (String line : lines.split("\n")) {
				if (line.isEmpty() || line.startsWith("#"))
					continue;

				String data = line.trim();
				while (data.contains("  "))
					data = data.replace("  ", "");

				for (String word : data.split(" "))
					muteWords.add(word.toLowerCase());
			}
			strm.close();
		} catch (IOException e) {
		}
	}

	@Override
	public void process(String contentType, Socket client, String method) {
		try {
			if (!getRequest().method.equalsIgnoreCase("post")) {
				this.setResponseCode(400);
				this.setResponseMessage("Bad request");
				return;
			}

			// Load account manager
			AccountManager manager = AccountManager.getInstance();

			// Parse body
			ByteArrayOutputStream strm = new ByteArrayOutputStream();
			ConnectiveHTTPServer.transferRequestBody(getHeaders(), getRequestBodyStream(), strm);
			byte[] body = strm.toByteArray();
			strm.close();

			// Parse body
			JsonObject request = JsonParser.parseString(new String(body, "UTF-8")).getAsJsonObject();
			if (!request.has("login_name") || !request.has("display_name") || !request.has("password")) {
				this.setResponseCode(400);
				this.setResponseMessage("Bad request");
				return;
			}

			// Prepare response
			JsonObject response = new JsonObject();

			// Handle the request
			String accountName = request.get("login_name").getAsString();
			String displayName = request.get("display_name").getAsString();
			char[] password = request.get("password").getAsString().toCharArray();

			// Verify login name blacklist
			for (String name : nameBlacklist) {
				if (accountName.equalsIgnoreCase(name)) {
					// Reply with error
					response.addProperty("status", "failure");
					response.addProperty("error", "invalid_login_name");
					response.addProperty("error_message",
							"Invalid login name: this name may not be used as it may not be appropriate.");
					setBody("text/json", response.toString());
					this.setResponseCode(400);
					this.setResponseMessage("Bad request");
					return;
				}
			}

			// Verify name blacklist
			for (String name : nameBlacklist) {
				if (displayName.equalsIgnoreCase(name)) {
					// Reply with error
					response.addProperty("status", "failure");
					response.addProperty("error", "invalid_display_name");
					response.addProperty("error_message",
							"Invalid display name: this name may not be used as it may not be appropriate.");
					setBody("text/json", response.toString());
					this.setResponseCode(400);
					this.setResponseMessage("Bad request");
					return;
				}
			}

			// Verify login name with filters
			for (String word : accountName.split(" ")) {
				if (muteWords.contains(word.replaceAll("[^A-Za-z0-9]", "").toLowerCase())) {
					// Reply with error
					response.addProperty("status", "failure");
					response.addProperty("error", "invalid_login_name");
					response.addProperty("error_message",
							"Invalid login name: this name may not be used as it may not be appropriate.");
					setBody("text/json", response.toString());
					this.setResponseCode(400);
					this.setResponseMessage("Bad request");
					return;
				}

				if (filterWords.contains(word.replaceAll("[^A-Za-z0-9]", "").toLowerCase())) {
					// Reply with error
					response.addProperty("status", "failure");
					response.addProperty("error", "invalid_login_name");
					response.addProperty("error_message",
							"Invalid login name: this name may not be used as it may not be appropriate.");
					setBody("text/json", response.toString());
					this.setResponseCode(400);
					this.setResponseMessage("Bad request");
					return;
				}
			}

			// Verify name with filters
			for (String word : displayName.split(" ")) {
				if (muteWords.contains(word.replaceAll("[^A-Za-z0-9]", "").toLowerCase())) {
					// Reply with error
					response.addProperty("status", "failure");
					response.addProperty("error", "invalid_display_name");
					response.addProperty("error_message",
							"Invalid display name: this name may not be used as it may not be appropriate.");
					setBody("text/json", response.toString());
					this.setResponseCode(400);
					this.setResponseMessage("Bad request");
					return;
				}

				if (filterWords.contains(word.replaceAll("[^A-Za-z0-9]", "").toLowerCase())) {
					// Reply with error
					response.addProperty("status", "failure");
					response.addProperty("error", "invalid_display_name");
					response.addProperty("error_message",
							"Invalid display name: this name may not be used as it may not be appropriate.");
					setBody("text/json", response.toString());
					this.setResponseCode(400);
					this.setResponseMessage("Bad request");
					return;
				}
			}

			// Verify login name validity
			if (!accountName.matches("^[A-Za-z0-9@._#]+$") || accountName.contains(".cred")
					|| !accountName.matches(".*[A-Za-z0-9]+.*") || accountName.isBlank()
					|| accountName.length() > 320) {
				// Reply with error
				response.addProperty("status", "failure");
				response.addProperty("error", "invalid_login_name");
				response.addProperty("error_message", "Invalid login name.");
				setBody("text/json", response.toString());
				this.setResponseCode(400);
				this.setResponseMessage("Bad request");
				return;
			}

			// Verify name validity
			if (!displayName.matches("^[0-9A-Za-z\\-_. ]+") || displayName.length() > 16 || displayName.length() < 2) {
				// Reply with error
				response.addProperty("status", "failure");
				response.addProperty("error", "invalid_display_name");
				response.addProperty("error_message", "Invalid display name.");
				setBody("text/json", response.toString());
				this.setResponseCode(400);
				this.setResponseMessage("Bad request");
				return;
			}

			// Check if the login name is in use
			if (manager.getUserByLoginName(accountName) != null) {
				// Reply with error
				response.addProperty("status", "failure");
				response.addProperty("error", "login_name_in_use");
				response.addProperty("error_message", "Selected login name is already in use.");
				setBody("text/json", response.toString());
				this.setResponseCode(400);
				this.setResponseMessage("Bad request");
				return;
			}

			// Check if the name is in use
			if (manager.isDisplayNameInUse(displayName)) {
				// Reply with error
				response.addProperty("status", "failure");
				response.addProperty("error", "display_name_in_use");
				response.addProperty("error_message", "Selected display name is already in use.");
				setBody("text/json", response.toString());
				this.setResponseCode(400);
				this.setResponseMessage("Bad request");
				return;
			}

			// Check server settings
			if (Centuria.allowRegistration) {
				// Quick and easy

				// Register the account
				String accountID = manager.register(accountName);
				if (accountID == null) {
					// Registration failed
					return;
				}

				// Assign and lock display name
				manager.getAccount(accountID).updateDisplayName(displayName);
				manager.lockDisplayName(displayName, accountID);

				// Set password
				manager.updatePassword(accountID, password);

				response.addProperty("status", "success");
				response.addProperty("account_id", accountID);
			} else {
				// Full flow

				// Check data
				if (!request.has("verification_method") || !request.has("verification_payload")) {
					// Reply with error
					response.addProperty("status", "failure");
					response.addProperty("error", "missing_verification");
					response.addProperty("error_message", "This server requires registration verification.");
					setBody("text/json", response.toString());
					this.setResponseCode(400);
					this.setResponseMessage("Bad request");
					return;
				}

				// Find helper
				RegistrationVerificationHelper helper = RegistrationVerificationHelper
						.getByMethod(request.get("verification_method").getAsString());
				if (helper == null) {
					// Reply with error
					response.addProperty("status", "failure");
					response.addProperty("error", "invalid_verification_method");
					response.addProperty("error_message", "Verification method unsupported.");
					setBody("text/json", response.toString());
					this.setResponseCode(400);
					this.setResponseMessage("Bad request");
					return;
				}

				// Attempt to run helper
				RegistrationVerificationResult result = helper.verify(accountName, displayName,
						request.get("verification_payload").getAsJsonObject());
				if (result.status == RegistrationVerificationStatus.SUCCESS) {
					// Register the account
					String accountID = manager.register(accountName);
					if (accountID == null) {
						// Registration failed
						return;
					}

					// Assign and lock display name
					manager.getAccount(accountID).updateDisplayName(displayName);
					manager.lockDisplayName(displayName, accountID);

					// Set password
					manager.updatePassword(accountID, password);

					response.addProperty("status", "success");
					response.addProperty("account_id", accountID);

					// Call helper post registration
					helper.postRegistration(manager.getAccount(accountID), accountID, displayName,
							request.get("verification_payload").getAsJsonObject());
				} else {
					// Set error
					if (result.error == null)
						switch (result.status) {
						case DEFER_MISSING_FIELDS:
							result.error = "defer_missing_data";
							if (result.errorMessage == null) {
								result.errorMessage = "Missing verification data.";
							}
							break;
						case DEFER_REQUIRE_CODE:
							result.error = "defer_require_code";
							if (result.errorMessage == null) {
								result.errorMessage = "Missing verification code.";
							}
							break;
						case FAILURE:
							result.error = "verification_failure";
							if (result.errorMessage == null) {
								result.errorMessage = "Verification failure.";
							}
							break;
						case INVALID_CODE:
							result.error = "invalid_code";
							if (result.errorMessage == null) {
								result.errorMessage = "Invalid verification code.";
							}
							break;
						default:
							break;

						}
					response.addProperty("status", "failure");
					response.addProperty("error", result.error);
					response.addProperty("error_message", result.errorMessage);
					this.setResponseCode(400);
					this.setResponseMessage("Bad request");
				}
			}

			// Send response
			setBody("text/json", response.toString());
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
		return new RegistrationHandler();
	}

	@Override
	public String path() {
		return "/centuria/register";
	}

}
