package org.asf.emuferal;

import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JPasswordField;

import java.awt.FlowLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Component;
import javax.swing.border.BevelBorder;

import org.asf.emuferal.data.XtReader;
import org.asf.emuferal.util.TaskThread;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.JLabel;
import javax.net.ssl.SSLSocketFactory;
import javax.swing.JButton;
import javax.swing.JScrollPane;
import java.awt.event.ActionListener;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.util.Base64;
import java.util.HashMap;
import java.util.zip.GZIPInputStream;
import java.awt.event.ActionEvent;

public class TestChatClient {

	private String room = null;
	private HashMap<String, String> displayNames = new HashMap<String, String>();

	private Socket gameClient;
	private Socket chatClient;

	private JFrame frmEmuferalChatClient;
	private JTextField txtUser;
	private JTextField txtPass;
	private JTextField txtChatInput;
	private JTextPane textPane;

	private JTextField txtRoom;
	private JTextField txtDirectorServer;
	private JTextField txtAPIServer;
	private JTextField txtGamePort;
	private JTextField txtChatPort;

	private JButton btnNewButton;
	private JButton btnNewButton_1;

	private String localUUID;
	private String localToken;

	private boolean entryComplete;
	private boolean joinedRoom;
	private String levelId;
	private String pendingChatRoom;

	private TaskThread clientOutputs = new TaskThread();

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					TestChatClient window = new TestChatClient();
					window.frmEmuferalChatClient.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	public void log(String message) {
		SwingUtilities.invokeLater(() -> {
			textPane.setText(textPane.getText() + message.replace("\0", "") + System.lineSeparator());
			textPane.setCaretPosition(textPane.getDocument().getLength());
		});
	}

