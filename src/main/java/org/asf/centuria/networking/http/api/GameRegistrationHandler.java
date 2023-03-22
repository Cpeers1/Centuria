package org.asf.centuria.networking.http.api;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.UUID;

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

public class GameRegistrationHandler extends HttpUploadProcessor {

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
			// Parse body
			ByteArrayOutputStream strm = new ByteArrayOutputStream();
			ConnectiveHTTPServer.transferRequestBody(getHeaders(), getRequestBodyStream(), strm);
			byte[] body = strm.toByteArray();
			strm.close();

			// Load manager
			AccountManager manager = AccountManager.getInstance();

			// Parse body
			JsonObject request = JsonParser.parseString(new String(body, "UTF-8")).getAsJsonObject();

			// Handle the request
			String accountName = request.get("username").getAsString();
			String displayName = request.get("display_name").getAsString();
			char[] password = request.get("password").getAsString().toCharArray();

			// Prepare response
			JsonObject response = new JsonObject();

			// Verify login name blacklist
			for (String name : nameBlacklist) {
				if (accountName.equalsIgnoreCase(name)) {
					// Reply with error
					response.addProperty("error", "invalid_username");
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
					response.addProperty("error", "display_name_sift_rejected");
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
					response.addProperty("error", "invalid_username");
					setBody("text/json", response.toString());
					this.setResponseCode(400);
					this.setResponseMessage("Bad request");
					return;
				}

				if (filterWords.contains(word.replaceAll("[^A-Za-z0-9]", "").toLowerCase())) {
					// Reply with error
					response.addProperty("error", "invalid_username");
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
					response.addProperty("error", "display_name_sift_rejected");
					setBody("text/json", response.toString());
					this.setResponseCode(400);
					this.setResponseMessage("Bad request");
					return;
				}

				if (filterWords.contains(word.replaceAll("[^A-Za-z0-9]", "").toLowerCase())) {
					// Reply with error
					response.addProperty("error", "display_name_sift_rejected");
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
				response.addProperty("error", "invalid_username");
				setBody("text/json", response.toString());
				this.setResponseCode(400);
				this.setResponseMessage("Bad request");
				return;
			}

			// Verify name validity
			if (!displayName.matches("^[0-9A-Za-z\\-_. ]+") || displayName.length() > 16 || displayName.length() < 2) {
				// Reply with error
				response.addProperty("error", "display_name_invalid_format");
				setBody("text/json", response.toString());
				this.setResponseCode(400);
				this.setResponseMessage("Bad request");
				return;
			}

			// Check if the login name is in use
			if (manager.getUserByLoginName(accountName) != null) {
				// Reply with error
				response.addProperty("error", "username_already_exists");
				setBody("text/json", response.toString());
				this.setResponseCode(400);
				this.setResponseMessage("Bad request");
				return;
			}

			// Check if the name is in use
			if (manager.isDisplayNameInUse(displayName)) {
				// Reply with error
				response.addProperty("error", "display_name_already_taken");
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

				// Build JWT
				JsonObject headers = new JsonObject();
				headers.addProperty("alg", "RS256");
				headers.addProperty("kid", FallbackAPIProcessor.KeyID);
				headers.addProperty("typ", "JWT");
				String headerD = Base64.getUrlEncoder().withoutPadding()
						.encodeToString(headers.toString().getBytes("UTF-8"));

				JsonObject payload = new JsonObject();
				payload.addProperty("iat", System.currentTimeMillis() / 1000);
				payload.addProperty("exp", (System.currentTimeMillis() / 1000) + (2 * 24 * 60 * 60));
				payload.addProperty("jti", UUID.randomUUID().toString());
				payload.addProperty("iss", "Centuria");
				payload.addProperty("sub", "Centuria");
				payload.addProperty("uuid", accountID);
				String payloadD = Base64.getUrlEncoder().withoutPadding()
						.encodeToString(payload.toString().getBytes("UTF-8"));
				String tkn = headerD + "." + payloadD + "." + Base64.getUrlEncoder().withoutPadding()
						.encodeToString(Centuria.sign((headerD + "." + payloadD).getBytes("UTF-8")));

				// Add response
				response.addProperty("uuid", accountID);
				response.addProperty("auth_token", tkn);
				response.addProperty("refresh_token", tkn);
			} else {
				// Full flow

				// Find method and payload
				if (!new File("ingameregistrationhelper.conf").exists()) {
					// Create helper document
					Files.writeString(Path.of("ingameregistrationhelper.conf"),
							"^[\\w%+\\.-]+@(?:[a-zA-Z0-9-]+[\\.{1}])+[a-zA-Z]{2,}$ email {\"address\":\"%loginname%\"}\n");
				}

				// Find helper
				for (String helperLine : Files.readAllLines(Path.of("ingameregistrationhelper.conf"))) {
					if (helperLine.split(" ").length != 3)
						continue;
					String regex = helperLine.split(" ")[0].replace("%space%", " ").replace("%loginname%", accountName)
							.replace("%displayname%", displayName);
					String helperID = helperLine.split(" ")[1].replace("%space%", " ")
							.replace("%loginname%", accountName).replace("%displayname%", displayName);
					String json = helperLine.split(" ")[2].replace("%space%", " ").replace("%loginname%", accountName)
							.replace("%displayname%", displayName);

					// Check regex
					if (!accountName.matches(regex))
						continue;

					// Append to JSON if otp is present
					if (request.has("otp") && !request.get("otp").getAsString().isEmpty()) {
						JsonObject data = JsonParser.parseString(json).getAsJsonObject();
						data.addProperty("code", request.get("otp").getAsString());
						json = data.toString();
					}
					JsonObject verifyPayload = JsonParser.parseString(json).getAsJsonObject();

					// Find helper
					RegistrationVerificationHelper helper = RegistrationVerificationHelper.getByMethod(helperID);
					if (helper == null) {
						// Reply with error
						response.addProperty("error",
								"Unable to verify with that name. Please use a different login name format.");
						setBody("text/json", response.toString());
						this.setResponseCode(400);
						this.setResponseMessage("Bad request");
						return;
					}

					// Attempt to run helper
					RegistrationVerificationResult result = helper.verify(accountName, displayName, verifyPayload);
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

						// Build JWT
						JsonObject headers = new JsonObject();
						headers.addProperty("alg", "RS256");
						headers.addProperty("kid", FallbackAPIProcessor.KeyID);
						headers.addProperty("typ", "JWT");
						String headerD = Base64.getUrlEncoder().withoutPadding()
								.encodeToString(headers.toString().getBytes("UTF-8"));

						JsonObject payload = new JsonObject();
						payload.addProperty("iat", System.currentTimeMillis() / 1000);
						payload.addProperty("exp", (System.currentTimeMillis() / 1000) + (2 * 24 * 60 * 60));
						payload.addProperty("jti", UUID.randomUUID().toString());
						payload.addProperty("iss", "Centuria");
						payload.addProperty("sub", "Centuria");
						payload.addProperty("uuid", accountID);
						String payloadD = Base64.getUrlEncoder().withoutPadding()
								.encodeToString(payload.toString().getBytes("UTF-8"));
						String tkn = headerD + "." + payloadD + "." + Base64.getUrlEncoder().withoutPadding()
								.encodeToString(Centuria.sign((headerD + "." + payloadD).getBytes("UTF-8")));

						// Add response
						response.addProperty("uuid", accountID);
						response.addProperty("auth_token", tkn);
						response.addProperty("refresh_token", tkn);

						// Call helper post registration
						helper.postRegistration(manager.getAccount(accountID), accountID, displayName, verifyPayload);
						setBody("text/json", response.toString());
						return;
					} else {
						// Set error
						switch (result.status) {
						case DEFER_REQUIRE_CODE:
							result.error = "pending_otp_confirmation";
							if (result.errorMessage == null) {
								result.errorMessage = "Missing verification code.";
							}
							break;
						case INVALID_CODE:
							result.error = "invalid_otp";
							if (result.errorMessage == null) {
								result.errorMessage = "Invalid verification code.";
							}
							break;
						default:
							if (result.errorMessage == null) {
								result.errorMessage = "Error unknown, code: " + result.error + ".";
							}
							result.error = result.errorMessage;
							break;

						}
						response.addProperty("error", result.error);
						response.addProperty("error_message", result.errorMessage);
						this.setResponseCode(400);
						this.setResponseMessage("Bad request");
						setBody("text/json", response.toString());
						return;
					}
				}
				// Reply with error
				response.addProperty("error",
						"Unable to verify with that name. Please use a different login name format.");
				setBody("text/json", response.toString());
				this.setResponseCode(400);
				this.setResponseMessage("Bad request");
				return;
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
	public HttpUploadProcessor createNewInstance() {
		return new GameRegistrationHandler();
	}

	@Override
	public String path() {
		return "/u/register";
	}

}
