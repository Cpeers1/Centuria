package org.asf.centuria.networking.smartfox;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Base64;
import java.util.zip.GZIPInputStream;

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
			SmartfoxClient client = new SmartfoxClient(clientSocket, this);

			// Non-debug
			if (!Centuria.debugMode) {
				try {
					// Run start code
					startClient(client);

					// Client loop
					while (client.getSocket() != null) {
						String data = readRawPacket(client);
						try {
							handle(data, client);
						} catch (Exception e) {
							if (!(e instanceof IOException)) {
								Centuria.logger.error("Connection died!", e);
							}
							try {
								client.getSocket().close();
							} catch (Exception e2) {
							}
							if (client.getSocket() != null) {
								clientDisconnect(client);
								client.stop();
							}
							return;
						}
					}

					if (client.getSocket() != null) {
						// Disconnected
						clientDisconnect(client);
						client.stop();
					}
				} catch (Exception e) {
					if (!(e instanceof IOException)) {
						Centuria.logger.error("Connection died!", e);
					}
					try {
						client.getSocket().close();
					} catch (Exception e2) {
					}
					if (client.getSocket() != null) {
						clientDisconnect(client);
						client.stop();
					}
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
				while (client.getSocket() != null) {
					String data;
					try {
						data = readRawPacket(client);
					} catch (IOException e1) {
						try {
							client.getSocket().close();
						} catch (Exception e2) {
						}
						if (client.getSocket() != null) {
							clientDisconnect(client);
							client.stop();
						}
						return;
					}
					try {
						handle(data, client);
					} catch (IOException e) {
						try {
							client.getSocket().close();
						} catch (Exception e2) {
						}
						if (client.getSocket() != null) {
							clientDisconnect(client);
							client.stop();
						}
						return;
					}
				}

				if (client.getSocket() != null) {
					// Disconnected
					clientDisconnect(client);
					client.stop();
				}
			}
		}, "Smartfox Client Thread: " + clientSocket);
		th.setDaemon(true);
		th.start();
	}

	private void handle(String data, SmartfoxClient client) throws IOException {
		if (!handlePacket(data, client)) {
			// Allow debug mode to re-register packets
			if (Centuria.debugMode) {
				packets.clear();
				setupComplete = false;
				registerPackets();
				setupComplete = true;
			}

			System.err.println("Unhandled packet: client " + client.getSocket() + " sent: " + data);
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
	 * @param smartfoxClient Player to send the packet to
	 * @param packet         Packet to send
	 * @throws IOException If transmission fails
	 */
	public void sendPacket(SmartfoxClient smartfoxClient, ISmartfoxPacket packet) throws IOException {
		smartfoxClient.sendPacket(packet);
	}

	/**
	 * Sends a raw packet to a specific player
	 *
	 * @param smartfoxClient Player to send the packet to
	 * @param packet         Packet to send
	 * @throws IOException If transmission fails
	 */
	public void sendPacket(SmartfoxClient smartfoxClient, String packet) throws IOException {
		smartfoxClient.sendPacket(packet);
	}

	/**
	 * Reads a single packet from a client
	 *
	 * @param <T>            Packet return type
	 * @param smartfoxClient Player to read from
	 * @param packetType     Expected packet class
	 * @return ISmartfoxPacket instance or null
	 * @throws IOException If reading fails
	 */
	public <T extends ISmartfoxPacket> T readPacket(SmartfoxClient smartfoxClient, Class<T> packetType)
			throws IOException {
		// Read data
		String data = readRawPacket(smartfoxClient);

		// Parse packet
		return parsePacketPayload(data, packetType);
	}

	private <T extends ISmartfoxPacket> T parsePacketPayload(String packet, Class<T> packetType) throws IOException {
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
	 * @param smartfoxClient Client to read from
	 * @return Packet string
	 * @throws IOException If reading fails
	 */
	public String readRawPacket(SmartfoxClient smartfoxClient) throws IOException {
		String payload = new String();
		while (true) {
			int b = smartfoxClient.input.read();
			if (b == -1) {
				throw new IOException("Stream closed");
			} else if (b == 0) {
				// Solve for the XT issue
				if (payload.startsWith("%xt|n%"))
					payload = "%xt%" + payload.substring("%xt|n%".length());

				// Compression
				if (payload.startsWith("$")) {
					// Decompress packet
					byte[] compressedData = Base64.getDecoder().decode(payload.substring(1));
					GZIPInputStream dc = new GZIPInputStream(new ByteArrayInputStream(compressedData));
					byte[] newData = dc.readAllBytes();
					dc.close();
					payload = new String(newData, "UTF-8");
				}

				return payload;
			} else
				payload += (char) b;
		}
	}
}
