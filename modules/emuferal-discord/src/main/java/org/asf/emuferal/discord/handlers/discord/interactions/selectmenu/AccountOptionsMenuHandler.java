package org.asf.emuferal.discord.handlers.discord.interactions.selectmenu;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.asf.emuferal.accounts.AccountManager;
import org.asf.emuferal.accounts.EmuFeralAccount;
import org.asf.emuferal.accounts.PlayerInventory;
import org.asf.emuferal.discord.LinkUtils;
import org.asf.emuferal.discord.TimedActions;

import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.SelectMenuInteractionEvent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.component.SelectMenu;
import discord4j.core.object.component.TextInput;
import discord4j.core.object.component.SelectMenu.Option;
import discord4j.core.spec.InteractionApplicationCommandCallbackSpec;
import discord4j.core.spec.InteractionPresentModalSpec;
import discord4j.core.spec.MessageCreateSpec;
import reactor.core.publisher.Mono;

public class AccountOptionsMenuHandler {

	/**
	 * Handles the 'account config' select menu event
	 * 
	 * @param event   Select menu event
	 * @param gateway Discord client
	 * @return Result Mono object
	 */
	public static Mono<?> handle(String id, SelectMenuInteractionEvent event, GatewayDiscordClient gateway) {
		// Load option
		String option = event.getValues().get(0);

		// Find owner UserID
		String userID = event.getInteraction().getUser().getId().asString();

		// Find account
		EmuFeralAccount account = LinkUtils.getAccountByDiscordID(userID);
		if (account == null)
			return Mono.empty();

		// Reset selection
		event.getMessage().get().edit().withComponents(ActionRow.of(SelectMenu.of("accountoption",
				Option.of("Change display name", "displayname"),
				Option.of("Enable/disable 2-factor authentication", "2fa"),
				Option.of("Forgot password", "forgotpassword"), Option.of("Forgot login name", "forgotloginname"),
				Option.of("Download your account inventory (including avatars)", "downloaddata")))).subscribe();

		// Handle request
		switch (option) {

		// Display name
		case "displayname": {
			// Show display name change form
			InteractionPresentModalSpec.Builder modal = InteractionPresentModalSpec.builder();
			modal.title("Update display name");
			modal.addComponent(ActionRow.of(TextInput.small("displayname", "New display name", 2, 16).required()
					.prefilled(account.getDisplayName())));
			modal.customId("updatedisplayname");

			// Show form
			return event.presentModal(modal.build());
		}

		// 2-factor authentication
		case "2fa": {
			// Build message
			InteractionApplicationCommandCallbackSpec.Builder msg = InteractionApplicationCommandCallbackSpec.builder();

			// Get status of 2fa
			boolean enabled2fa = account.getPlayerInventory().containsItem("accountoptions")
					&& account.getPlayerInventory().getItem("accountoptions").getAsJsonObject().has("enable2fa")
					&& account.getPlayerInventory().getItem("accountoptions").getAsJsonObject().get("enable2fa")
							.getAsBoolean();

			// Message content
			msg.content("User account options: **2-factor authentication**");

			// Message buttons
			if (enabled2fa)
				msg.addComponent(ActionRow.of(
						Button.success("confirmenable2fa/" + userID + "/" + account.getAccountID(), "Enable")
								.disabled(),
						Button.danger("confirmdisable2fa/" + userID + "/" + account.getAccountID(), "Disable")));
			else
				msg.addComponent(ActionRow.of(
						Button.success("confirmenable2fa/" + userID + "/" + account.getAccountID(), "Enable"),
						Button.danger("confirmdisable2fa/" + userID + "/" + account.getAccountID(), "Disable")
								.disabled()));

			// Send message
			return event.reply(msg.build());
		}

		// Forgot password
		case "forgotpassword": {
			// Build message
			InteractionApplicationCommandCallbackSpec.Builder msg = InteractionApplicationCommandCallbackSpec.builder();

			// Message content
			String message = "**Received account password reset request.**\n";
			message += "\n";
			message += "A account password reset request was just made, here follow the details:\n";
			message += "**Account login name:** `" + account.getLoginName() + "`\n";
			message += "**Ingame player name:** `" + account.getDisplayName() + "`\n";
			message += "**Last login time:** "
					+ (account.getLastLoginTime() == -1 ? "`Unknown`" : "<t:" + account.getLastLoginTime() + ">")
					+ "\n";
			message += "**Requested at:** <t:" + System.currentTimeMillis() / 1000 + ">\n";
			message += "\n";
			message += "This request is only valid for 5 minutes.\n";
			message += "\n";
			message += "When clicking `Reset password`, the account password will be unlocked and the next login will be saved.\n";
			message += "__Please do not press reset until you started the Fer.al client, otherwise it might get abused.__\n";
			msg.content(message);

			// Schedule reset action
			String code = TimedActions.addAction(account.getAccountID() + "-forgotpassword", () -> {
				// Release password lock
				AccountManager.getInstance().makePasswordUpdateRequested(account.getAccountID());
			}, 5 * 60);

			// Buttons
			msg.addComponent(
					ActionRow.of(Button.danger("confirmresetpassword/" + userID + "/" + code, "Reset password"),
							Button.primary("dismiss", "Dismiss")));

			// Send message
			return event.reply(msg.build());
		}

		// Display login name
		case "forgotloginname": {
			// Build message
			InteractionApplicationCommandCallbackSpec.Builder msg = InteractionApplicationCommandCallbackSpec.builder();

			// Message content
			String message = "**Received a request to show the account login name.**\n";
			message += "\n";
			message += "Account login name: ||" + account.getLoginName() + "||\n";
			message += "\n";
			message += "__Please delete this message as soon as possible, the given information is sensitive.__";
			msg.content(message);

			// Buttons
			msg.addComponent(ActionRow.of(Button.success("dismissDelete", "Delete this message")));

			// Send message
			return event.reply(msg.build());
		}

		// Account inventory download
		case "downloaddata": {
			// Build message
			MessageCreateSpec.Builder msg = MessageCreateSpec.builder();

			// Message
			msg.content("The following zip contains your player inventory.\n"
					+ "Please note that some items aren't included for server protection.");

			ByteArrayOutputStream strm = new ByteArrayOutputStream();
			try {
				ZipOutputStream invZip = new ZipOutputStream(strm);
				// Add all inventory objects
				addItemToZip(account.getPlayerInventory(), "1", invZip);
				addItemToZip(account.getPlayerInventory(), "10", invZip);
				addItemToZip(account.getPlayerInventory(), "100", invZip);
				addItemToZip(account.getPlayerInventory(), "102", invZip);
				addItemToZip(account.getPlayerInventory(), "104", invZip);
				addItemToZip(account.getPlayerInventory(), "105", invZip);
				addItemToZip(account.getPlayerInventory(), "110", invZip);
				addItemToZip(account.getPlayerInventory(), "111", invZip);
				addItemToZip(account.getPlayerInventory(), "2", invZip);
				addItemToZip(account.getPlayerInventory(), "201", invZip);
				addItemToZip(account.getPlayerInventory(), "3", invZip);
				addItemToZip(account.getPlayerInventory(), "300", invZip);
				addItemToZip(account.getPlayerInventory(), "302", invZip);
				addItemToZip(account.getPlayerInventory(), "303", invZip);
				addItemToZip(account.getPlayerInventory(), "304", invZip);
				addItemToZip(account.getPlayerInventory(), "311", invZip);
				addItemToZip(account.getPlayerInventory(), "4", invZip);
				addItemToZip(account.getPlayerInventory(), "400", invZip);
				addItemToZip(account.getPlayerInventory(), "5", invZip);
				addItemToZip(account.getPlayerInventory(), "6", invZip);
				addItemToZip(account.getPlayerInventory(), "7", invZip);
				addItemToZip(account.getPlayerInventory(), "8", invZip);
				addItemToZip(account.getPlayerInventory(), "9", invZip);
				addItemToZip(account.getPlayerInventory(), "avatars", invZip);
				invZip.close();
				strm.close();
			} catch (IOException e) {
			}

			// Add file
			msg.addFile("inventory.zip", new ByteArrayInputStream(strm.toByteArray()));

			// Send message
			event.getInteraction().getChannel().block().createMessage(msg.build()).block();

			// Acknowledge interaction
			return event.deferEdit();
		}

		}

		// Default response
		return Mono.empty();
	}

	private static void addItemToZip(PlayerInventory inv, String item, ZipOutputStream zipStrm)
			throws UnsupportedEncodingException, IOException {
		if (inv.containsItem(item))
			transferDataToZip(zipStrm, item + ".json", inv.getItem(item).toString().getBytes("UTF-8"));
	}

	private static void transferDataToZip(ZipOutputStream zip, String file, byte[] data) throws IOException {
		zip.putNextEntry(new ZipEntry(file));
		zip.write(data);
		zip.closeEntry();
	}

}
