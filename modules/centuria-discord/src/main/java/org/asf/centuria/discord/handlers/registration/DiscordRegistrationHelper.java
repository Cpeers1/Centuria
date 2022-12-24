package org.asf.centuria.discord.handlers.registration;

import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Random;

import org.asf.centuria.accounts.CenturiaAccount;
import org.asf.centuria.accounts.registration.RegistrationVerificationHelper;
import org.asf.centuria.accounts.registration.RegistrationVerificationResult;
import org.asf.centuria.accounts.registration.RegistrationVerificationStatus;
import org.asf.centuria.discord.DiscordBotModule;
import org.asf.centuria.discord.LinkUtils;
import org.asf.centuria.discord.TimedActions;

import com.google.gson.JsonObject;

import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.spec.MessageCreateSpec;

public class DiscordRegistrationHelper extends RegistrationVerificationHelper {

	private static Random rnd = new Random();

	private static class RegistrationCode {
		public long timeRemaining;
		public String code;
	}

	private static HashMap<String, RegistrationCode> verificationCodes = new HashMap<String, RegistrationCode>();

	// Start expiry thread
	static {
		Thread th = new Thread(() -> {
			while (true) {
				HashMap<String, RegistrationCode> sCodes;
				while (true) {
					try {
						sCodes = new HashMap<String, RegistrationCode>(verificationCodes);
						break;
					} catch (ConcurrentModificationException e) {
					}
				}

				for (String c : sCodes.keySet()) {
					if (verificationCodes.get(c).timeRemaining - 1 <= 0) {
						verificationCodes.remove(c);
					} else {
						verificationCodes.get(c).timeRemaining--;
					}
				}

				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					break;
				}
			}
		}, "Verification cleanup");
		th.setDaemon(true);
		th.start();
	}

	/**
	 * Verifies a verification code
	 * 
	 * @param namespace Verification namespace
	 * @param code      Verification code
	 * @return True if successful, false otherwise
	 */
	public static boolean checkCode(String namespace, String code) {
		HashMap<String, RegistrationCode> sCodes;
		while (true) {
			try {
				sCodes = new HashMap<String, RegistrationCode>(verificationCodes);
				break;
			} catch (ConcurrentModificationException e) {
			}
		}

		// Find token by user ID
		if (sCodes.containsKey(namespace) && sCodes.get(namespace).code.equalsIgnoreCase(code)) {
			// Remove code
			verificationCodes.remove(namespace);

			// Return success
			return true;
		}

		// Return failure
		return false;
	}

	/**
	 * Retrieves a verification code by namespace
	 * 
	 * @param namespace VErification namespace
	 * @return Verification code or null
	 */
	public static String getCode(String namespace) {
		HashMap<String, RegistrationCode> sCodes;
		while (true) {
			try {
				sCodes = new HashMap<String, RegistrationCode>(verificationCodes);
				break;
			} catch (ConcurrentModificationException e) {
			}
		}

		// Find token by user ID
		if (sCodes.containsKey(namespace))
			return sCodes.get(namespace).code;
		return null;
	}

	/**
	 * Generates a verification code
	 * 
	 * @param namespace Code namespace (actions already in this namespace would be
	 *                  overwritten)
	 * @param time      Code expiry time
	 * @return Verification code
	 */
	public static String generateCode(String namespace, long time) {
		// Generate action code
		String code = "";
		while (true) {
			// Generate new code
			code = "";
			for (int i = 0; i < 5; i++) {
				if (rnd.nextBoolean())
					code += (char) rnd.nextInt((int) 'A', (int) 'Z' + 1);
				else
					code += (char) rnd.nextInt((int) '0', (int) '9' + 1);
			}

			// Attempt to locate existing code
			boolean found = false;

			HashMap<String, RegistrationCode> sCodes;
			while (true) {
				try {
					sCodes = new HashMap<String, RegistrationCode>(verificationCodes);
					break;
				} catch (ConcurrentModificationException e) {
				}
			}

			for (RegistrationCode d : sCodes.values())
				if (d.code.equals(code)) {
					found = true;
					break;
				}

			// Break loop
			if (!found)
				break;
		}

		// Build object info and save
		RegistrationCode data = new RegistrationCode();
		data.code = code;
		data.timeRemaining = time;
		verificationCodes.put(namespace, data);

		// Return code
		return code;
	}

	public static void init() {
		new DiscordRegistrationHelper().register();
	}

	@Override
	public String registrationMethodID() {
		return "discord";
	}

	@Override
	public RegistrationVerificationResult verify(String accountName, String displayName, JsonObject payload) {
		RegistrationVerificationResult res = new RegistrationVerificationResult();
		if (!payload.has("discordUser")) {
			res.status = RegistrationVerificationStatus.DEFER_MISSING_FIELDS;
			res.errorMessage = "Missing discordUser field in verification payload.";
			return res;
		}

		// Find member
		for (Guild g : DiscordBotModule.getClient().getGuilds().toIterable()) {
			for (Member u : g.getMembers().toIterable()) {
				if (u.getTag().equals(payload.get("discordUser").getAsString())) {
					// Found it

					// Check code
					if (!payload.has("code")) {
						// Check link status
						if (LinkUtils.isPairedWithCenturia(u.getId().asString())) {
							res.status = RegistrationVerificationStatus.FAILURE;
							res.error = "discordaccount_already_used";
							res.errorMessage = "The selected Discord account already has a Centuria account paired.";
							return res;
						}

						// Set status
						res.status = RegistrationVerificationStatus.DEFER_REQUIRE_CODE;

						try {
							// Build message
							MessageCreateSpec.Builder msg = MessageCreateSpec.builder();

							// Build action
							String code = TimedActions.addAction(u.getId().asString() + "-apiregistration", () -> {
								// Generate code
								generateCode("apiregistration-" + u.getId().asString(), 15 * 60);
							}, 15 * 60);

							// Message content
							String content = "**" + DiscordBotModule.getServerName() + " Registration**\n";
							content += "\n";
							content += "Received a registration request for **" + DiscordBotModule.getServerName()
									+ "**\n";
							content += "Here follow the details of your requested account settings:\n";
							content += "**Account login name:** `" + accountName + "`\n";
							content += "**Account display name:** `" + displayName + "`\n";
							content += "\n";
							content += "If you wish to proceed with registration, __please have the "
									+ DiscordBotModule.getServerName() + " client ready.__\n";
							content += "After registration completes, the account will have no protection until you log in with the password you wish to use to verify future login attempts.\n";
							content += "\n";
							content += "Note: this request is only valid for 15 minutes.\n";
							content += "\n";
							content += "Select `Confirm Registration` below to continue...";
							msg.content(content);

							// Buttons
							msg.addComponent(ActionRow
									.of(Button.success("createaccount/" + u.getId().asString() + "/" + code + "/api",
											"Confirm Registration"), Button.primary("dismiss", "Cancel Registration")));

							// Send DM
							u.getPrivateChannel().block().createMessage(msg.build()).block();
						} catch (Exception e) {
							res.status = RegistrationVerificationStatus.FAILURE;
							res.error = "discordaccount_dm_failure";
							res.errorMessage = "The selected Discord account already has a Centuria account paired.";
							return res;
						}
					} else {
						// Handle code
						String code = payload.get("code").getAsString();

						// Verify code
						if (checkCode("apiregistration-" + u.getId().asString(), code)) {
							res.status = RegistrationVerificationStatus.SUCCESS;
							try {
								// Build message
								MessageCreateSpec.Builder msg = MessageCreateSpec.builder();

								// Set content
								msg.content("**Account created successfully**\n" + "Successfully registered your **"
										+ DiscordBotModule.getServerName() + "** account!\n\n**Login name:** `"
										+ accountName + "`\n**Display name:** `" + displayName
										+ "`\n\nThis account has been paired to this Discord account.");
								// Buttons
								msg.addComponent(ActionRow.of(Button.success("accountpanel", "Open account panel")));

								u.getPrivateChannel().block().createMessage(msg.build()).block();
							} catch (Exception e) {
							}
						} else {
							res.status = RegistrationVerificationStatus.INVALID_CODE;
						}
					}

					return res;
				}
			}
		}

		res.status = RegistrationVerificationStatus.FAILURE;
		res.errorMessage = "User not found in any mutual guild with the Discord Bot.";
		return res;
	}

	@Override
	public void postRegistration(CenturiaAccount account, String accountName, String displayName, JsonObject payload) {
		// Find member
		for (Guild g : DiscordBotModule.getClient().getGuilds().toIterable()) {
			for (Member u : g.getMembers().toIterable()) {
				if (u.getTag().equals(payload.get("discordUser").getAsString())) {
					// Found it
					// Pair accounts
					LinkUtils.pairAccount(account, u.getId().asString(), null, false, false);
				}
			}
		}
	}

}
