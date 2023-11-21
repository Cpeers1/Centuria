package org.asf.centuria;

import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JPasswordField;

import java.awt.FlowLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Component;
import javax.swing.border.BevelBorder;
import javax.swing.event.ListDataListener;

import org.asf.centuria.data.XtReader;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.ListModel;
import javax.swing.SwingUtilities;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.net.ssl.SSLSocketFactory;
import javax.swing.JButton;
import javax.swing.JScrollPane;
import java.awt.event.ActionListener;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.zip.GZIPInputStream;
import java.awt.event.ActionEvent;

public class TestChatClient {

	private String room = null;
	private HashMap<String, String> displayNames = new HashMap<String, String>();

	private Socket gameClient;
	private Socket chatClient;

	private JFrame frmCenturiaChatClient;
	private JTextField txtUser;
	private JTextField txtPass;
	private JTextField txtChatInput;
	private JTextPane textPane;

	private JTextField txtRoom;
	private JTextField txtDirectorServer;
	private JTextField txtAPIServer;
	private JTextField txtGamePort;
	private JTextField txtChatPort;

	private JTextField txtTPLevel;

	private JButton btnNewButton;
	private JButton btnNewButton_1;
	private JButton btnTeleport;

	private String localUUID;
	private String localToken;

	private boolean entryComplete;
	private boolean joinedRoom;
	private String levelId;
	private String pendingChatRoom;

	private JList<String> playersList;
	private ArrayList<String> onlinePlayerNames = new ArrayList<String>();

