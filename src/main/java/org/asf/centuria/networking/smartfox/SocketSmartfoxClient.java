package org.asf.centuria.networking.smartfox;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.zip.GZIPInputStream;

import org.asf.centuria.Centuria;
import org.asf.centuria.packets.smartfox.ISmartfoxPacket;
import org.asf.centuria.util.io.DataReader;
import org.asf.centuria.util.io.DataWriter;
import org.asf.connective.tasks.AsyncTaskManager;

public class SocketSmartfoxClient extends SmartfoxClient {

	private Socket client;
	private BaseSmartfoxServer server;
	private String messageBuffer = "";

	private Object sendLock = new Object();
	private Object readLock = new Object();

	private DataReader reader;
	private DataWriter writer;

	private ArrayList<String> sendQueue = new ArrayList<String>();

	private boolean ioThreadInited;
	private boolean disconnecting = false;

	InputStream input;
	OutputStream output;

	public SocketSmartfoxClient(Socket client, BaseSmartfoxServer server) {
		this.client = client;
		this.server = server;

		// IO streams
		try {
			input = client.getInputStream();
			output = client.getOutputStream();
		} catch (IOException e) {
		}
	}

	/**
	 * Avoid usage from the gameserver API, this is non-standard
	 */
	public Socket getSocket() {
		return client;
	}

	@Override
	protected void stop() {
		client = null;
		reader = null;
		writer = null;
		disconnecting = false;
		sendQueue.clear();
	}

	@Override
	public boolean isConnected() {
		return client != null && !disconnecting;
	}

	@Override
	public void disconnect() {
		closeClient();
	}

	@Override
	public void sendPacket(ISmartfoxPacket packet) {
		try {
			sendPacket(packet.build());
		} catch (Exception e) {
		}
	}

	@Override
	public void sendPacket(String packetToSend) {
		if (!ioThreadInited) {
			ioThreadInited = true;

			// Start IO
			AsyncTaskManager.runAsync(() -> {
				// IO Logic
				while (client != null) {
					// Pick up next packet awaiting transmission
					String packet = null;
					synchronized (sendLock) {
						// Check
						if (sendQueue.size() != 0) {
							// Get packet
							packet = sendQueue.get(0);
						}
					}

					// Check result
					if (packet == null) {
						try {
							Thread.sleep(10);
						} catch (InterruptedException e) {
						}
						continue;
					}

					// Send
					if (Centuria.debugMode)
						Centuria.logger.debug("S->C: " + packet);

					// Check protocol mode
					if (shouldUseEfgl()) {
						// EFGL
						try {
							// Prepare writer if missing
							if (writer == null)
								writer = new DataWriter(output);

							// Write
							writer.writeString(packet);

							// Remove from queue
							synchronized (sendLock) {
								sendQueue.remove(0);
							}
						} catch (Exception e) {
							// Failed to send
							// Assume disconnect
							sendQueue.clear();
							break;
						}
					} else {
						// SFS1X
						try {
							// Send packet
							byte[] payload = packet.getBytes("UTF-8");
							if (client == null)
								return;
							output.write(payload);
							output.write(0);
							output.flush();

							// Remove from queue
							synchronized (sendLock) {
								sendQueue.remove(0);
							}
						} catch (Exception e) {
							// Failed to send
							// Assume disconnect
							sendQueue.clear();
							break;
						}
					}
				}

				// End
				ioThreadInited = false;
			});
		}
		if (!isConnected())
			return;
		synchronized (sendLock) {
			// Add
			if (!isConnected())
				return;
			sendQueue.add(packetToSend);
		}
	}

	@Override
	public BaseSmartfoxServer getServer() {
		return server;
	}

	@Override
	public String readRawPacket() throws IOException {
		lockProtocol();
		synchronized (readLock) {
			// Check protocol mode
			if (shouldUseEfgl()) {
				// EFGL-mode
				// Prepare data reader if missing
				if (reader == null)
					reader = new DataReader(input);

				// Read message
				String packet = reader.readString();
				return packet;
			} else if (protocolSwitchPossible()) {
				// Byte-by-byte mode so that a switch can still be performed
				String buffer = "";
				int b = input.read();
				while (b != 0 && b != -1) {
					buffer += (char) b;
					b = input.read();
				}
				if (b == -1)
					throw new IOException("Stream closed unexpectedly");
				return buffer;
			}

			// Read in regular-performance mode
			// Go over received messages
			String res = findFirstPacket(messageBuffer);
			if (res != null) {
				onPacketReceived();
				if (Centuria.debugMode)
					Centuria.logger.debug("C->S: " + res);
				return res; // Received a message
			}

			// Read messages
			while (true) {
				// Read bytes
				byte[] buffer = new byte[20480];
				int read = input.read(buffer);
				if (read <= -1) {
					// Go over received messages
					res = findFirstPacket(messageBuffer);
					if (res != null) {
						onPacketReceived();
						if (Centuria.debugMode)
							Centuria.logger.debug("C->S: " + res);
						return res; // Received a message
					}

					// Throw exception
					throw new IOException("Stream closed");
				}
				buffer = Arrays.copyOfRange(buffer, 0, read);

				// Load messages string, combining the previous buffer with the current one
				String messages = messageBuffer + new String(buffer, "UTF-8");
				res = findFirstPacket(messages);
				if (res != null) {
					onPacketReceived();
					if (Centuria.debugMode)
						Centuria.logger.debug("C->S: " + res);
					return res; // Received a message
				}

				// Push remaining bytes to the next message
				messageBuffer = messages;
			}
		}
	}

	private String findFirstPacket(String messages) throws IOException {
		// Go over received messages
		if (messages.contains("\0")) {
			// Pending message found
			String message = messages.substring(0, messages.indexOf("\0"));

			// Push remaining bytes to the next message
			messages = messages.substring(messages.indexOf("\0") + 1);
			messageBuffer = messages;

			// Solve for the XT issue
			if (message.startsWith("%xt|n%"))
				message = "%xt%" + message.substring("%xt|n%".length());

			// Compression
			if (message.startsWith("$")) {
				// Decompress packet
				byte[] compressedData = Base64.getDecoder().decode(message.substring(1));
				GZIPInputStream dc = new GZIPInputStream(new ByteArrayInputStream(compressedData));
				byte[] newData = dc.readAllBytes();
				dc.close();
				message = new String(newData, "UTF-8");
			}

			// Handle
			return message;
		}
		return null;
	}

	@Override
	public String getAddress() {
		InetSocketAddress ip = (InetSocketAddress) client.getRemoteSocketAddress();
		InetAddress addr = ip.getAddress();
		String ipaddr = addr.getHostAddress();
		return ipaddr;
	}

	@Override
	protected void closeClient() {
		// Wait for queue to flush
		disconnecting = true;
		int lastSize = 0;
		long lastSent = System.currentTimeMillis();
		boolean hasPackets = true;
		while (hasPackets) {
			int lastPSize = lastSize;
			synchronized (sendQueue) {
				lastSize = sendQueue.size();
				hasPackets = lastSize > 0;
			}

			// Wait
			if (hasPackets) {
				// Reset timer
				if (lastSize != lastPSize)
					lastSent = System.currentTimeMillis();

				// Check time
				if (System.currentTimeMillis() - lastSent > 15000)
					break; // Terminate connection and send logic, taking too long to send
			}
		}

		// Close
		try {
			client.close();
		} catch (Exception e2) {
		}
		if (client != null) {
			server.clientDisconnect(this);
			stop();
		}
	}

}
