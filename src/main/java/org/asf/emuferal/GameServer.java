package org.asf.emuferal;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Base64;
import java.util.ConcurrentModificationException;
import java.util.Random;
import java.util.UUID;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.asf.emuferal.data.XtReader;
import org.asf.emuferal.data.XtWriter;
import org.asf.emuferal.packets.LoginPackets;
import org.asf.emuferal.packets.VersionHandshakePackets;
import org.asf.emuferal.players.Player;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class GameServer {

	private ServerSocket gameServer;
	private Random rnd = new Random();
	private ArrayList<Player> players = new ArrayList<Player>();

	private Player[] getPlayers() {
		while (true) {
			try {
				return players.toArray(t -> new Player[t]);
			} catch (ConcurrentModificationException e) {
			}
		}
	}

	public void run(ServerSocket server) {
		Thread serverProcessor = new Thread(() -> {
			gameServer = server;
			while (true) {
				try {
					Socket client = gameServer.accept();
					runClient(client);
				} catch (IOException ex) {
				}
			}
		}, "Game Server Thread");
		serverProcessor.setDaemon(true);
		serverProcessor.start();
	}

	public void runClient(Socket client) {
		Thread serverProcessor = new Thread(() -> {
			// XML parser
			XmlMapper mapper = new XmlMapper();

			// Player object
			Player plr = new Player();
			plr.client = client;

			try {
				// Version handshake
				boolean badClient = false;
				String versionPk = readPacket(client);
				VersionHandshakePackets.Request.msg data = mapper.readValue(versionPk,
						VersionHandshakePackets.Request.msg.class);
				// Check version
				VersionHandshakePackets.Response.msg m = new VersionHandshakePackets.Response.msg();
				m.body.action = "apiOK";
				m.body.r = "0";
				sendPacket(client, mapper.writeValueAsString(m));
				if (!data.body.action.equals("verChk") && data.body.ver.v.equals("165")) {
					badClient = true;
				}

				// Random key
				readPacket(client);
				String key = Long.toString(rnd.nextLong(), 16);
				sendPacket(client, "<msg t='sys'><body action='rndK' r='-1'><k>" + key + "</k></body></msg>");

				// Authentication
				String ap1 = readPacket(client);
				LoginPackets.Request.msg authD = mapper.readValue(ap1, LoginPackets.Request.msg.class);

				// If the client is out of date, send error
				if (badClient) {
					JsonObject response = new JsonObject();
					JsonObject b = new JsonObject();
					b.addProperty("r", authD.body.r);
					JsonObject o = new JsonObject();
					o.addProperty("statusId", -24);
					o.addProperty("_cmd", "login");
					o.addProperty("status", -24);
					b.add("o", o);
					response.add("b", b);
					response.addProperty("t", "xt");
					sendPacket(client, response.toString());
					try {
						client.close();
					} catch (IOException e2) {
					}
					return;
				}

				// Find the account
				// ----------------------------------------------------------------------------------------------------------
				String token = authD.body.login.pword;
				JsonObject payload = JsonParser
						.parseString(new String(Base64.getUrlDecoder().decode(token.split("\\.")[1]), "UTF-8"))
						.getAsJsonObject();
				File uf = new File("accounts/" + payload.get("uuid").getAsString());
				if (uf.exists()) {
					plr.userUUID = Files.readAllLines(uf.toPath()).get(0);
					plr.loginName = Files.readAllLines(uf.toPath()).get(1);
					plr.isNew = Boolean.valueOf(Files.readAllLines(uf.toPath()).get(2));
					plr.displayName = Files.readAllLines(uf.toPath()).get(3);
					plr.userID = Integer.parseInt(Files.readAllLines(uf.toPath()).get(4));
				} else {
					// Failure
					JsonObject response = new JsonObject();
					JsonObject b = new JsonObject();
					b.addProperty("r", authD.body.r);
					JsonObject o = new JsonObject();
					o.addProperty("statusId", -10);
					o.addProperty("_cmd", "login");
					o.addProperty("status", -10);
					b.add("o", o);
					response.add("b", b);
					response.addProperty("t", "xt");
					sendPacket(client, response.toString());
					try {
						client.close();
					} catch (IOException e2) {
					}
					return;
				}

				// Looks
				File lookFiles = new File("accounts/" + payload.get("uuid").getAsString() + ".looks");
				lookFiles.mkdirs();

				// Active look
				File activeLookFileC = new File("accounts/" + payload.get("uuid").getAsString() + ".looks/active.look");
				String activeLook = UUID.randomUUID().toString();
				if (activeLookFileC.exists()) {
					activeLook = Files.readAllLines(activeLookFileC.toPath()).get(0);
				} else {
					Files.writeString(activeLookFileC.toPath(), activeLook);
				}

				// Sanctuary looks
				File sLookFiles = new File("accounts/" + payload.get("uuid").getAsString() + ".sanctuary.looks");
				sLookFiles.mkdirs();

				// Active look
				File activeSLookFileC = new File(
						"accounts/" + payload.get("uuid").getAsString() + ".sanctuary.looks/active.look");
				String activeSanctuaryLook = UUID.randomUUID().toString();
				if (activeSLookFileC.exists()) {
					activeSanctuaryLook = Files.readAllLines(activeSLookFileC.toPath()).get(0);
				} else {
					Files.writeString(activeSLookFileC.toPath(), activeSanctuaryLook);
				}
				// ----------------------------------------------------------------------------------------------------------

				// Send response
				JsonObject response = new JsonObject();
				JsonObject b = new JsonObject();
				b.addProperty("r", authD.body.r);
				JsonObject o = new JsonObject();
				o.addProperty("statusId", 1);
				o.addProperty("_cmd", "login");
				// Params
				JsonObject params = new JsonObject();
				params.addProperty("jamaaTime", System.currentTimeMillis() / 1000);
				params.addProperty("pendingFlags", (plr.isNew
						|| !new File("accounts/" + payload.get("uuid").getAsString() + ".looks/" + activeLook + ".look")
								.exists() ? 2 : 3));
				params.addProperty("activeLookId", activeLook);
				params.addProperty("sanctuaryLookId", activeSanctuaryLook);
				params.addProperty("sessionId", plr.userID);
				params.addProperty("userId", plr.userID);
				params.addProperty("avatarInvId", 0);
				plr.activeLook = activeLook;
				plr.activeSanctuaryLook = activeSanctuaryLook;
				o.add("params", params);
				// -----
				o.addProperty("status", 1);
				b.add("o", o);
				response.add("b", b);
				response.addProperty("t", "xt");
				sendPacket(client, response.toString());

				// Initial login
				System.out.println("Player connected: " + plr.loginName + " (as " + plr.displayName + ")");
				sendPacket(client, "%xt%ulc%-1%");
				String playerMsg = "%xt%rfl%-1%true%";
				for (Player player : getPlayers()) {
					playerMsg += player.userUUID + "%-1%";
				}
				sendPacket(client, playerMsg); // TODO: verify this!

				// Add player
				players.add(plr);

				// Gameplay
				boolean disconnect = false;
				while (!disconnect) {
					String packet = readPacket(client);
					handlePacket(client, packet, plr);
				}
				System.out.println("Player disconnected: " + plr.loginName + " (was " + plr.displayName + ")");
				players.remove(plr);
			} catch (IOException ex) {
				try {
					client.close();
				} catch (IOException e2) {
				}
				if (plr.loginName != null) {
					System.out.println("Player disconnected: " + plr.loginName + " (was " + plr.displayName + ")");
					players.remove(plr);
				}
			}
		}, "Game Server Client: " + client);
		serverProcessor.setDaemon(true);
		serverProcessor.start();
	}

	private void handlePacket(Socket client, String packet, Player plr) throws IOException {
		if (packet.startsWith("%xt|n%"))
			packet = "%xt%" + packet.substring("%xt|n%".length());
		if (packet.startsWith("%xt%")) {
			XtReader rd = new XtReader(packet);
			XtWriter response = new XtWriter();
			String type = rd.read();

			switch (type) {
			case "o": {
				// Data request?
				String request = rd.read();
				String dataPrefix = rd.read();

				switch (request) {
				case "ilt": {
					// Inventory items

					String slot = rd.read();
					response.add("il");
					response.add("-1"); // data prefix
					try {
						File itemFile = getItemFile(plr, slot);
						ByteArrayOutputStream op = new ByteArrayOutputStream();
						GZIPOutputStream gz = new GZIPOutputStream(op);
						gz.write(Files.readAllBytes(itemFile.toPath()));
						gz.close();
						op.close();

						response.add(Base64.getEncoder().encodeToString(op.toByteArray()));
					} catch (IOException e) {
						System.err.println(
								"Inventory item file could not be found: " + slot + ", responding with empty packet!");
						e = e;
					}
					response.add(""); // empty suffix
					break;
				}
				case "rj": {
					// Room Join

					String id = rd.read();
					response.add("rj");
					response.add(Integer.toString(plr.userID));
					response.add("true");
					response.add(id);
					response.add("0");
					response.add(Integer.toString(plr.userID));
					response.add("");
					response.add("room_" + plr.userUUID);

					response.add(""); // empty suffix
					break;
				}
				case "wr": {
					// World Ready

					String uuid = rd.read();
					plr.room = uuid;

					// Send to tutorial if new
					if (plr.isNew) {
						sendPacket(client, "%xt%oial%-1%107.67%8.85%-44.85%0%0.9171%-0%0.3987%"); // Tutorial spawn
						return;
					}

					// Load character
					JsonObject avaD = JsonParser.parseString(Files.readString(
							new File("accounts/" + plr.userUUID + ".looks/" + plr.activeLook + ".look").toPath()))
							.getAsJsonObject().get("look").getAsJsonObject();

					try {
						Thread.sleep(5000); // Temporary wait
					} catch (InterruptedException e) {
					}
					handleSpawn(plr.room, plr, client, avaD);
					return;
				}
				case "orr": {
					// Respawn

					response.add("orr");
					response.add("-1");
					response.add(plr.userUUID);
					response.add(plr.respawn);
					response.add("");

					break;
				}
				case "$l": {
					// Shop List

					sendPacket(client,
							"%xt%$l%-1%30550%70%30198%31878%31876%31877%30554%30555%30556%30557%16939%18662%16938%30816%30818%30817%30819%14867%31655%25793%30766%30184%30767%27780%30769%30770%30768%30771%30772%30773%30774%30775%30776%30781%30777%30782%30778%30783%30779%30784%30780%30785%31832%31831%31992%31993%31994%32041%32034%32033%32143%32148%32144%32149%32147%32140%32145%32146%32158%32141%32151%32154%32156%32152%32157%32142%32153%32150%32155%32159%32160%32161%");
					return;
				}
				case "ka": {
					// Keep Alive
					return;
				}
				case "mm": {
					// FIXME: NEEDS TO BE IMPLEMENTED
					// Action

					String action = rd.read();
					switch (action) {
					case "loadGame": {
						action = action;
						break;
					}
					case "placeWager": {
						action = action;
						break;
					}
					default:
						action = action;
					}
				}
				case "rjt":
					// Room Join (tutorial)

					response.add("rj");
					response.add("-1");
					response.add("true");
					response.add("25280");
					response.add("4");
					response.add("35");
					response.add("");
					response.add("");

					response.add(""); // empty suffix
					break;
				case "alz": {
					// Avatar save
					if (plr.pendingLookID == null) {
						return; // It was already handled by the tutorial
					}

					String lookID = rd.read();
					String lookName = rd.read();
					rd.read();
					String avatarJson = rd.read();
					JsonObject lookData = JsonParser.parseString(avatarJson).getAsJsonObject();

					// Save look file to look database
					JsonObject look = new JsonObject();
					look.addProperty("name", lookName);
					look.add("look", lookData);
					plr.activeLook = lookID;
					File activeLookFileC = new File("accounts/" + plr.userUUID + ".looks/active.look");
					File lookFileC = new File("accounts/" + plr.userUUID + ".looks/" + plr.activeLook + ".look");
					Files.writeString(activeLookFileC.toPath(), plr.activeLook);
					Files.writeString(lookFileC.toPath(), look.toString());
					Files.writeString(new File("accounts/" + plr.userUUID).toPath(),
							plr.userUUID + "\n" + plr.loginName + "\nfalse\n" + plr.displayName + "\n" + plr.userID);

					// Update inventory item (item 200 is looks)
					File itemFile = getItemFile(plr, "200");
					JsonArray items = JsonParser.parseString(Files.readString(itemFile.toPath())).getAsJsonArray();
					boolean isPrimary = false;
					JsonObject lookObj = null;
					for (JsonElement itm : items) {
						if (itm.isJsonObject()) {
							JsonObject obj = itm.getAsJsonObject();
							if (obj.get("id").getAsString().equals(plr.activeLook)) {
								lookObj = obj;
								isPrimary = lookObj.get("components").getAsJsonObject().has("PrimaryLook");
								lookObj.remove("components");
								break;
							}
						}
					}
					if (lookObj == null) {
						lookObj = new JsonObject();
						lookObj.addProperty("defId", plr.pendingLookDefID);
						lookObj.addProperty("id", plr.activeLook);
						lookObj.addProperty("type", 200);
						items.add(lookObj);
					}
					JsonObject ts = new JsonObject();
					ts.addProperty("ts", System.currentTimeMillis());
					JsonObject nm = new JsonObject();
					nm.addProperty("name", lookName);
					JsonObject al = new JsonObject();
					al.addProperty("gender", 0);
					al.add("info", lookData);
					JsonObject components = new JsonObject();
					if (isPrimary)
						components.add("PrimaryLook", new JsonObject());
					components.add("Timestamp", ts);
					components.add("AvatarLook", al);
					components.add("Name", nm);
					lookObj.add("components", components);
					Files.writeString(itemFile.toPath(), items.toString());

					// Respond with the new inventory item
					ByteArrayOutputStream op = new ByteArrayOutputStream();
					GZIPOutputStream gz = new GZIPOutputStream(op);
					gz.write(items.toString().getBytes("UTF-8"));
					gz.close();
					op.close();

					sendPacket(client, "%xt%il%-1%" + Base64.getEncoder().encodeToString(op.toByteArray()) + "%");
					plr.pendingLookID = null;
					plr.pendingLookDefID = 8254;

					break;
				}
				case "ou": {
					// World object update (such as movement)
					// TODO

					rd = rd;

					break;
				}
				case "ors": {
					// Set respawn

					plr.respawn = rd.read() + "%" + rd.read() + "%" + rd.read() + "%" + rd.read() + "%" + rd.read()
							+ "%" + rd.read() + "%" + rd.read();

					break;
				}
				case "aa": {
					// Avatar action

					String action = rd.read();
					// TODO

					break;
				}
				case "oas": {
					// Interact

					String target = rd.read();
					// TODO

					break;
				}
				case "oac": {
					// Interact cancel

					String target = rd.read();
					// TODO

					return;
				}
				case "oaf":
					// Object action finish
					// TODO

					String ud = rd.read();
					switch (ud) {
					case "bbb68d30-90e6-4fe8-9c50-93098f0e7992":
						// Final with sparky before the editor
						// TODO
						break;
					case "b089c842-7c79-4468-a270-f8efc00d5955":
						// Editor interaction finished (maybe just sparky)
						break;
					default:
						ud = ud;
					}

					break;
				case "als": {
					// Avatar look switch

					String lookID = rd.read();
					plr.pendingLookID = lookID; // save the pending look ID

					// Respond with switch packet
					response.add("als");
					response.add("-1");
					response.add(plr.pendingLookID);
					response.add("");

					// Save active look ID
					File activeLookFileC = new File("accounts/" + plr.userUUID + ".looks/active.look");
					Files.writeString(activeLookFileC.toPath(), plr.pendingLookID);
					plr.activeLook = plr.pendingLookID;

					// Save the look file
					File itemFile = getItemFile(plr, "200");
					JsonArray items = JsonParser.parseString(Files.readString(itemFile.toPath())).getAsJsonArray();
					JsonObject lookObj = null;
					for (JsonElement itm : items) {
						if (itm.isJsonObject()) {
							JsonObject obj = itm.getAsJsonObject();
							if (obj.get("id").getAsString().equals(plr.activeLook)) {
								lookObj = obj;
								break;
							}
						}
					}

					plr.pendingLookDefID = 8254;
					if (lookObj != null) {
						plr.pendingLookDefID = lookObj.get("defId").getAsInt();
						String lookName = "";

						// Save
						File lookFileC = new File("accounts/" + plr.userUUID + ".looks/" + plr.activeLook + ".look");
						if (lookFileC.exists()) {
							// Load old name
							JsonObject lookData = JsonParser.parseString(Files.readString(lookFileC.toPath()))
									.getAsJsonObject();
							lookName = lookData.get("name").getAsString();
						}

						JsonObject look = new JsonObject();
						look.addProperty("name", lookName);
						look.add("look", lookObj);
						Files.writeString(lookFileC.toPath(), look.toString());
					}

					break;
				}
				case "utc": {
					// user tutorial complete

					// Decode look file
					String lookName = rd.read();
					if (lookName.equals("Look Name"))
						lookName = "";
					rd.read();
					String avatarJson = rd.read();
					JsonObject lookData = JsonParser.parseString(avatarJson).getAsJsonObject();

					// Save look file to look database
					JsonObject look = new JsonObject();
					look.addProperty("name", lookName);
					look.add("look", lookData);
					plr.activeLook = plr.pendingLookID;
					File activeLookFileC = new File("accounts/" + plr.userUUID + ".looks/active.look");
					File lookFileC = new File("accounts/" + plr.userUUID + ".looks/" + plr.activeLook + ".look");
					Files.writeString(activeLookFileC.toPath(), plr.activeLook);
					Files.writeString(lookFileC.toPath(), look.toString());
					Files.writeString(new File("accounts/" + plr.userUUID).toPath(),
							plr.userUUID + "\n" + plr.loginName + "\nfalse\n" + plr.displayName + "\n" + plr.userID);

					// Update inventory item (item 200 is looks)
					File itemFile = getItemFile(plr, "200");
					JsonArray items = JsonParser.parseString(Files.readString(itemFile.toPath())).getAsJsonArray();
					JsonObject lookObj = null;
					for (JsonElement itm : items) {
						if (itm.isJsonObject()) {
							JsonObject obj = itm.getAsJsonObject();
							if (obj.get("id").getAsString().equals(plr.activeLook)) {
								lookObj = obj;
								lookObj.remove("components");
								break;
							}
						}
					}
					if (lookObj == null) {
						lookObj = new JsonObject();
						lookObj.addProperty("defId", plr.pendingLookDefID);
						lookObj.addProperty("id", plr.activeLook);
						lookObj.addProperty("type", 200);
						items.add(lookObj);
					}
					JsonObject ts = new JsonObject();
					ts.addProperty("ts", System.currentTimeMillis());
					JsonObject nm = new JsonObject();
					nm.addProperty("name", lookName);
					JsonObject al = new JsonObject();
					al.addProperty("gender", 0);
					al.add("info", lookData);
					JsonObject components = new JsonObject();
					components.add("PrimaryLook", new JsonObject());
					components.add("Timestamp", ts);
					components.add("AvatarLook", al);
					components.add("Name", nm);
					lookObj.add("components", components);
					Files.writeString(itemFile.toPath(), items.toString());

					// Respond with the new inventory item
					ByteArrayOutputStream op = new ByteArrayOutputStream();
					GZIPOutputStream gz = new GZIPOutputStream(op);
					gz.write(items.toString().getBytes("UTF-8"));
					gz.close();
					op.close();

					sendPacket(client, "%xt%il%-1%" + Base64.getEncoder().encodeToString(op.toByteArray()) + "%");
					sendPacket(client, "%xt%utc%-1%true%" + plr.pendingLookID + "%%");
					plr.pendingLookID = null;
					plr.pendingLookDefID = 8254;
					plr.isNew = false;

					return;
				}
				default:
					request = request;
				}
				break;
			}
			default:
				type = type;
			}

			sendPacket(client, response.encode());
		} else {
			if (packet.startsWith("$")) {
				// Compressed packet
				byte[] compressedData = Base64.getDecoder().decode(packet.substring(1));
				GZIPInputStream dc = new GZIPInputStream(new ByteArrayInputStream(compressedData));
				byte[] newData = dc.readAllBytes();
				dc.close();

				// Handle the compressed packet
				this.handlePacket(client, new String(newData, "UTF-8"), plr);
			}
		}
	}

	private File getItemFile(Player plr, String slot) throws IOException {
		File inventory = new File("inventories/" + plr.userUUID);
		if (!inventory.exists()) {
			inventory.mkdirs();
		}

		File itemFile = new File(inventory, slot + ".json");
		if (slot.equals("200"))
			itemFile = new File(inventory, "avatars.json");
		if (!itemFile.exists()) {
			if (slot.equals("200")) {
				// Avatar slots are not stored yet so lets build the json
				JsonArray avatars = buildDefaultLooksFile(plr);

				// Check if the user played on 1.0.0.A1
				File oldAvatarFile = new File(inventory, slot + ".json");
				if (oldAvatarFile.exists()) {
					// Migrate avatars
					JsonArray old = JsonParser.parseString(Files.readString(oldAvatarFile.toPath())).getAsJsonArray();
					avatars.addAll(old);

					// Delete old file
					oldAvatarFile.delete();
				}

				// Save
				FileOutputStream op = new FileOutputStream(itemFile);
				op.write(avatars.toString().getBytes("UTF-8"));
				op.close();
			} else {
				// Save item if not stored on disk (copy from resources)
				InputStream strm = getClass().getClassLoader()
						.getResourceAsStream("defaultitems/inventory-" + slot + ".json");
				if (strm != null) {
					FileOutputStream op = new FileOutputStream(itemFile);
					strm.transferTo(op);
					op.close();
					strm.close();
				} else {
					throw new IOException("File not found");
				}
			}
		}

		return itemFile;
	}

	private void handleSpawn(String room, Player plr, Socket client, JsonObject avaD) throws IOException {
		switch (room) {
		case "3b8493d7-5077-4e90-880c-ed2974513a2f": {
			// City Fera Join Spawn
			plr.room = "cityfera";
			sendPacket(client, "%xt%oial%-1%54.57%3.82%-7.76%0%0.9979%-0%-0.0654%" + avaD + "%");
			plr.respawn = "54.57%3.82%-7.76%0%0.9979%-0%-0.0654";
			System.out
					.println("Player teleport: " + plr.displayName + ", teleport destination: City Fera: Login Spawn");
			return;
		}
		case "79d7fdcb-38bc-41ea-b422-e9517f3c946b": {
			// City Shopping Plaza // TODO
			plr.room = "cityfera";
			sendPacket(client, "%xt%oial%-1%99.99889%9.128172%-50.78214%0%0.6088401%0%-0.7932929%" + avaD + "%");
			plr.respawn = "99.99889%9.128172%-50.78214%0%0.6088401%0%-0.7932929";
			System.out.println(
					"Player teleport: " + plr.displayName + ", teleport destination: City Fera: Shopping Plaza");
			return;
		}
		case "5975db44-3bd3-481b-90a1-a12b561c5eff": {
			// City Fera: Centuria Door
			plr.room = "cityfera";
			sendPacket(client, "%xt%oial%-1%144.0079%14.16582%-43.13978%0%-0.7482728%0%0.6633912%" + avaD + "%");
			plr.respawn = "144.0079%14.16582%-43.13978%0%-0.7482728%0%0.6633912";
			System.out.println(
					"Player teleport: " + plr.displayName + ", teleport destination: City Fera: Centuria (Exit Door)");
			return;
		}
		case "8f1dc98c-2d5d-47b0-9ef7-87d7e606d37b": {
			// Centuria // TODO
			plr.room = "centuria";
			sendPacket(client,
					"%xt%oial%-1%0.03377199%-4.768372E-07%19.85855%0%0.9999619%0%-0.008726531%" + avaD + "%");
			plr.respawn = "0.03377199%-4.768372E-07%19.85855%0%0.9999619%0%-0.008726531";
			System.out.println("Player teleport: " + plr.displayName + ", teleport destination: City Fera: Centuria");
			return;
		}
		case "3814e0d6-e731-4cf8-ac7c-6a663fe24c6b": {
			// Centuria: Door // TODO
			plr.room = "centuria";
			sendPacket(client,
					"%xt%oial%-1%0.03377199%-4.768372E-07%19.85855%0%0.9999619%0%-0.008726531%" + avaD + "%");
			plr.respawn = "0.03377199%-4.768372E-07%19.85855%0%0.9999619%0%-0.008726531";
			System.out.println("Player teleport: " + plr.displayName
					+ ", teleport destination: City Fera: Centuria (from City Fera)");
			return;
		}
		case "6cf58e20-1975-453f-ba90-a0c93ad50391": {
			// Blood Tundra West Spawn // TODO
			plr.room = "bloodtundra";
			sendPacket(client, "%xt%oial%-1%397.9397%51.88305%197.1988%3.961745E-12%0.9622269%-1.400224E-11%-0.2722489%"
					+ avaD + "%");
			plr.respawn = "397.9397%51.88305%197.1988%3.961745E-12%0.9622269%-1.400224E-11%-0.2722489";
			System.out
					.println("Player teleport: " + plr.displayName + ", teleport destination: Blood Tundra: The Tree");
			return;
		}
		case "f82542e2-5c28-4ef1-a475-a486c337d5c6": {
			// Lakeroot West Spawn // TODO
			plr.room = "lakeroot";
			sendPacket(client, "%xt%oial%-1%-333.6679%43.56293%224.623%0%0.5688978%0%0.8224082%" + avaD + "%");
			plr.respawn = "-333.6679%43.56293%224.623%0%0.5688978%0%0.8224082";
			System.out.println(
					"Player teleport: " + plr.displayName + ", teleport destination: Lakeroot: Lakeroot Beach");
			break;
		}
		case "1473b384-8887-48a8-b6a1-04db3a874a0c": {
			// Shattered Bay: Back Entryway // TODO
			plr.room = "shatteredbay";
			sendPacket(client, "%xt%oial%-1%0.2999811%26.56618%-193.95%0%0%0%1%" + avaD + "%");
			plr.respawn = "0.2999811%26.56618%-193.95%0%0%0%1";
			System.out.println(
					"Player teleport: " + plr.displayName + ", teleport destination: Shattered Bay: Back Entryway");
			break;
		}
		case "8bdf6b4c-f968-435c-b71d-266ab47869d0": {
			// Mugmyre Marsh: Kobold Camp // TODO
			plr.room = "murgmyre";
			sendPacket(client, "%xt%oial%-1%-318.6938%72.18782%10.87311%-1.31041E-11%0.9773986%-6.32777E-12%-0.2114049%"
					+ avaD + "%");
			plr.respawn = "-318.6938%72.18782%10.87311%-1.31041E-11%0.9773986%-6.32777E-12%-0.2114049";
			System.out.println(
					"Player teleport: " + plr.displayName + ", teleport destination: Mugmyre Marsh: Kobold Camp");
			break;
		}
		case "33c96321-50ba-48b1-8448-2dfc0ee063b0": {
			// Sunken Thicket: Thicket Base // TODO
			plr.room = "sunkenthicket";
			sendPacket(client, "%xt%oial%-1%-39.86998%30.04944%9.340012%0%0%0%1%" + avaD + "%");
			plr.respawn = "-39.86998%30.04944%9.340012%0%0%0%1";
			System.out.println(
					"Player teleport: " + plr.displayName + ", teleport destination: Sunken Thicket: Thicket Base");
			break;
		}
		case "88111293-ddaf-4fe5-b1d3-ee54b6970153": {
			// Latchkey's Lab (Door)
			plr.room = "latchkey";
			sendPacket(client, "%xt%oial%-1%7.515773E-07%-0.4878199%8.597492%0%1%0%-4.371139E-08%" + avaD + "%");
			plr.respawn = "7.515773E-07%-0.4878199%8.597492%0%1%0%-4.371139E-08";
			System.out.println("Player teleport: " + plr.displayName + ", teleport destination: Latchkey's Lab (Door)");
			break;
		}
		case "c2e9f139-1829-4f23-a357-e717d55ba26b": {
			// Latchkey's Lab (Map)
			plr.room = "latchkey";
			sendPacket(client, "%xt%oial%-1%7.515773E-07%-0.4878199%8.597492%0%1%0%-4.371139E-08%" + avaD + "%");
			plr.respawn = "7.515773E-07%-0.4878199%8.597492%0%1%0%-4.371139E-08";
			System.out.println("Player teleport: " + plr.displayName + ", teleport destination: Latchkey's Lab (Map)");
			break;
		}
		case "bda8e376-3915-482a-a634-e0f1fcc386c8": {
			// Sunken Thicket: Latchkey's Lab (Exit Door)
			plr.room = "sunkenthicket";
			sendPacket(client, "%xt%oial%-1%-89.39825%33.78165%32.70113%0%0.7639955%0%0.6452216%" + avaD + "%");
			plr.respawn = "-89.39825%33.78165%32.70113%0%0.7639955%0%0.6452216";
			System.out.println("Player teleport: " + plr.displayName
					+ ", teleport destination: Sunken Thicket: Latchkey's Lab (Exit Door)");
			break;
		}
		default: {
			room = room;
		}
		}
	}

	private JsonArray buildDefaultLooksFile(Player plr) throws IOException {
		JsonArray items = new JsonArray();

		// Load the helper from resources
		System.out.println("Generating avatar file for " + plr.displayName);
		InputStream strm = getClass().getClassLoader().getResourceAsStream("defaultitems/avatarhelper.json");
		JsonObject helper = JsonParser.parseString(new String(strm.readAllBytes(), "UTF-8")).getAsJsonObject()
				.get("Avatars").getAsJsonObject();
		ArrayList<String> ids = new ArrayList<String>();
		for (String avatarSpecies : helper.keySet()) {
			JsonObject speciesData = helper.get(avatarSpecies).getAsJsonObject();
			System.out.println("Generating avatar species object " + avatarSpecies + " for " + plr.displayName + "...");

			// Build 11 look files and set the first to primary
			boolean primary = true;
			for (int i = 0; i < 13; i++) {
				// Generate look ID
				String lID = UUID.randomUUID().toString();
				while (ids.contains(lID))
					lID = UUID.randomUUID().toString();
				ids.add(lID);

				// Timestamp
				JsonObject ts = new JsonObject();
				ts.addProperty("ts", System.currentTimeMillis());

				// Name
				JsonObject nm = new JsonObject();
				nm.addProperty("name", "");

				// Avatar info
				JsonObject al = new JsonObject();
				al.addProperty("gender", 0);
				al.add("info", speciesData.get("info").getAsJsonObject());

				// Build components
				JsonObject components = new JsonObject();
				if (primary)
					components.add("PrimaryLook", new JsonObject());
				components.add("Timestamp", ts);
				components.add("AvatarLook", al);
				components.add("Name", nm);

				// Build data container
				JsonObject lookObj = new JsonObject();
				lookObj.addProperty("defId", speciesData.get("defId").getAsInt());
				lookObj.add("components", components);
				lookObj.addProperty("id", lID);
				lookObj.addProperty("type", 200);

				// Add the avatar
				items.add(lookObj);
				primary = false;
			}
		}

		return items;
	}

	private void sendPacket(Socket client, String packet) throws IOException {
		byte[] payload = packet.getBytes("UTF-8");
		client.getOutputStream().write(payload);
		client.getOutputStream().write(0);
		client.getOutputStream().flush();
	}

	private String readPacket(Socket client) throws IOException {
		String payload = new String();
		while (true) {
			int b = client.getInputStream().read();
			if (b == -1) {
				throw new IOException("Stream closed");
			} else if (b == 0) {
				return payload;
			} else
				payload += (char) b;
		}
	}

}
