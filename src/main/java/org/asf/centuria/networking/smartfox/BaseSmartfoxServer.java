package org.asf.centuria.networking.smartfox;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import org.asf.centuria.Centuria;
import org.asf.centuria.packets.smartfox.ISmartfoxPacket;

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
	 * Called to create a socket smartfox client
	 * 
	 * @param client Client socket
	 * @return AbstractAbstractSmartfoxClient instance
	 */
	protected abstract SmartfoxClient createSocketClient(Socket client);

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
		// Start the client thread
		Thread th = new Thread(() -> {
			SmartfoxClient client = createSocketClient(clientSocket);

			// Non-debug
			if (!Centuria.debugMode) {
				try {
					// Run start code
					startClient(client);

					// Client loop
					while (client.isConnected()) {
						String data = readRawPacket(client);
						try {
							handle(data, client);
						} catch (Exception e) {
							if (!(e instanceof IOException)) {
								Centuria.logger.error("Connection died!", e);
							}
							client.closeClient();
							return;
						}
					}

					if (client.isConnected()) {
						// Disconnected
						clientDisconnect(client);
						client.stop();
					}
				} catch (Exception e) {
					if (!(e instanceof IOException)) {
						Centuria.logger.error("Connection died!", e);
					}
					client.closeClient();
					return;
				}
			} else {
				// Debug mode

				// Run start code
				try {
					startClient(client);
				} catch (IOException e1) {
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
					try {
						handle(data, client);
					} catch (IOException e) {
						client.closeClient();
						return;
					}
				}

				if (client.isConnected()) {
					// Disconnected
					clientDisconnect(client);
					client.stop();
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
				registerPackets();
				setupComplete = true;
			}

			Centuria.logger.error("Unhandled packet: client " + client.getAddress() + " sent: " + data);
		}
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
	 * @param <T>                    Packet return type
	 * @param AbstractSmartfoxClient Player to read from
	 * @param packetType             Expected packet class
	 * @return ISmartfoxPacket instance or null
	 * @throws IOException If reading fails
	 */
	public <T extends ISmartfoxPacket> T readPacket(SmartfoxClient AbstractSmartfoxClient, Class<T> packetType)
			throws IOException {
		// Read data
		String data = readRawPacket(AbstractSmartfoxClient);

		// Parse packet
		return parsePacketPayload(data, packetType);
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
}
