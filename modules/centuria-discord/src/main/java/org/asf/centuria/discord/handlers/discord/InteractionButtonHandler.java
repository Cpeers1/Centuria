package org.asf.centuria.discord.handlers.discord;

import org.asf.centuria.discord.handlers.discord.interactions.buttons.AppealButtonHandler;
import org.asf.centuria.discord.handlers.discord.interactions.buttons.BasicDismissDeleteHandler;
import org.asf.centuria.discord.handlers.discord.interactions.buttons.BasicDismissHandler;
import org.asf.centuria.discord.handlers.discord.interactions.buttons.DownloadSingleplayerLauncherHandler;
import org.asf.centuria.discord.handlers.discord.interactions.buttons.FeedbackReplyButtonHandler;
import org.asf.centuria.discord.handlers.discord.interactions.buttons.ReportReplyButtonHandler;
import org.asf.centuria.discord.handlers.discord.interactions.buttons.accountoptions.ConfirmDisable2fa;
import org.asf.centuria.discord.handlers.discord.interactions.buttons.accountoptions.ConfirmEnable2fa;
import org.asf.centuria.discord.handlers.discord.interactions.buttons.accountoptions.Disable2fa;
import org.asf.centuria.discord.handlers.discord.interactions.buttons.accountoptions.Enable2fa;
import org.asf.centuria.discord.handlers.discord.interactions.buttons.auth2fa.AllowHandler2fa;
import org.asf.centuria.discord.handlers.discord.interactions.buttons.auth2fa.BlockIpHandler2fa;
import org.asf.centuria.discord.handlers.discord.interactions.buttons.auth2fa.ConfirmAllowHandler2fa;
import org.asf.centuria.discord.handlers.discord.interactions.buttons.auth2fa.ConfirmBlockIpHandler2fa;
import org.asf.centuria.discord.handlers.discord.interactions.buttons.auth2fa.ConfirmRejectHandler2fa;
import org.asf.centuria.discord.handlers.discord.interactions.buttons.auth2fa.ConfirmWhitelistHandler2fa;
import org.asf.centuria.discord.handlers.discord.interactions.buttons.auth2fa.RejectHandler2fa;
import org.asf.centuria.discord.handlers.discord.interactions.buttons.auth2fa.WhitelistHandler2fa;
import org.asf.centuria.discord.handlers.discord.interactions.buttons.linking.RelinkButtonHandler;
import org.asf.centuria.discord.handlers.discord.interactions.buttons.linking.UnlinkButtonHandler;
import org.asf.centuria.discord.handlers.discord.interactions.buttons.panel.AccountPanelHandler;
import org.asf.centuria.discord.handlers.discord.interactions.buttons.panel.PairAccountHandler;
import org.asf.centuria.discord.handlers.discord.interactions.buttons.panel.RegisterAccountHandler;
import org.asf.centuria.discord.handlers.discord.interactions.buttons.password.BlockIpHandler;
import org.asf.centuria.discord.handlers.discord.interactions.buttons.password.ConfirmBlockIpHandler;
import org.asf.centuria.discord.handlers.discord.interactions.buttons.password.ConfirmResetPasswordHandler;
import org.asf.centuria.discord.handlers.discord.interactions.buttons.password.ResetPasswordHandler;
import org.asf.centuria.discord.handlers.discord.interactions.buttons.registration.CreateAccountHandler;

import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import reactor.core.publisher.Mono;

public class InteractionButtonHandler {

