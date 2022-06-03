package org.asf.emuferal.modules.events.chatcommands;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Map;

import org.asf.emuferal.accounts.EmuFeralAccount;
import org.asf.emuferal.modules.eventbus.EventObject;
import org.asf.emuferal.modules.eventbus.EventPath;
import org.asf.emuferal.networking.chatserver.ChatClient;
import org.asf.emuferal.networking.chatserver.ChatServer;
import org.asf.emuferal.networking.gameserver.GameServer;

/**
 * 
 * Event called to register command syntaxes (for the help command)
 * 
 * @author Sky Swimmer - AerialWorks Software Foundation
 *
 */
@EventPath("chatcommands.helpsyntax")
public class ModuleCommandSyntaxListEvent extends EventObject {

	private ArrayList<String> commandMessages;

	private String permLevel;
	private ChatClient client;
	private ChatServer server;
	private EmuFeralAccount account;

	public ModuleCommandSyntaxListEvent(ArrayList<String> commandMessages, ChatClient client, EmuFeralAccount account,
			String permLevel) {
		this.commandMessages = commandMessages;
		this.client = client;
		this.server = client.getServer();
		this.account = account;
		this.permLevel = permLevel;
	}

	@Override
	public String eventPath() {
		return "chatcommands.helpsyntax";
	}

	/**
	 * Retrieves the chat client instance
	 * 
	 * @return Chat client instance
	 */
	public ChatClient getClient() {
		return client;
	}

	/**
	 * Retrieves the chat server instance
	 * 
	 * @return Chat server instance
	 */
	public ChatServer getServer() {
		return server;
	}

	/**
	 * Retrieves the player account object
	 * 
	 * @return EmuFeralAccount instance
	 */
	public EmuFeralAccount getAccount() {
		return account;
	}

	/**
	 * Adds a command syntax message to the help command
	 * 
	 * @param message Message to add
	 */
	public void addCommandSyntaxMessage(String message) {
		commandMessages.add(message);
	}

	/**
	 * Checks if the player has a specific permission level
	 * 
	 * @param perm Permission level to check
	 * @return True if the player has the given permission level or greater, false
	 *         otherwise
	 */
	public boolean hasPermission(String perm) {
		return GameServer.hasPerm(permLevel, perm);
	}

	@Override
	public Map<String, String> eventProperties() {
		return Map.of("accountId", account.getAccountID(), "playerName", account.getDisplayName(), "accountName",
				account.getLoginName(), "address",
				((InetSocketAddress) client.getSocket().getRemoteSocketAddress()).getAddress().getHostAddress());
	}

}
