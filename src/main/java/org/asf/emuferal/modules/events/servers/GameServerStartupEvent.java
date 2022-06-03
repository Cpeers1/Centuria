package org.asf.emuferal.modules.events.servers;

import java.util.Map;
import java.util.function.Consumer;

import org.asf.emuferal.modules.eventbus.EventObject;
import org.asf.emuferal.modules.eventbus.EventPath;
import org.asf.emuferal.networking.gameserver.GameServer;
import org.asf.emuferal.packets.smartfox.ISmartfoxPacket;

/**
 * 
 * GameServer Startup Event - used to handle startup of the game server and
 * register packets via modules.
 * 
 * @author Sky Swimmer - AerialWorks Software Foundation
 *
 */
@EventPath("gameserver.startup")
public class GameServerStartupEvent extends EventObject {

	private GameServer server;
	private Consumer<ISmartfoxPacket> registrationCommand;

	public GameServerStartupEvent(GameServer server, Consumer<ISmartfoxPacket> registrationCommand) {
		this.server = server;
		this.registrationCommand = registrationCommand;
	}

	@Override
	public String eventPath() {
		return "gameserver.startup";
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

	@Override
	public Map<String, String> eventProperties() {
		return Map.of();
	}

}