	/**
	 * Handles interaction buttons
	 * 
	 * @param event   Button event
	 * @param gateway Discord client
	 * @return Result Mono object
	 */
	public static Mono<?> handle(ButtonInteractionEvent event, GatewayDiscordClient gateway) {
		String id = event.getCustomId();

		if (id.startsWith("relink/") && id.split("/").length == 3) {
			return RelinkButtonHandler.handle(id, event, gateway);
		} else if (id.startsWith("unlink/") && id.split("/").length == 3) {
			return UnlinkButtonHandler.handle(id, event, gateway);
		} else if (id.startsWith("appeal/") && id.split("/").length == 3) {
			return AppealButtonHandler.handle(id, event, gateway);
		} else if (id.startsWith("doblockip/") && id.split("/").length == 4) {
			return BlockIpHandler.handle(id, event, gateway);
		} else if (id.startsWith("confirmblockip/") && id.split("/").length == 4) {
			return ConfirmBlockIpHandler.handle(id, event, gateway);
		} else if (id.startsWith("confirmresetpassword/") && id.split("/").length == 3) {
			return ConfirmResetPasswordHandler.handle(id, event, gateway);
		} else if (id.startsWith("doresetpassword/") && id.split("/").length == 3) {
			return ResetPasswordHandler.handle(id, event, gateway);
		} else if (id.equals("dismiss")) {
			return BasicDismissHandler.handle(id, event, gateway);
		} else if (id.equals("dismissDelete")) {
			return BasicDismissDeleteHandler.handle(id, event, gateway);
		} else if (id.equals("pair")) {
			return PairAccountHandler.handle(id, event, gateway);
		} else if (id.equals("register")) {
			return RegisterAccountHandler.handle(id, event, gateway);
		} else if (id.equals("accountpanel")) {
			return AccountPanelHandler.handle(id, event, gateway);
		} else if (id.startsWith("doblock2fa/") && id.split("/").length == 3) {
			return BlockIpHandler2fa.handle(id, event, gateway);
		} else if (id.startsWith("confirmblock2fa/") && id.split("/").length == 3) {
			return ConfirmBlockIpHandler2fa.handle(id, event, gateway);
		} else if (id.startsWith("dodeny2fa/") && id.split("/").length == 3) {
			return RejectHandler2fa.handle(id, event, gateway);
		} else if (id.startsWith("confirmdeny2fa/") && id.split("/").length == 3) {
			return ConfirmRejectHandler2fa.handle(id, event, gateway);
		} else if (id.startsWith("doallow2fa/") && id.split("/").length == 3) {
			return AllowHandler2fa.handle(id, event, gateway);
		} else if (id.startsWith("confirmallow2fa/") && id.split("/").length == 3) {
			return ConfirmAllowHandler2fa.handle(id, event, gateway);
		} else if (id.startsWith("dowhitelistip2fa/") && id.split("/").length == 3) {
			return WhitelistHandler2fa.handle(id, event, gateway);
		} else if (id.startsWith("confirmwhitelistip2fa/") && id.split("/").length == 3) {
			return ConfirmWhitelistHandler2fa.handle(id, event, gateway);
		} else if (id.startsWith("dodisable2fa/") && id.split("/").length == 3) {
			return Disable2fa.handle(id, event, gateway);
		} else if (id.startsWith("confirmdisable2fa/") && id.split("/").length == 3) {
			return ConfirmDisable2fa.handle(id, event, gateway);
		} else if (id.startsWith("doenable2fa/") && id.split("/").length == 3) {
			return Enable2fa.handle(id, event, gateway);
		} else if (id.startsWith("confirmenable2fa/") && id.split("/").length == 3) {
			return ConfirmEnable2fa.handle(id, event, gateway);
		} else if (id.startsWith("createaccount/") && id.split("/").length == 3) {
			return CreateAccountHandler.handle(id, event, gateway);
		} else if (id.startsWith("feedbackreply/") && id.split("/").length == 2) {
			return FeedbackReplyButtonHandler.handle(id, event, gateway);
		} else if (id.startsWith("reportreply/") && id.split("/").length == 3) {
			return ReportReplyButtonHandler.handle(id, event, gateway);
		} else if (id.equals("downloadsingleplayerlauncher")) {
			return DownloadSingleplayerLauncherHandler.handle(id, event, gateway);
		}

		// Default handler
		return Mono.empty();
	}

}