	/**
	 * Create the application.
	 */
	public TestChatClient() {
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frmEmuferalChatClient = new JFrame();
		frmEmuferalChatClient.setTitle("EmuFeral Chat Client");
		frmEmuferalChatClient.setResizable(false);
		frmEmuferalChatClient.setBounds(100, 100, 845, 610);
		frmEmuferalChatClient.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frmEmuferalChatClient.getContentPane().setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));

		JPanel panel = new JPanel();
		panel.setAlignmentX(Component.LEFT_ALIGNMENT);
		panel.setFont(new Font("SansSerif", Font.PLAIN, 12));
		panel.setPreferredSize(new Dimension(800, 550));
		frmEmuferalChatClient.getContentPane().add(panel);
		panel.setLayout(null);

		JPanel panel_1 = new JPanel();
		panel_1.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
		panel_1.setBounds(10, 428, 780, 112);
		panel.add(panel_1);
		panel_1.setLayout(null);

		txtUser = new JTextField();
		txtUser.setBounds(10, 28, 226, 19);
		panel_1.add(txtUser);
		txtUser.setColumns(10);

		JLabel lblNewLabel = new JLabel("Username");
		lblNewLabel.setBounds(10, 10, 226, 13);
		panel_1.add(lblNewLabel);

		JLabel lblNewLabel_1 = new JLabel("Password");
		lblNewLabel_1.setBounds(10, 57, 226, 13);
		panel_1.add(lblNewLabel_1);

		txtPass = new JPasswordField();
		txtPass.setBounds(10, 80, 226, 19);
		panel_1.add(txtPass);
		txtPass.setColumns(10);

		btnNewButton = new JButton("Connect");
		btnNewButton.setBounds(634, 79, 136, 21);
		panel_1.add(btnNewButton);

		JLabel lblDirectorIp = new JLabel("Director");
		lblDirectorIp.setBounds(246, 10, 226, 13);
		panel_1.add(lblDirectorIp);

		txtDirectorServer = new JTextField();
		txtDirectorServer.setText("http://aerialworks.ddns.net:6969/v1/bestGameServer");
		txtDirectorServer.setColumns(10);
		txtDirectorServer.setBounds(246, 28, 272, 19);
		panel_1.add(txtDirectorServer);

		txtAPIServer = new JTextField();
		txtAPIServer.setText("https://aerialworks.ddns.net:6970");
		txtAPIServer.setColumns(10);
		txtAPIServer.setBounds(246, 80, 272, 19);
		panel_1.add(txtAPIServer);

		JLabel lblApi = new JLabel("API");
		lblApi.setBounds(246, 62, 226, 13);
		panel_1.add(lblApi);

		txtGamePort = new JTextField();
		txtGamePort.setText("6968");
		txtGamePort.setBounds(528, 28, 96, 19);
		panel_1.add(txtGamePort);
		txtGamePort.setColumns(10);

		JLabel lblNewLabel_2 = new JLabel("Game port");
		lblNewLabel_2.setBounds(528, 10, 96, 13);
		panel_1.add(lblNewLabel_2);

		JLabel lblNewLabel_2_1 = new JLabel("Chat port");
		lblNewLabel_2_1.setBounds(528, 62, 96, 13);
		panel_1.add(lblNewLabel_2_1);

		txtChatPort = new JTextField();
		txtChatPort.setText("6972");
		txtChatPort.setColumns(10);
		txtChatPort.setBounds(528, 80, 96, 19);
		panel_1.add(txtChatPort);

		JPanel panel_1_1 = new JPanel();
		panel_1_1.setLayout(null);
		panel_1_1.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
		panel_1_1.setBounds(280, 10, 510, 408);
		panel.add(panel_1_1);

		txtChatInput = new JTextField();
		txtChatInput.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (room != null) {
					// Send message
					try {
						JsonObject cmd = new JsonObject();
						cmd.addProperty("cmd", "chat.postMessage");
						cmd.addProperty("conversationId", room);
						cmd.addProperty("message", txtChatInput.getText());
						sendChatPacket(cmd);
					} catch (Exception e2) {
					}
				}

				txtChatInput.setText("");
			}
		});
		txtChatInput.setBounds(10, 379, 490, 19);
		panel_1_1.add(txtChatInput);
		txtChatInput.setColumns(10);

		textPane = new JTextPane();
		textPane.setEditable(false);
		JScrollPane pane = new JScrollPane(textPane);
		pane.setBounds(10, 10, 490, 359);
		panel_1_1.add(pane);

		JPanel panel_1_1_1 = new JPanel();
		panel_1_1_1.setLayout(null);
		panel_1_1_1.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
		panel_1_1_1.setBounds(10, 10, 260, 408);
		panel.add(panel_1_1_1);

		txtRoom = new JTextField();
		txtRoom.setBounds(10, 379, 155, 19);
		panel_1_1_1.add(txtRoom);
		txtRoom.setColumns(10);

		btnNewButton_1 = new JButton("Join");
		btnNewButton_1.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// Join room
				JsonObject pk = new JsonObject();
				pk.addProperty("cmd", "conversations.addParticipant");
				pk.addProperty("conversationId", txtRoom.getText());
				pk.addProperty("participant", localUUID);
				sendChatPacket(pk);

				room = txtRoom.getText();
			}
		});
		btnNewButton_1.setEnabled(false);
		btnNewButton_1.setBounds(175, 378, 85, 21);
		panel_1_1_1.add(btnNewButton_1);

		btnNewButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Thread th = new Thread(() -> {
					if (btnNewButton.getText().equals("Connect")) {
						// Connect
						room = null;
						textPane.setText("");
						btnNewButton.setEnabled(false);
						btnNewButton_1.setEnabled(false);

						clientOutputs.stopCleanly();
						clientOutputs = new TaskThread();
						clientOutputs.start();

						btnNewButton.setText("Connecting...");
						log("[system] Connecting to API server...");

						// Build login request
						JsonObject loginReq = new JsonObject();
						loginReq.addProperty("username", txtUser.getText());
						loginReq.addProperty("password", txtPass.getText());

						// Authenticate with the API server
						String uuid = "";
						String token = "";
						try {
							String data = downloadJSON(txtAPIServer.getText() + "/a/authenticate", loginReq.toString(),
									null);
							if (data.isEmpty())
								throw new IOException("Unauthorized");
							JsonObject login = JsonParser.parseString(data).getAsJsonObject();
							uuid = login.get("uuid").getAsString();
							token = login.get("auth_token").getAsString();

							// Pull user info
							log("[system] Authenticated successfully, downloading user information...");
							data = downloadJSON(txtAPIServer.getText() + "/u/user", null, token);
							JsonObject user = JsonParser.parseString(data).getAsJsonObject();
							log("[system] Logged in as: " + user.get("display_name").getAsString());
							displayNames.put(uuid, user.get("display_name").getAsString());

							// Find game server
							log("[system] Finding servers...");
							String server = JsonParser
									.parseString(downloadJSON(txtDirectorServer.getText(), null, null))
									.getAsJsonObject().get("smartfoxServer").getAsString();
							log("[system] Selected server: " + server);

							// Connect the game server
							log("[system] Connecting to the game server...");
							gameClient = new Socket(server, Integer.valueOf(txtGamePort.getText()));
							sendPacket("<msg t='sys'><body action='verChk' r='0'><ver v='165' /></body></msg>");
							readRawPacket();
							sendPacket("<msg t='sys'><body action='rndK' r='-1'></body></msg>");
							readRawPacket();
							log("[system] Authenticating with the game server...");
							sendPacket("<msg t='sys'><body action='login' r='0'><login z='sbiLogin'><nick><![CDATA["
									+ uuid + "%0%0.19.1%9%0%Inspiron 7570 (Dell Inc.)%0]]></nick><pword><![CDATA["
									+ token + "]]></pword></login></body></msg>");

							// Parse response
							JsonObject loginData = JsonParser.parseString(readRawPacket()).getAsJsonObject();
							if (loginData.get("b").getAsJsonObject().get("o").getAsJsonObject().get("status")
									.getAsInt() != 1) {
								// Failure
								throw new IOException("Error code " + loginData.get("b").getAsJsonObject().get("o")
										.getAsJsonObject().get("status").getAsInt());
							}

							// Start game server packet handler
							Thread handler = new Thread(() -> {
								while (gameClient != null && gameClient.isConnected()) {
									try {
										String pkt = readRawPacket();
										if (pkt.startsWith("%xt%")) {
											XtReader rd = new XtReader(pkt);
											String id = rd.read();
											rd.read();

											// Handle
											switch (id) {
											case "rj": {
												rd.readBoolean();
												levelId = rd.read();
												rd.read();
												rd.read();
												rd.read();
												pendingChatRoom = rd.read();

												log("[system] Joining room: " + levelId);
												joinedRoom = true;
												break;
											}
											case "oial": {
												entryComplete = true;
												break;
											}
											}
										}
										// TODO
									} catch (IOException e1) {
										disconnect();
										break;
									}
								}
							}, "Game server packet handler");
							handler.setDaemon(true);
							handler.start();

							// Keep the connection alive
							Thread kaThread = new Thread(() -> {
								while (gameClient != null && gameClient.isConnected()) {
									try {
										sendPacket("%xt%o%ka%-1%");
									} catch (IOException e2) {
										disconnect();
										break;
									}

									try {
										Thread.sleep(10000);
									} catch (InterruptedException e2) {
									}
								}
							}, "Keep-alive");
							kaThread.setDaemon(true);
							kaThread.start();

							// Save UUID and log success
							localUUID = uuid;
							localToken = token;
							log("[system] Login successful, requesting world entry...");

							// World entry
							sendPacket("%xt%o%rj%-1%820%0%");

							// Wait for success
							while (!joinedRoom) {
								Thread.sleep(100);
							}

							// Establish chat server connection
							log("[system] Connecting to the chat server...");
							chatClient = SSLSocketFactory.getDefault().createSocket(server,
									Integer.parseInt(txtChatPort.getText()));

							// Authenticate with chat server
							log("[system] Authenticating with the chat server...");
							JsonObject pk = new JsonObject();
							pk.addProperty("cmd", "sessions.start");
							pk.addProperty("uuid", uuid);
							pk.addProperty("auth_token", token);
							sendChatPacket(pk);

							// Get response
							JsonObject chR = readChatPacket();
							if (!chR.get("eventId").getAsString().equals("sessions.start")) {
								throw new IllegalArgumentException("Unexpected chat packet: " + chR);
							}

							// Verify response
							if (!chR.has("success") || !chR.get("success").getAsBoolean())
								throw new IOException("Unauthorized");

							// Start keep alive thread
							kaThread = new Thread(() -> {
								while (chatClient != null && chatClient.isConnected()) {
									JsonObject res = new JsonObject();
									res.addProperty("eventId", "ping");
									res.addProperty("success", true);
									sendChatPacket(res);

									try {
										Thread.sleep(10000);
									} catch (InterruptedException e2) {
									}
								}
							}, "Chat keep-alive");
							kaThread.setDaemon(true);
							kaThread.start();

							// Start chat packet handler thread
							kaThread = new Thread(() -> {
								while (chatClient != null && chatClient.isConnected()) {
									try {
										JsonObject packet = readChatPacket();
										if (packet.get("eventId").getAsString().equals("chat.postMessage")) {
											// Received a chat message, verify it
											if (room != null
													&& packet.get("conversationId").getAsString().equals(room)) {
												// Show message
												log(getDisplayName(packet.get("source").getAsString()) + ": "
														+ packet.get("message").getAsString());
											}
										}
									} catch (IOException e2) {
										disconnect();
										break;
									}
								}
							}, "Chat packet handler");
							kaThread.setDaemon(true);
							kaThread.start();

							// Log success
							log("[system] Chat server connection established.");

							// Join chat room
							if (pendingChatRoom != null) {
								// Set room
								txtRoom.setText(pendingChatRoom);

								// Join room
								pk = new JsonObject();
								pk.addProperty("cmd", "conversations.addParticipant");
								pk.addProperty("conversationId", txtRoom.getText());
								pk.addProperty("participant", localUUID);
								sendChatPacket(pk);

								room = txtRoom.getText();
							}

							// Full world entry
							log("[system] Entering world...");
							sendPacket("%xt%o%wr%-1%3b8493d7-5077-4e90-880c-ed2974513a2f%");

							// Wait for entry
							while (!entryComplete)
								Thread.sleep(100);

							// Log completion
							log("[system] World join complete");

							btnNewButton.setEnabled(true);
							btnNewButton_1.setEnabled(true);
							btnNewButton.setText("Disconnect");
						} catch (Exception e1) {
							// Error
							log("[system] Connection failure: " + e1.getClass().getSimpleName()
									+ (e1.getMessage() != null ? ": " + e1.getMessage() : ""));
							btnNewButton.setText("Connect");
							btnNewButton.setEnabled(true);
							btnNewButton_1.setEnabled(false);

							// Cleanup
							try {
								if (gameClient != null)
									gameClient.close();
								if (chatClient != null)
									chatClient.close();
							} catch (IOException e2) {
							}
							gameClient = null;
							chatClient = null;
							room = null;

							return;
						}

					} else {
						disconnect();
					}
				});
				th.setDaemon(true);
				th.start();
			}
		});
		JLabel lblNewLabel_3 = new JLabel("Rooms");
		lblNewLabel_3.setBounds(10, 264, 240, 13);
		panel_1_1_1.add(lblNewLabel_3);

		JLabel lblNewLabel_3_1 = new JLabel("Players");
		lblNewLabel_3_1.setBounds(10, 10, 240, 13);
		panel_1_1_1.add(lblNewLabel_3_1);
	}

	private String getDisplayName(String ID) {
		// Find cached name
		if (displayNames.containsKey(ID))
			return displayNames.get(ID);

		// Contact the API
		JsonObject req = new JsonObject();
		JsonArray uuids = new JsonArray();
		uuids.add(ID);
		req.add("uuids", uuids);
		try {
			JsonArray foundNames = JsonParser
					.parseString(downloadJSON(txtAPIServer.getText() + "/i/display_names", req.toString(), localToken))
					.getAsJsonObject().get("found").getAsJsonArray();
			if (foundNames.size() == 1) {
				String name = foundNames.get(0).getAsJsonObject().get("display_name").getAsString();
				displayNames.put(ID, name);
				return name;
			}
		} catch (JsonSyntaxException | IOException e) {
		}

		// Unknown
		return "unknown";
	}

	private void disconnect() {
		if (gameClient == null)
			return;

		// Disconnect
		clientOutputs.stopCleanly();

		try {
			if (gameClient != null)
				gameClient.close();
			if (chatClient != null)
				chatClient.close();
		} catch (IOException e1) {
		}
		gameClient = null;
		chatClient = null;
		room = null;

		log("[system] Disconnected from game and chat servers.");
		btnNewButton.setEnabled(true);
		btnNewButton_1.setEnabled(false);
		btnNewButton.setText("Connect");
	}

	private void sendChatPacket(JsonObject packet) {
		clientOutputs.schedule(() -> {
			try {
				chatClient.getOutputStream().write(packet.toString().getBytes("UTF-8"));
			} catch (IOException e) {
				disconnect();
			}
		});
	}

	private synchronized JsonObject readChatPacket() throws IOException {
		// Read packet
		String payload = new String();
		while (true) {
			int b = chatClient.getInputStream().read();
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

				return JsonParser.parseString(payload).getAsJsonObject();
			} else
				payload += (char) b;
		}
	}

	private void sendPacket(String packet) throws IOException {
		clientOutputs.schedule(() -> {
			try {
				// Send packet
				byte[] payload = packet.getBytes("UTF-8");
				gameClient.getOutputStream().write(payload);
				gameClient.getOutputStream().write(0);
				gameClient.getOutputStream().flush();
			} catch (IOException e) {
			}
		});
	}

	private String readRawPacket() throws IOException {
		// Read packet
		String payload = new String();
		while (true) {
			int b = gameClient.getInputStream().read();
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

	private String downloadJSON(String url, String body, String token) throws IOException {
		HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
		if (token != null)
			conn.addRequestProperty("Authorization", "Bearer " + token);
		if (body != null) {
			conn.setDoOutput(true);
			conn.getOutputStream().write(body.getBytes("UTF-8"));
		}
		byte[] data = conn.getInputStream().readAllBytes();
		conn.getInputStream().close();
		return new String(data, "UTF-8");
	}
}
