package org.asf.emuferal.networking.chatserver;

import java.io.IOException;
import java.io.Reader;
import java.net.Socket;
import java.util.Base64;

import org.asf.emuferal.EmuFeral;
import org.asf.emuferal.accounts.AccountManager;
import org.asf.emuferal.accounts.EmuFeralAccount;
import org.asf.emuferal.util.TaskThread;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;

public class ChatClient {

	private Socket client;
	private ChatServer server;
	private JsonReader reader;
	private EmuFeralAccount player;
	private String room;

	private TaskThread taskThread;

	/**
	 * Field for storing, eg. a player instance object
	 */
	public Object container;

	public ChatClient(Socket client, ChatServer server) {
		this.client = client;
		this.server = server;

		taskThread = new TaskThread(client.toString());
		taskThread.start();

		reader = new JsonReader(new Reader() {

			@Override
			public int read(char[] cbuf, int off, int len) throws IOException {
				byte[] data = new byte[cbuf.length];
				int l = client.getInputStream().read(data, off, len);
				for (int i = 0; i < cbuf.length; i++)
					cbuf[i] = (char) data[i];
				return l;
			}

			@Override
			public void close() throws IOException {
			}

		});
	}

	void stop() {
		taskThread.stopCleanly();
	}

	// Client init
	void runClient() throws IOException {
		// Read initial packet
		JsonObject handshakeStart = readRawPacket();

		// Check validity
		if (!handshakeStart.get("cmd").getAsString().equals("sessions.start")) {
			// Invalid request, too early to send different packet
			disconnect();
			return;
		}

		// Parse payload
		String token = handshakeStart.get("auth_token").getAsString();

		// Verify signature
		String verifyD = token.split("\\.")[0] + "." + token.split("\\.")[1];
		String sig = token.split("\\.")[2];
		if (!EmuFeral.verify(verifyD.getBytes("UTF-8"), Base64.getUrlDecoder().decode(sig))) {
			disconnect();
			return;
		}

		// Parse JWT
		JsonObject payload = JsonParser
				.parseString(new String(Base64.getUrlDecoder().decode(token.split("\\.")[1]), "UTF-8"))
				.getAsJsonObject();

		// Locate account
		EmuFeralAccount acc = AccountManager.getInstance().getAccount(payload.get("uuid").getAsString());
		if (acc == null) {
			disconnect();
			return;
		}

		// Save account in memory
		player = acc;

		// Send success
		JsonObject res = new JsonObject();
		res.addProperty("cmd", "sessions.start");
		res.addProperty("success", true);
		sendPacket(res);
	}

	// Packet handling code
	void handle(JsonObject packet) {
		// TODO: packet framework
		packet = packet;
	}

	/**
	 * Retrieves the client socket
	 * 
	 * @return Socket instance
	 */
	public Socket getSocket() {
		return client;
	}

	/**
	 * Disconnects the client
	 */
	public void disconnect() {
		try {
			if (client != null)
				client.close();
		} catch (IOException e) {
		}
		stop();
		client = null;
	}

	/**
	 * Sends a packet to the client
	 * 
	 * @param packet Raw packet to send
	 * @throws IOException If transmission fails
	 */
	public void sendPacket(JsonObject packet) throws IOException {
		taskThread.schedule(() -> {
			try {
				// Send packet
				byte[] payload = packet.toString().getBytes("UTF-8");
				if (getSocket() == null)
					return;
				client.getOutputStream().write(payload);
				client.getOutputStream().flush();
			} catch (Exception e) {
			}
		});
	}

	/**
	 * Reads a single raw packet
	 * 
	 * @return JsonObject instance
	 * @throws IOException If reading fails
	 */
	public JsonObject readRawPacket() throws IOException {
		return JsonParser.parseReader(reader).getAsJsonObject();
	}

	/**
	 * Retrieves the server object
	 * 
	 * @return BaseSmartfoxServer instance
	 */
	public ChatServer getServer() {
		return server;
	}

	/**
	 * Checks if the client is still connected
	 * 
	 * @return True if connected, false otherwise
	 */
	public boolean isConnected() {
		return client != null;
	}

	/**
	 * Retrieves the connected player object
	 * 
	 * @return EmuFeralAccount instance
	 */
	public EmuFeralAccount getPlayer() {
		return player;
	}

	/**
	 * Retrieves the chat room ID
	 * 
	 * @return Chat room ID
	 */
	public String getRoom() {
		return room;
	}

}
