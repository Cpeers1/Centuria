package org.asf.emuferal.discord.handlers.discord.interactions.forms;

import org.asf.emuferal.accounts.AccountManager;
import org.asf.emuferal.accounts.PlayerInventory;
import org.asf.emuferal.discord.DiscordBotModule;
import org.asf.emuferal.discord.LinkUtils;
import org.asf.emuferal.discord.TimedActions;

import com.google.gson.JsonObject;

import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ModalSubmitInteractionEvent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.spec.MessageCreateSpec;
import reactor.core.publisher.Mono;

public class RegistrationHandler {

	/**
	 * Handles the registration form submission event
	 * 
	 * @param event   Modal submission event
	 * @param gateway Discord client
	 * @return Result Mono object
	 */
	public static Mono<?> handle(String id, ModalSubmitInteractionEvent event, GatewayDiscordClient gateway) {
		// Load fields
		String accountName = event.getInteraction().getData().data().get().components().get().get(0).components().get()
				.get(0).value().get();
		String displayName = event.getInteraction().getData().data().get().components().get().get(1).components().get()
				.get(0).value().get();
		String pref2faSettings = event.getInteraction().getData().data().get().components().get().get(2).components()
				.get().get(0).value().get();

		// Load account manager
		AccountManager manager = AccountManager.getInstance();

		// Find UserID
		String userID = event.getInteraction().getUser().getId().asString();

		// Check if a account is already linked
		if (LinkUtils.isPairedWithEmuFeral(userID))
			return Mono.empty();

		// Verify validity of the 2fa option
		boolean enable2fa = pref2faSettings.equalsIgnoreCase("yes");
		if (!accountName.matches("^[A-Za-z0-9@._#]+$") || accountName.contains(".cred")
				|| !accountName.matches(".*[A-Za-z0-9]+.*") || accountName.isBlank() || accountName.length() > 320) {
			// Reply with error
			return event.reply("Invalid value for `Enable 2-factor authentication`").withEphemeral(true);
		}

		// Verify login name validity
		if (!accountName.matches("^[0-9A-Za-z\\-_. ]+") || accountName.length() > 16 || accountName.length() < 2) {
			// Reply with error
			return event.reply("Invalid login name.").withEphemeral(true);
		}

		// Verify name validity
		if (!displayName.matches("^[0-9A-Za-z\\-_. ]+") || displayName.length() > 16 || displayName.length() < 2) {
			// Reply with error
			return event.reply("Invalid display name.").withEphemeral(true);
		}

		// Check if the login name is in use
		if (manager.getUserByLoginName(accountName) != null) {
			// Reply with error
			return event.reply("Selected login name is already in use.").withEphemeral(true);
		}

		// Check if the name is in use
		if (manager.isDisplayNameInUse(displayName)) {
			// Reply with error
			return event.reply("Selected display name is already in use.").withEphemeral(true);
		}

		try {
			// Build message
			MessageCreateSpec.Builder msg = MessageCreateSpec.builder();

			// Build action
			String code = TimedActions.addAction(userID + "-registration", () -> {
				// Register the account
				String accountID = manager.register(accountName);
				if (accountID == null) {
					// Registration failed
					return;
				}

				// Assign and lock display name
				manager.getAccount(accountID).updateDisplayName(displayName);
				manager.lockDisplayName(displayName, accountID);

				// Make a default password so the change display name prompt wont show up
				manager.updatePassword(accountID, "emuferal".toCharArray());

				// Make sure the password gets updated
				manager.makePasswordUpdateRequested(accountID);

				// Pair accounts
				LinkUtils.pairAccount(manager.getAccount(accountID), userID, null, false, false);

				// Enable 2fa
				if (enable2fa) {
					PlayerInventory inv = manager.getAccount(accountID).getPlayerInventory();
					if (!inv.containsItem("accountoptions"))
						inv.setItem("accountoptions", new JsonObject());
					JsonObject config = inv.getItem("accountoptions").getAsJsonObject();
					if (config.has("enable2fa"))
						config.remove("enable2fa");
					config.addProperty("enable2fa", true);
					inv.setItem("accountoptions", config);
				}
			}, 5 * 60);

			// Message content
			String content = "**" + DiscordBotModule.getServerName() + " Registration**\n";
			content += "\n";
			content += "Received a registration request for **" + DiscordBotModule.getServerName() + "**\n";
			content += "Here follow the details of your requested account settings:\n";
			content += "**Account login name:** `" + accountName + "`\n";
			content += "**Account display name:** `" + displayName + "`\n";
			content += "**2-factor authentication:** `" + (enable2fa ? "Yes" : "No") + "`\n";
			content += "\n";
			content += "If you wish to proceed with registration, __please have the " + DiscordBotModule.getServerName()
					+ " client ready.__\n";
			content += "After registration completes, the account will have no protection until you log in with the password you wish to use to verify future login attempts.\n";
			content += "\n";
			content += "Note: this request is only valid for 5 minutes.\n";
			content += "\n";
			content += "Select `Confirm Registration` below to continue...";
			msg.content(content);

			// Buttons
			msg.addComponent(
					ActionRow.of(Button.success("createaccount/" + userID + "/" + code, "Confirm Registration"),
							Button.primary("dismiss", "Cancel Registration")));

			// Send DM
			event.getInteraction().getUser().getPrivateChannel().block().createMessage(msg.build()).block();
		} catch (Exception e) {
			return event
					.reply("Unable to send confirmation DM message, please make sure to enable DMs for this server.")
					.withEphemeral(true);
		}

		return event.deferEdit();
	}
}
