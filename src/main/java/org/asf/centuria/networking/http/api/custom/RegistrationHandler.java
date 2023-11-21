package org.asf.centuria.networking.http.api.custom;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.asf.centuria.Centuria;
import org.asf.centuria.accounts.AccountManager;
import org.asf.centuria.accounts.registration.RegistrationVerificationHelper;
import org.asf.centuria.accounts.registration.RegistrationVerificationResult;
import org.asf.centuria.accounts.registration.RegistrationVerificationStatus;
import org.asf.centuria.textfilter.TextFilterService;
import org.asf.connective.RemoteClient;
import org.asf.connective.processors.HttpPushProcessor;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class RegistrationHandler extends HttpPushProcessor {

	@Override
	public void process(String path, String method, RemoteClient client, String contentType) throws IOException {
		try {
			if (!method.equalsIgnoreCase("post")) {
				this.setResponseStatus(400, "Bad request");
				return;
			}

			// Load account manager
			AccountManager manager = AccountManager.getInstance();

			// Parse body
			ByteArrayOutputStream strm = new ByteArrayOutputStream();
			getRequest().transferRequestBody(strm);
			byte[] body = strm.toByteArray();
			strm.close();

			// Parse body
			JsonObject request = JsonParser.parseString(new String(body, "UTF-8")).getAsJsonObject();
			if (!request.has("login_name") || !request.has("display_name") || !request.has("password")) {
				this.setResponseStatus(400, "Bad request");
				return;
			}

			// Prepare response
			JsonObject response = new JsonObject();

			// Handle the request
			String accountName = request.get("login_name").getAsString();
			String displayName = request.get("display_name").getAsString();
			char[] password = request.get("password").getAsString().toCharArray();

			// Verify login name blacklist
			if (TextFilterService.getInstance().isFiltered(accountName, true, "USERNAMEFILTER")) {
				// Reply with error
				response.addProperty("status", "failure");
				response.addProperty("error", "invalid_login_name");
				response.addProperty("error_message",
						"Invalid login name: this name may not be used as it may not be appropriate.");
				setResponseContent("text/json", response.toString());
				this.setResponseStatus(400, "Bad request");
				return;
			}

			// Verify name blacklist
			if (TextFilterService.getInstance().isFiltered(displayName, true, "USERNAMEFILTER")) {
				// Reply with error
				response.addProperty("status", "failure");
				response.addProperty("error", "invalid_display_name");
				response.addProperty("error_message",
						"Invalid display name: this name may not be used as it may not be appropriate.");
				setResponseContent("text/json", response.toString());
				this.setResponseStatus(400, "Bad request");
				return;
			}

			// Verify login name validity
			if (!accountName.matches("^[A-Za-z0-9@._#]+$") || accountName.contains(".cred")
					|| !accountName.matches(".*[A-Za-z0-9]+.*") || accountName.isBlank()
					|| accountName.length() > 320) {
				// Reply with error
				response.addProperty("status", "failure");
				response.addProperty("error", "invalid_login_name");
				response.addProperty("error_message", "Invalid login name.");
				setResponseContent("text/json", response.toString());
				this.setResponseStatus(400, "Bad request");
				return;
			}

			// Verify name validity
			if (!displayName.matches("^[0-9A-Za-z\\-_. ]+") || displayName.length() > 16 || displayName.length() < 2) {
				// Reply with error
				response.addProperty("status", "failure");
				response.addProperty("error", "invalid_display_name");
				response.addProperty("error_message", "Invalid display name.");
				setResponseContent("text/json", response.toString());
				this.setResponseStatus(400, "Bad request");
				return;
			}

			// Check if the login name is in use
			if (manager.getUserByLoginName(accountName) != null) {
				// Reply with error
				response.addProperty("status", "failure");
				response.addProperty("error", "login_name_in_use");
				response.addProperty("error_message", "Selected login name is already in use.");
				setResponseContent("text/json", response.toString());
				this.setResponseStatus(400, "Bad request");
				return;
			}

			// Check if the name is in use
			if (manager.isDisplayNameInUse(displayName)) {
				// Reply with error
				response.addProperty("status", "failure");
				response.addProperty("error", "display_name_in_use");
				response.addProperty("error_message", "Selected display name is already in use.");
				setResponseContent("text/json", response.toString());
				this.setResponseStatus(400, "Bad request");
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
					setResponseContent("text/json", response.toString());
					this.setResponseStatus(400, "Bad request");
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
					setResponseContent("text/json", response.toString());
					this.setResponseStatus(400, "Bad request");
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
					this.setResponseStatus(400, "Bad request");
				}
			}

			// Send response
			setResponseContent("text/json", response.toString());
		} catch (Exception e) {
			setResponseStatus(500, "Internal Server Error");
			Centuria.logger.error(getRequest().getRequestPath() + " failed: 500: Internal Server Error", e);
		}
	}

	@Override
	public boolean supportsNonPush() {
		return true;
	}

	@Override
	public HttpPushProcessor createNewInstance() {
		return new RegistrationHandler();
	}

	@Override
	public String path() {
		return "/centuria/register";
	}

}
