package org.asf.emuferal.networking.chatserver;

import java.io.IOException;
import java.lang.reflect.Array;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;

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
					byte[] data = client.getInputStream().readNBytes(client.getInputStream().available());
					while (data.length == 0)
						data = client.getInputStream().readNBytes(client.getInputStream().available());
					Files.write(Path.of("test-"+System.currentTimeMillis()+".bin"), data);
					data = data;
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
