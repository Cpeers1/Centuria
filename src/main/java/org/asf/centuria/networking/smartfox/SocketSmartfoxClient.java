package org.asf.centuria.networking.smartfox;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Arrays;
import java.util.Base64;
import java.util.zip.GZIPInputStream;

import org.asf.centuria.Centuria;
import org.asf.centuria.packets.smartfox.ISmartfoxPacket;

public class SocketSmartfoxClient extends SmartfoxClient {

	private Socket client;
	private BaseSmartfoxServer server;
	private String messageBuffer = "";

	private Object sendLock = new Object();
	private Object readLock = new Object();

	InputStream input;
	OutputStream output;

	public SocketSmartfoxClient(Socket client, BaseSmartfoxServer server) {
		this.client = client;
		this.server = server;

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
	}

	@Override
	public boolean isConnected() {
		return client != null;
	}

	@Override
	public void disconnect() {
		synchronized (sendLock) {
			try {
				if (client != null)
					client.close();
			} catch (IOException e) {
			}
			server.clientDisconnect(this);
			stop();
		}
	}

	@Override
	public void sendPacket(ISmartfoxPacket packet) {
		synchronized (sendLock) {
			try {
				// Instantiate the packet and build
				String content = packet.build();

				// Send packet
				byte[] payload = content.getBytes("UTF-8");
				output.write(payload);
				output.write(0);
				output.flush();
			} catch (Exception e) {
			}
		}
	}

	@Override
	public void sendPacket(String packet) {
		if (Centuria.debugMode)
			Centuria.logger.debug("S->C: " + packet);
		synchronized (sendLock) {
			try {
				// Send packet
				byte[] payload = packet.getBytes("UTF-8");
				if (client == null)
					return;
				output.write(payload);
				output.write(0);
				output.flush();
			} catch (Exception e) {
			}
		}
	}

	@Override
	public <T extends ISmartfoxPacket> T readPacket(Class<T> packetType) throws IOException {
		return (T) server.<T>readPacket(this, packetType);
	}

	@Override
	public BaseSmartfoxServer getServer() {
		return server;
	}

	@Override
	public String readRawPacket() throws IOException {
		synchronized (readLock) {
			// Go over received messages
			String res = findFirstPacket(messageBuffer);
			if (res != null)
				return res; // Received a message

			// Read messages
			while (true) {
				// Read bytes
				byte[] buffer = new byte[20480];
				int read = input.read(buffer);
				if (read <= -1) {
					// Go over received messages
					res = findFirstPacket(messageBuffer);
					if (res != null)
						return res; // Received a message

					// Throw exception
					throw new IOException("Stream closed");
				}
				buffer = Arrays.copyOfRange(buffer, 0, read);

				// Load messages string, combining the previous buffer with the current one
				String messages = messageBuffer + new String(buffer, "UTF-8");
				res = findFirstPacket(messages);
				if (res != null)
					return res; // Received a message

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
