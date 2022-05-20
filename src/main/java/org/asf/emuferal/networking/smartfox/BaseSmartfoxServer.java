package org.asf.emuferal.networking.smartfox;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

import org.asf.emuferal.packets.smartfox.ISmartfoxPacket;

public abstract class BaseSmartfoxServer {

	private ServerSocket server;
	private ArrayList<ISmartfoxPacket> packets = new ArrayList<ISmartfoxPacket>();
	private boolean setupComplete = false;

	public BaseSmartfoxServer(ServerSocket socket) {
		server = socket;

		// Register packets
		registerPackets();

		// Lock the registry
		setupComplete = true;
	}

	/**
	 * Registers the server packets (internal)
	 */
	protected abstract void registerPackets();

	/**
	 * Client start event (internal)
	 */
	protected abstract void startClient(SmartfoxClient client) throws IOException;

	/**
	 * Client disconnect event (internal)
	 */
	protected abstract void clientDisconnect(SmartfoxClient client);

	/**
	 * Server startup event (internal)
	 */
	protected abstract void onStart();

	/**
	 * Server shutdown event (internal)
	 */
	protected abstract void onStop();

	/**
	 * Registers a packet type
	 * 
	 * @param packet ISmartfoxPacket instance
	 */
	protected void registerPacket(ISmartfoxPacket packet) {
		if (setupComplete)
			throw new IllegalStateException("Registry has been locked");
		packets.add(packet);
	}

	/**
	 * Runs the server
	 */
	public void start() {
		Thread serverProcessor = new Thread(() -> {
			// Run start code
			onStart();

			// Server loop
			while (server != null) {
				try {
					Socket client = server.accept();
					runClient(client);
				} catch (IOException ex) {
					server = null;
					break;
				}
			}

			// Shutdown
			onStop();
		}, "Smartfox Server Thread: " + this.getClass().getSimpleName());
		serverProcessor.setDaemon(true);
		serverProcessor.start();
	}

	// Client system
	private void runClient(Socket clientSocket) {
		SmartfoxClient client = new SmartfoxClient(clientSocket, this);

		// Start the client thread
		Thread th = new Thread(() -> {
			try {
				// Run start code
				startClient(client);

				// Client loop
				while (client.getSocket() != null) {
					String data = readPacketString(client);
					if (!runPacket(data)) {
						System.err.println("Unhandled packet: client " + client + " sent: " + data);
					}
				}

				// Disconnected
				clientDisconnect(client);
			} catch (Exception e) {
				try {
					client.getSocket().close();
				} catch (IOException e2) {
				}
				if (client.getSocket() != null) {
					clientDisconnect(client);
				}
			}
		}, "Smartfox Client Thread: " + clientSocket);
		th.setDaemon(true);
		th.start();
	}

	/**
	 * Stops the server
	 */
	public void stop() {
		try {
			server.close();
		} catch (IOException e) {
		}
		server = null;
	}

	/**
	 * Retrieves the server socket
	 * 
	 * @return ServerSocket instance or null
	 */
	public ServerSocket getServerSocket() {
		return server;
	}

	/**
	 * Sends a packet to a specific player
	 * 
	 * @param smartfoxClient Player to send the packet to
	 * @param packet         Packet to send
	 * @throws IOException If transmission fails
	 */
	public synchronized void sendPacket(SmartfoxClient smartfoxClient, ISmartfoxPacket packet) throws IOException {
		// Instantiate the packet and build
		ISmartfoxPacket inst = packet.instantiate();
		String content = inst.build();

		// Send packet
		byte[] payload = content.getBytes("UTF-8");
		smartfoxClient.getSocket().getOutputStream().write(payload);
		smartfoxClient.getSocket().getOutputStream().write(0);
		smartfoxClient.getSocket().getOutputStream().flush();
	}

	/**
	 * Sends a raw packet to a specific player
	 * 
	 * @param smartfoxClient Player to send the packet to
	 * @param packet         Packet to send
	 * @throws IOException If transmission fails
	 */
	public synchronized void sendPacket(SmartfoxClient smartfoxClient, String packet) throws IOException {
		// Send packet
		byte[] payload = packet.getBytes("UTF-8");
		smartfoxClient.getSocket().getOutputStream().write(payload);
		smartfoxClient.getSocket().getOutputStream().write(0);
		smartfoxClient.getSocket().getOutputStream().flush();
	}

	/**
	 * Reads a single packet from a client
	 * 
	 * @param <T>            Packet return type
	 * @param smartfoxClient Player to read from
	 * @param packetType     Expected packet class
	 * @throws IOException If reading fails
	 * @return ISmartfoxPacket instance or null
	 */
	public synchronized <T extends ISmartfoxPacket> T readPacket(SmartfoxClient smartfoxClient, Class<T> packetType)
			throws IOException {
		// Read data
		String data = readPacketString(smartfoxClient);

		// Parse packet
		return parsePacketPayload(data, packetType);
	}

	private <T extends ISmartfoxPacket> T parsePacketPayload(String packet, Class<T> packetType) {
		// Find a packet
		for (ISmartfoxPacket pkt : packets) {
			if (pkt.canParse(packet) && packetType.isAssignableFrom(pkt.getClass())) {
				// Found a compatible packet, instantiate it and parse
				@SuppressWarnings("unchecked")
				T res = (T) pkt.instantiate();
				if (!res.parse(packet))
					continue; // Apparently this packet doesnt support the payload, odd

				return res;
			}
		}

		// Could not find a packet that matched
		return null;
	}

	private boolean runPacket(String packet) throws IOException {
		// Find a packet
		for (ISmartfoxPacket pkt : packets) {
			if (pkt.canParse(packet)) {
				// Found a compatible packet, instantiate it and parse
				ISmartfoxPacket res = pkt.instantiate();
				if (!res.parse(packet))
					continue; // Apparently this packet doesnt support the payload, odd

				// Handle it
				if (res.handle())
					return true; // It was handled, lets return true and end the loop
			}
		}

		return false;
	}

	// Reads a packet
	private synchronized String readPacketString(SmartfoxClient smartfoxClient) throws IOException {
		String payload = new String();
		while (true) {
			int b = smartfoxClient.getSocket().getInputStream().read();
			if (b == -1) {
				throw new IOException("Stream closed");
			} else if (b == 0) {
				return payload;
			} else
				payload += (char) b;
		}
	}
}