	private void reloadPlayerList() {
		playersList.setModel(new ListModel<String>() {

			@Override
			public int getSize() {
				return onlinePlayerNames.size();
			}

			@Override
			public String getElementAt(int index) {
				return onlinePlayerNames.get(index);
			}

			@Override
			public void addListDataListener(ListDataListener l) {
			}

			@Override
			public void removeListDataListener(ListDataListener l) {
			}

		});
	}

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					TestChatClient window = new TestChatClient();
					window.frmCenturiaChatClient.setVisible(true);
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
		frmCenturiaChatClient = new JFrame();
		frmCenturiaChatClient.setTitle("Centuria Chat Client");
		frmCenturiaChatClient.setResizable(false);
		frmCenturiaChatClient.setBounds(100, 100, 845, 610);
		frmCenturiaChatClient.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frmCenturiaChatClient.getContentPane().setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));

		JPanel panel = new JPanel();
		panel.setAlignmentX(Component.LEFT_ALIGNMENT);
		panel.setFont(new Font("SansSerif", Font.PLAIN, 12));
		panel.setPreferredSize(new Dimension(800, 550));
		frmCenturiaChatClient.getContentPane().add(panel);
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
		txtDirectorServer.setText("http://emuferal.ddns.net:6969/v1/bestGameServer");
		txtDirectorServer.setColumns(10);
		txtDirectorServer.setBounds(246, 28, 272, 19);
		panel_1.add(txtDirectorServer);

		txtAPIServer = new JTextField();
		txtAPIServer.setText("https://emuferal.ddns.net:6970");
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
		txtRoom.setBounds(10, 311, 147, 19);
		panel_1_1_1.add(txtRoom);
		txtRoom.setColumns(10);

		btnNewButton_1 = new JButton("Join");
		btnNewButton_1.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {

				if (chatClient != null) {
					// Join room
					try {
						JsonObject pk = new JsonObject();
						pk.addProperty("cmd", "conversations.addParticipant");
						pk.addProperty("conversationId", txtRoom.getText());
						pk.addProperty("participant", localUUID);
						sendChatPacket(pk);
						room = txtRoom.getText();
					} catch (IOException e2) {
					}
				}

				room = txtRoom.getText();
			}
		});
		btnNewButton_1.setEnabled(false);
		btnNewButton_1.setBounds(165, 310, 85, 21);
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
						btnTeleport.setEnabled(false);
						entryComplete = false;
						joinedRoom = false;

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

							// Find game server
							log("[system] Finding servers...");
							String server = JsonParser
									.parseString(downloadJSON(txtDirectorServer.getText(), null, null))
									.getAsJsonObject().get("smartfoxServer").getAsString();
							log("[system] Selected server: " + server);

							// Connect the game server
							log("[system] Connecting to the game server...");
							gameClient = new Socket(server, Integer.valueOf(txtGamePort.getText()));
							messageBuffer = "";
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

											// Spawn object
											case "oi": {
												String uid = rd.read();
												String defID = rd.read();
												if (defID.equals("852")) {
													String dsp = getDisplayName(uid);
													if (!onlinePlayerNames.contains(dsp)) {
														onlinePlayerNames.add(dsp);
														SwingUtilities.invokeLater(() -> {
															reloadPlayerList();
														});
													}
													break;
												}
											}

											// Remove object
											case "od": {
												String uid = rd.read();
												String dsp = getDisplayName(uid);
												if (onlinePlayerNames.contains(dsp)) {
													onlinePlayerNames.remove(dsp);
													SwingUtilities.invokeLater(() -> {
														reloadPlayerList();
													});
												}
												break;
											}

											// Room join
											case "rj": {
												boolean success = rd.readBoolean();
												if (success) {
													// Parse
													levelId = rd.read();
													txtTPLevel.setText(levelId);
													rd.read();
													rd.read();
													rd.read();
													pendingChatRoom = rd.read();

													// Log
													log("[system] Joining room: " + levelId);

													// Reset players
													onlinePlayerNames.clear();
													SwingUtilities.invokeLater(() -> {
														reloadPlayerList();
													});

													// Check chat join
													if (chatClient != null) {
														// Switch chat
														log("[system] Switching chat room to: " + pendingChatRoom);

														// Set room
														txtRoom.setText(pendingChatRoom);

														// Join room
														JsonObject pk = new JsonObject();
														pk.addProperty("cmd", "conversations.addParticipant");
														pk.addProperty("conversationId", pendingChatRoom);
														pk.addProperty("participant", localUUID);
														sendChatPacket(pk);

														room = txtRoom.getText();
													}

													// Full world entry
													log("[system] Entering world...");
													sendPacket("%xt%o%wr%-1%3b8493d7-5077-4e90-880c-ed2974513a2f%");

													// Mark joined
													joinedRoom = true;
												} else {
													log("[system] Room join failure!");
													joinedRoom = true;
													entryComplete = true;
												}

												break;
											}

											// ObjectInfo Avatar Local
											case "oial": {
												// Log completion
												log("[system] World join complete");

												String dsp = getDisplayName(localUUID);
												if (!onlinePlayerNames.contains(dsp)) {
													onlinePlayerNames.add(dsp);
													SwingUtilities.invokeLater(() -> {
														reloadPlayerList();
													});
												}

												// Mark done
												entryComplete = true;
												break;
											}
											}
										}
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
									try {
										JsonObject res = new JsonObject();
										res.addProperty("cmd", "ping");
										res.addProperty("success", true);
										sendChatPacket(res);

										try {
											Thread.sleep(10000);
										} catch (InterruptedException e2) {
										}
									} catch (IOException e2) {
										break;
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

										// Handle packet
										switch (packet.get("eventId").getAsString()) {

										case "chat.postMessage": {
											// Received a chat message, verify it
											if (room != null
													&& packet.get("conversationId").getAsString().equals(room)) {
												// Show message
												log(getDisplayName(packet.get("source").getAsString()) + ": "
														+ packet.get("message").getAsString());
											}
											break;
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

							// Wait for entry
							while (!entryComplete)
								Thread.sleep(100);

							// Log success
							log("[system] Chat server connection established.");
							log("");

							btnNewButton.setEnabled(true);
							btnNewButton_1.setEnabled(true);
							btnTeleport.setEnabled(true);
							btnNewButton.setText("Disconnect");
						} catch (Exception e1) {
							// Error
							log("[system] Connection failure: " + e1.getClass().getSimpleName()
									+ (e1.getMessage() != null ? ": " + e1.getMessage() : ""));
							btnNewButton.setText("Connect");
							btnNewButton.setEnabled(true);
							btnNewButton_1.setEnabled(false);
							btnTeleport.setEnabled(false);

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

		lblNewLabel_4 = new JLabel("Players");
		lblNewLabel_4.setBounds(10, 12, 240, 13);
		panel_1_1_1.add(lblNewLabel_4);

		playersList = new JList<String>();
		JScrollPane pan = new JScrollPane(playersList);
		pan.setBounds(10, 31, 240, 117);
		panel_1_1_1.add(pan);

		JLabel lblNewLabel_3 = new JLabel("Rooms");
		lblNewLabel_3.setBounds(10, 158, 240, 13);
		panel_1_1_1.add(lblNewLabel_3);

		JLabel lblNewLabel_3_2 = new JLabel("Map teleporter");
		lblNewLabel_3_2.setBounds(10, 358, 240, 13);
		panel_1_1_1.add(lblNewLabel_3_2);

		txtTPLevel = new JTextField();
		txtTPLevel.setColumns(10);
		txtTPLevel.setBounds(10, 376, 147, 19);
		panel_1_1_1.add(txtTPLevel);

		btnTeleport = new JButton("Teleport");
		btnTeleport.setEnabled(false);
		btnTeleport.setBounds(165, 375, 85, 21);
		btnTeleport.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (gameClient != null) {
					Thread th = new Thread(() -> {
						try {
							// Join room
							joinedRoom = false;
							entryComplete = false;
							sendPacket("%xt%o%rj%-1%" + txtTPLevel.getText() + "%0%");

							// Wait for success
							while (!joinedRoom) {
								Thread.sleep(100);
							}

							// Wait for entry
							while (!entryComplete)
								Thread.sleep(100);
						} catch (Exception e2) {
						}
					});
					th.setDaemon(true);
					th.start();
				}
			}
		});
		panel_1_1_1.add(btnTeleport);

	}

	private String getDisplayName(String ID) {
		// Find cached name
		if (displayNames.containsKey(ID))
			return displayNames.get(ID);

		// Contact the API
		JsonObject req = new JsonObject();
		req.addProperty("id", ID);
		try {
			JsonObject res = JsonParser
					.parseString(downloadJSON(txtAPIServer.getText() + "/centuria/getuser", req.toString(), localToken))
					.getAsJsonObject();
			String name = res.get("display_name").getAsString();
			displayNames.put(ID, name);
			return name;
		} catch (JsonSyntaxException | IOException e) {
		}

		// Unknown
		return "unknown";
	}

	private void disconnect() {
		if (gameClient == null)
			return;

		try {
			if (gameClient != null)
				gameClient.close();
			if (chatClient != null)
				chatClient.close();
		} catch (IOException e1) {
		}
		displayNames.clear();
		gameClient = null;
		chatClient = null;
		room = null;

		log("[system] Disconnected from game and chat servers.");
		btnNewButton.setEnabled(true);
		btnNewButton_1.setEnabled(false);
		btnTeleport.setEnabled(false);
		btnNewButton.setText("Connect");

		this.onlinePlayerNames.clear();
		SwingUtilities.invokeLater(() -> {
			this.reloadPlayerList();
		});
	}

	private Object chatSendLock = new Object();

	private void sendChatPacket(JsonObject packet) throws IOException {
		synchronized (chatSendLock) {
			try {
				chatClient.getOutputStream().write(packet.toString().getBytes("UTF-8"));
			} catch (IOException e) {
				disconnect();
				throw e;
			}
		}
	}

	private synchronized JsonObject readChatPacket() throws IOException {
		// Read packet
		String payload = new String();
		while (true) {
			int b = chatClient.getInputStream().read();
			if (b == -1) {
				throw new IOException("Stream closed");
			} else if (b == 0) {
				return JsonParser.parseString(payload).getAsJsonObject();
			} else
				payload += (char) b;
		}
	}

	private Object gameSendLock = new Object();

	private void sendPacket(String packet) throws IOException {
		synchronized (gameSendLock) {
			try {
				// Send packet
				byte[] payload = packet.getBytes("UTF-8");
				gameClient.getOutputStream().write(payload);
				gameClient.getOutputStream().write(0);
				gameClient.getOutputStream().flush();
			} catch (IOException e) {
			}
		}
	}

	private String messageBuffer = "";
	private Object readLock = new Object();
	private JLabel lblNewLabel_4;

	private String readRawPacket() throws IOException {
		synchronized (readLock) {
			// Go over received messages
			if (messageBuffer.contains("\0")) {
				String message = messageBuffer.substring(0, messageBuffer.indexOf("\0"));
				messageBuffer = messageBuffer.substring(messageBuffer.indexOf("\0") + 1);

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

			// Read messages
			while (true) {
				// Read bytes
				byte[] buffer = new byte[20480];
				int read = gameClient.getInputStream().read(buffer);
				if (read <= -1) {
					// Go over received messages
					if (messageBuffer.contains("\0")) {
						String message = messageBuffer.substring(0, messageBuffer.indexOf("\0"));
						messageBuffer = messageBuffer.substring(messageBuffer.indexOf("\0") + 1);

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

					// Throw exception
					throw new IOException("Stream closed");
				}
				buffer = Arrays.copyOfRange(buffer, 0, read);

				// Load messages string, combining the previous buffer with the current one
				String messages = messageBuffer + new String(buffer, "UTF-8");

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

				// Push remaining bytes to the next message
				messageBuffer = messages;
			}
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
