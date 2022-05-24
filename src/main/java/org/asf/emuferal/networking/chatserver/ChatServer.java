package org.asf.emuferal.networking.chatserver;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class ChatServer {

	private ServerSocket server;
	private ArrayList<ChatClient> clients = new ArrayList<ChatClient>();

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
	private void runClient(Socket clientSocket) throws IOException {
		ChatClient client = new ChatClient(clientSocket, this);

		// Start the client thread
		Thread th = new Thread(() -> {
			try {
				// Run start code
				client.runClient();

				// Add client
				if (client.isConnected())
					clients.add(client);

				// Client loop
				while (client != null) {
					client.handle(client.readRawPacket());
				}

				// Remove client
				if (clients.contains(client))
					clients.remove(client);
			} catch (Exception e) {
				// Close connection
				try {
					if (client.getSocket() != null)
						client.getSocket().close();
				} catch (IOException e2) {
				}

				// Remove client
				if (clients.contains(client))
					clients.remove(client);
			}
		}, "Chat Client Thread: " + client);
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

}
