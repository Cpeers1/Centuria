package org.asf.emuferal.networking.chatserver;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.zip.GZIPInputStream;

import org.asf.emuferal.networking.smartfox.BaseSmartfoxServer;
import org.asf.emuferal.networking.smartfox.SmartfoxClient;

public class ChatServer {

	private ServerSocket server;

	public ChatServer(ServerSocket socket) {
		server = socket;
	}

	/**
	 * Runs the server
	 */
	public void start() {
		Thread serverProcessor = new Thread(() -> {
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
		}, "Chat Server Thread: " + this.getClass().getSimpleName());
		serverProcessor.setDaemon(true);
		serverProcessor.start();
	}

	// Client system
	private void runClient(Socket client) throws IOException {
		// Start the client thread
		Thread th = new Thread(() -> {
			try {
				// Run start code
				startClient(client);

				// Client loop
				while (client != null) {
					String packet = readRawPacket(client);
					packet = packet;
				}
			} catch (Exception e) {
				try {
					client.close();
				} catch (IOException e2) {
				}
			}
		}, "Chat Client Thread: " + client);
		th.setDaemon(true);
		th.start();
	}

	private String readRawPacket(Socket client) throws IOException {
		String payload = new String();
		while (true) {
			int b = client.getInputStream().read();
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

	private void startClient(Socket client) throws IOException {

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

}
