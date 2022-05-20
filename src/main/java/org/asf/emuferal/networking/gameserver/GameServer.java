package org.asf.emuferal.networking.gameserver;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Random;

import org.asf.emuferal.networking.smartfox.BaseSmartfoxServer;
import org.asf.emuferal.networking.smartfox.SmartfoxClient;
import org.asf.emuferal.packets.smartfox.ISmartfoxPacket;
import org.asf.emuferal.packets.xml.VersionHandshakePackets;
import org.asf.emuferal.packets.xml.handshake.version.ClientToServerHandshake;
import org.asf.emuferal.packets.xml.handshake.version.ServerToClientOK;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;

public class GameServer extends BaseSmartfoxServer {

	// Random number generator
	private Random rnd = new Random();

	// XML
	private XmlMapper mapper = new XmlMapper();

	public GameServer(ServerSocket socket) {
		super(socket);
	}

	@Override
	protected void registerPackets() {
		// Handshake
		registerPacket(new ClientToServerHandshake(mapper));
	}

	@Override
	protected void startClient(SmartfoxClient client) throws IOException {
		// Read first handshake packet
		ClientToServerHandshake pk = client.readPacket(ClientToServerHandshake.class);

		// Check version
		boolean badClient = false;
		if (!pk.actionField.equals("verChk") || !pk.clientBuild.equals("165")) {
			badClient = true; // Ok, bad version, lets make sure the client disconnects
		}

		// Send response so that the client moves on
		client.sendPacket(new ServerToClientOK(mapper));

		// Random key
		client.readPacket(ISmartfoxPacket.class);
		String key = Long.toString(rnd.nextLong(), 16);
		client.sendPacket("<msg t='sys'><body action='rndK' r='-1'><k>" + key + "</k></body></msg>");

		// Authenticate the player
		
		client = client;
	}

	@Override
	protected void clientDisconnect(SmartfoxClient client) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void onStart() {
		// TODO Auto-generated method stub

	}

	@Override
	protected void onStop() {
		// TODO Auto-generated method stub

	}

}
