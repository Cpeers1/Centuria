package org.asf.centuria.networking.smartfox;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Base64;
import java.util.zip.GZIPInputStream;

import org.asf.centuria.packets.smartfox.ISmartfoxPacket;

public class SocketSmartfoxClient extends SmartfoxClient {

	private Socket client;
	private BaseSmartfoxServer server;

	private Object sendLock = new Object();

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
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		while (true) {
			int b = input.read();
			if (b == -1) {
				throw new IOException("Stream closed");
			} else if (b == 0) {
				String payload = new String(buffer.toByteArray(), "UTF-8");

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
				buffer.write(b);
		}
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
