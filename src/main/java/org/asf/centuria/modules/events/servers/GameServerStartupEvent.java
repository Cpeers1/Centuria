package org.asf.centuria.modules.events.servers;

import java.util.function.Consumer;

import org.asf.centuria.modules.eventbus.EventObject;
import org.asf.centuria.networking.gameserver.GameServer;
import org.asf.centuria.packets.smartfox.ISmartfoxPacket;

/**
 * 
 * GameServer Startup Event - used to handle startup of the game server and
 * register packets via modules.
 * 
 * @author Sky Swimmer - AerialWorks Software Foundation
 *
 */
public class GameServerStartupEvent extends EventObject {

	private GameServer server;
	private Consumer<ISmartfoxPacket> registrationCommand;

	public GameServerStartupEvent(GameServer server, Consumer<ISmartfoxPacket> registrationCommand) {
		this.server = server;
		this.registrationCommand = registrationCommand;
	}

	/**
	 * Retrieves the game server
	 * 
	 * @return GameServer instance
	 */
	public GameServer getServer() {
		return server;
	}

	/**
	 * Registers packets on the game server
	 * 
	 * @param packet Packet to register
	 */
	public void registerPacket(ISmartfoxPacket packet) {
		registrationCommand.accept(packet);
	}

}
