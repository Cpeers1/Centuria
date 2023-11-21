package org.asf.centuria.networking.persistentservice;

import java.io.IOException;
import java.lang.reflect.Array;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import org.asf.centuria.Centuria;
import org.asf.centuria.networking.persistentservice.networking.AbstractPersistentServicePacket;

import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;

public abstract class BasePersistentServiceServer<T extends BasePersistentServiceClient<T, T2>, T2 extends BasePersistentServiceServer<T, T2>> {

	private ServerSocket server;
	private ArrayList<T> clients = new ArrayList<T>();
	protected ArrayList<AbstractPersistentServicePacket<T, T2>> registry = new ArrayList<AbstractPersistentServicePacket<T, T2>>();

	private Class<T> clientType;

	public BasePersistentServiceServer(ServerSocket socket, Class<T> clientType) {
		this.clientType = clientType;
		server = socket;
		registerPackets();
	}

	/**
	 * Called to register packets
	 */
	protected abstract void registerPackets();

	/**
	 * Called to create client instances
	 * 
	 * @param clientSocket Client socket instance
	 * @return Client instance
	 */
	protected abstract T createClient(Socket clientSocket);

	/**
	 * Called to log disconnects
	 * 
	 * @param client Client that was disconnected
	 */
	protected abstract void logDisconnect(T client);

	/**
	 * Retrieves all connected clients
	 * 
	 * @return Array of client instances
	 */
	@SuppressWarnings("unchecked")
	public T[] getClients() {
		synchronized (clients) {
			return clients.toArray(t -> (T[]) Array.newInstance(clientType, t));
		}
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
		}, "Persistent Service Thread: " + this.getClass().getSimpleName());
		serverProcessor.setDaemon(true);
		serverProcessor.start();
	}

	// Adds packets to the registry
	protected void registerPacket(AbstractPersistentServicePacket<T, T2> packet) {
		registry.add(packet);
	}

	// Client system
	private void runClient(Socket clientSocket) throws IOException {
		T client = createClient(clientSocket);

		// Start the client thread
		Thread th = new Thread(() -> {
			try {
				// Run start code
				client.runClient();

				// Add client
				synchronized (clients) {
					if (client.isConnected())
						clients.add(client);
				}

				// Client loop
				while (client.getSocket() != null) {
					JsonObject obj;
					try {
						obj = client.readRawPacket();
					} catch (IllegalArgumentException | JsonIOException e) {
						throw new IOException("Read failure");
					}

					try {
						client.handle(obj);
					} catch (Exception e) {
						if (client.isConnected())
							Centuria.logger.error("Error handling packet: " + obj, e);
					}
				}

				// Remove client
				synchronized (clients) {
					if (clients.contains(client)) {
						logDisconnect(client);
						clients.remove(client);
					}
				}

				// Mark disconnected
				if (client.isConnected())
					client.disconnect();
			} catch (Exception e) {
				// Close connection
				try {
					if (client.getSocket() != null)
						client.getSocket().close();
				} catch (IOException e2) {
				}

				// Remove client
				synchronized (clients) {
					if (clients.contains(client)) {
						logDisconnect(client);
						clients.remove(client);
					}
				}

				// Log disconnect
				if (!(e instanceof IOException) && !(e instanceof SocketException)) {
					Centuria.logger.error(getClass().getSimpleName() + " connection died!", e);
					e.printStackTrace();
				}

				// Mark disconnected
				if (client.isConnected())
					client.disconnect();
			}
		}, "Persistent Service Client Thread: " + client.getClass().getSimpleName() + " " + client.getSocket());
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
