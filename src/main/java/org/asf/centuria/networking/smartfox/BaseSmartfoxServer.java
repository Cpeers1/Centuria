package org.asf.centuria.networking.smartfox;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import org.asf.centuria.Centuria;
import org.asf.centuria.packets.smartfox.ISmartfoxPacket;

public abstract class BaseSmartfoxServer {

	private ServerSocket server;
	private ArrayList<SmartfoxClient> clients = new ArrayList<SmartfoxClient>();
	private ArrayList<ISmartfoxPacket> packets = new ArrayList<ISmartfoxPacket>();
	private boolean setupComplete = false;

	public BaseSmartfoxServer(ServerSocket socket) {
		server = socket;

		// Register packets
		registerServerPackets();

		// Lock the registry
		setupComplete = true;
	}

	/**
	 * Called to create a socket smartfox client
	 * 
	 * @param client Client socket
	 * @return AbstractAbstractSmartfoxClient instance
	 */
	protected abstract SmartfoxClient createSocketClient(Socket client);

	/**
	 * Registers the server packets (internal)
	 */
	protected abstract void registerServerPackets();

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
		// Server logic
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

		// Start watchdog
		serverProcessor = new Thread(() -> {
			// Server watchdog loop
			while (server != null) {
				// Tick all client PPS rates
				for (SmartfoxClient client : getClients()) {
					client.updatePPS();
				}

				// Wait
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
				}
			}
		}, "Smartfox Server Watchdog Thread: " + this.getClass().getSimpleName());
		serverProcessor.setDaemon(true);
		serverProcessor.start();
	}

	// Client system
	private void runClient(Socket clientSocket) {
		// Start the client thread
		Thread th = new Thread(() -> {
			// Create socket client
			SmartfoxClient client = createSocketClient(clientSocket);

			// Add client
			synchronized (clients) {
				clients.add(client);
			}

			// Non-debug
			if (!Centuria.debugMode) {
				try {
					// Run start code
					startClient(client);

					// Client loop
					while (client.isConnected()) {
						String data = readRawPacket(client);
						if (Centuria.debugMode)
							Centuria.logger.debug("C->S: " + data);
						try {
							handle(data, client);
						} catch (Exception e) {
							if (!(e instanceof IOException)) {
								Centuria.logger.error("Connection died!", e);
							}
							client.closeClient();
							break;
						}
					}

					// Disconnect
					if (client.isConnected()) {
						// Disconnected
						clientDisconnect(client);
						client.stop();
					}

					// Remove from list
					synchronized (clients) {
						clients.remove(client);
					}
				} catch (Exception e) {
					// Disconnected before handshake
					if (!(e instanceof IOException)) {
						Centuria.logger.error("Connection died!", e);
					}
					client.closeClient();

					// Remove from list
					synchronized (clients) {
						clients.remove(client);
					}
					return;
				}
			} else {
				// Debug mode

				// Run start code
				try {
					startClient(client);
				} catch (IOException e1) {
					// Remove from list
					synchronized (clients) {
						clients.remove(client);
					}
					throw new RuntimeException(e1);
				}

				// Client loop
				while (client.isConnected()) {
					String data;
					try {
						data = readRawPacket(client);
					} catch (IOException e1) {
						client.closeClient();
						return;
					}
					if (Centuria.debugMode)
						Centuria.logger.debug("C->S: " + data);
					try {
						handle(data, client);
					} catch (IOException e) {
						client.closeClient();
						break;
					}
				}

				// Disconnect
				if (client.isConnected()) {
					// Disconnected
					clientDisconnect(client);
					client.stop();
				}

				// Remove from list
				synchronized (clients) {
					clients.remove(client);
				}
			}
		}, "Smartfox Client Thread: " + clientSocket);
		th.setDaemon(true);
		th.start();
	}

	/**
	 * Packet handler (internal)
	 * 
	 * @param data   Packet data to handle
	 * @param client Smartfox client
	 * @throws IOException If handling fails
	 */
	protected void handle(String data, SmartfoxClient client) throws IOException {
		if (!handlePacket(data, client)) {
			// Allow debug mode to re-register packets
			if (Centuria.debugMode) {
				packets.clear();
				setupComplete = false;
				registerServerPackets();
				setupComplete = true;
			}
			Centuria.logger.error("Unhandled packet: client " + client.getAddress() + " sent: " + data);
		}
	}

	/**
	 * Stops the server
	 */
	public void stop() {
		// Stop server
		try {
			server.close();
		} catch (IOException e) {
		}
		server = null;

		// Disconnect clients
		for (SmartfoxClient client : getClients()) {
			client.disconnect();
		}
		synchronized (clients) {
			clients.clear();
		}
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
	 * @param client Player to send the packet to
	 * @param packet Packet to send
	 */
	public void sendPacket(SmartfoxClient client, ISmartfoxPacket packet) {
		client.sendPacket(packet);
	}

	/**
	 * Sends a raw packet to a specific player
	 *
	 * @param client Player to send the packet to
	 * @param packet Packet to send
	 */
	public void sendPacket(SmartfoxClient client, String packet) {
		client.sendPacket(packet);
	}

	/**
	 * Reads a single packet from a client
	 *
	 * @param <T>        Packet return type
	 * @param client     Player to read from
	 * @param packetType Expected packet class
	 * @return ISmartfoxPacket instance or null
	 * @throws IOException If reading fails
	 */
	public <T extends ISmartfoxPacket> T readPacket(SmartfoxClient client, Class<T> packetType) throws IOException {
		return client.readPacket(packetType);
	}

	/**
	 * Parses a packet
	 * 
	 * @param <T>        Packet type
	 * @param packet     Raw packet
	 * @param packetType Packet type
	 * @return Packet instance or null
	 * @throws IOException If parsing fails
	 */
	protected <T extends ISmartfoxPacket> T parsePacketPayload(String packet, Class<T> packetType) throws IOException {
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

	/**
	 * Handles a packet
	 *
	 * @param packet Packet content to handle
	 * @param client Smartfox client
	 * @return True if handled successfully, false otherwise
	 */
	public boolean handlePacket(String packet, SmartfoxClient client) throws IOException {
		// Find a packet
		for (ISmartfoxPacket pkt : packets) {
			if (pkt.canParse(packet)) {
				// Found a compatible packet, instantiate it and parse
				ISmartfoxPacket res = pkt.instantiate();
				if (!res.parse(packet))
					continue; // Apparently this packet doesnt support the payload, odd

				// Handle it
				if (res.handle(client))
					return true; // It was handled, lets return true and end the loop
			}
		}

		return false;
	}

	/**
	 * Reads a single raw packet
	 *
	 * @param client Client to read from
	 * @return Packet string
	 * @throws IOException If reading fails
	 */
	public String readRawPacket(SmartfoxClient client) throws IOException {
		return client.readRawPacket();
	}

	/**
	 * Retrieves all connected clients
	 * 
	 * @return Array of SmartfoxClient instances
	 */
	public SmartfoxClient[] getClients() {
		synchronized (clients) {
			return clients.toArray(t -> new SmartfoxClient[t]);
		}
	}
}
