package org.asf.emuferal;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyManagementException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HashMap;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;

import org.asf.connective.https.ConnectiveHTTPSServer;
import org.asf.emuferal.accounts.EmuFeralAccount;
import org.asf.emuferal.accounts.PlayerInventory;
import org.asf.emuferal.entities.components.ComponentManager;
import org.asf.emuferal.modules.IEmuFeralModule;
import org.asf.emuferal.modules.ModuleManager;
import org.asf.emuferal.modules.eventbus.EventBus;
import org.asf.emuferal.modules.events.servers.APIServerStartupEvent;
import org.asf.emuferal.modules.events.servers.DirectorServerStartupEvent;
import org.asf.emuferal.modules.events.updates.ServerUpdateCompletionEvent;
import org.asf.emuferal.modules.events.updates.ServerUpdateEvent;
import org.asf.emuferal.modules.events.updates.UpdateCancelEvent;
import org.asf.emuferal.networking.chatserver.ChatServer;
import org.asf.emuferal.networking.gameserver.GameServer;
import org.asf.emuferal.networking.http.api.FallbackAPIProcessor;
import org.asf.emuferal.networking.http.api.AuthenticateHandler;
import org.asf.emuferal.networking.http.api.DisplayNamesRequestHandler;
import org.asf.emuferal.networking.http.api.RequestTokenHandler;
import org.asf.emuferal.networking.http.api.SettingsHandler;
import org.asf.emuferal.networking.http.api.UpdateDisplayNameHandler;
import org.asf.emuferal.networking.http.api.UserHandler;
import org.asf.emuferal.networking.http.api.XPDetailsHandler;
import org.asf.emuferal.networking.http.director.GameServerRequestHandler;
import org.asf.emuferal.players.Player;
import org.asf.rats.ConnectiveHTTPServer;
import org.asf.rats.ConnectiveServerFactory;

import com.google.gson.JsonObject;

public class EmuFeral {
	// Update
	public static final String SERVER_UPDATE_VERSION = "1.0.0.B1";
	public static final String DOWNLOAD_BASE_URL = "https://aerialworks.ddns.net/extra/emuferal";

	// Configuration
	public static boolean allowRegistration = true;
	public static boolean giveAllAvatars = true;
	public static boolean giveAllMods = true;
	public static boolean giveAllClothes = true;
	public static boolean giveAllWings = true;
	public static boolean giveAllFurnitureItems = true;
	public static boolean giveAllSanctuaryTypes = true;
	public static boolean giveAllCurrency = true;
	public static boolean giveAllResources = true;
	public static boolean encryptChat = false;
	public static boolean encryptGame = false;
	public static String discoveryAddress = "localhost";

	// Servers
	private static ConnectiveHTTPServer apiServer;
	public static ConnectiveHTTPServer directorServer;
	public static GameServer gameServer;
	public static ChatServer chatServer;

	// Keys
	private static PrivateKey privateKey;
	private static PublicKey publicKey;

	// Updating
	private static boolean cancelUpdate = false;
	private static boolean updating = false;
	private static String nextVersion = null;

	/**
	 * Main method used to start the servers
	 * 
	 * @throws SecurityException
	 * @throws NoSuchMethodException
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 */
	public static void main(String[] args)
			throws InvocationTargetException, IOException, NoSuchAlgorithmException, InstantiationException,
			IllegalAccessException, IllegalArgumentException, NoSuchMethodException, SecurityException {
		// Splash message
		System.out.println("--------------------------------------------------------------------");
		System.out.println("                                                                    ");
		System.out.println("                              EmuFeral                              ");
		System.out.println("                       Fer.al Server Emulator                       ");
		System.out.println("                                                                    ");
		System.out.println("                          Version 1.0.0.B1                          "); // not doing this
																									// dynamically as
																									// centering is a
																									// pain
		System.out.println("                                                                    ");
		System.out.println("--------------------------------------------------------------------");
		System.out.println("");

		// Load modules
		ModuleManager.getInstance().initializeComponents();

		// Update configuration
		String updateChannel = "beta";
		boolean disableUpdater = true;
		if (new File("updater.conf").exists()) {
			// Parse properties
			HashMap<String, String> properties = new HashMap<String, String>();
			for (String line : Files.readAllLines(Path.of("updater.conf"))) {
				String key = line;
				String value = "";
				if (key.contains("=")) {
					value = key.substring(key.indexOf("=") + 1);
					key = key.substring(0, key.indexOf("="));
				}
				properties.put(key, value);
			}

			// Load channel
			updateChannel = properties.getOrDefault("channel", updateChannel);

			// Check if disabled
			disableUpdater = Boolean.parseBoolean(properties.getOrDefault("disable", "true"));

			// Check if automatic updating is enabled
			if (Boolean.parseBoolean(properties.getOrDefault("runtime-auto-update", "false"))) {
				int mins = Integer.parseInt(properties.getOrDefault("runtime-update-timer-length", "10"));

				// Start the automatic update thread
				final String channel = updateChannel;
				Thread updater = new Thread(() -> {
					while (true) {
						// Run every 2 minutes
						try {
							Thread.sleep(120000);
						} catch (InterruptedException e) {
						}

						// Check for updates
						if (shouldUpdate(channel)) {
							runUpdater(mins);
							return;
						}
					}
				}, "Automatic update thread");
				updater.setDaemon(true);
				updater.start();
			}
		}

		// Updater
		if (!disableUpdater
				&& (System.getProperty("debugMode") == null || System.getProperty("debugMode").equals("false"))) {
			// Check for updates
			if (shouldUpdate(updateChannel)) {
				// Dispatch event
				EventBus.getInstance().dispatchEvent(new ServerUpdateEvent(nextVersion, -1));

				// Dispatch completion event
				EventBus.getInstance().dispatchEvent(new ServerUpdateCompletionEvent(nextVersion));

				// Exit server
				System.exit(0);
			}
		}

		// Managers

		ComponentManager.RegisterAllComponents();

		// Start the servers
		startServer();

		// Wait for exit
		directorServer.waitExit();
		apiServer.waitExit();
		chatServer.getServerSocket().close();
		gameServer.getServerSocket().close();
	}

	/**
	 * Cancels the update
	 * 
	 * @return True if successful, false otherwise
	 */
	public static boolean cancelUpdate() {
		if (updating) {
			cancelUpdate = true;
			nextVersion = null;
			EventBus.getInstance().dispatchEvent(new UpdateCancelEvent());
			return true;
		} else
			return false;
	}

	/**
	 * Runs the update timer (kicks players after a specified time for server
	 * reboot)
	 * 
	 * @param mins Time given before restart in minutes
	 * @return True if successful, false otherwise
	 */
	public static boolean runUpdater(int mins) {
		// Run timer
		if (!cancelUpdate) {
			updating = true;

			EventBus.getInstance().dispatchEvent(new ServerUpdateEvent(nextVersion, mins));
			final int minutes = mins;
			Thread th = new Thread(() -> {
				int remaining = minutes;
				while (!cancelUpdate) {
					String message = null;
					switch (remaining) {
					case 60:
					case 30:
					case 15:
					case 10:
					case 5:
					case 3:
						message = "%xt%ua%-1%7391|" + remaining + "%";
						break;
					case 1:
						message = "%xt%ua%-1%7390|1%";
						break;
					case 0:
						updateShutdown();
						cancelUpdate = false;
						return;
					}

					if (message != null) {
						// Warn everyone
						for (Player plr : EmuFeral.gameServer.getPlayers()) {
							plr.client.sendPacket(message);
						}
					}

					for (int i = 0; i < 60; i++) {
						if (cancelUpdate)
							break;
						try {
							Thread.sleep(1000);
						} catch (InterruptedException e) {
						}
					}

					remaining--;
				}
				cancelUpdate = false;
				updating = false;
			});
			th.setName("Update Thread");
			th.start();

			return true;
		}

		return false;
	}

	/**
	 * Shuts down the server with a update message
	 */
	public static void updateShutdown() {
		// Dispatch event if the update was instant
		if (!updating) {
			EventBus.getInstance().dispatchEvent(new ServerUpdateEvent(nextVersion, -1));
		}

		// Disconnect everyone
		for (Player plr : EmuFeral.gameServer.getPlayers()) {
			plr.client.sendPacket("%xt%ua%-1%__FORCE_RELOGIN__%");
		}

		// Inform the game server to disconnect with maintenance
		EmuFeral.gameServer.shutdown = true;
		EmuFeral.gameServer.maintenance = true;

		// Wait a bit
		int i = 0;
		while (EmuFeral.gameServer.getPlayers().length != 0) {
			i++;
			if (i == 30)
				break;
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
			}
		}
		for (Player plr : EmuFeral.gameServer.getPlayers()) {
			plr.client.disconnect();
		}

		// Wait for log off and exit
		int l = 0;
		while (EmuFeral.gameServer.getPlayers().length != 0) {
			l++;
			if (l == 60) {
				break;
			}
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
			}
		}

		// Dispatch completion event
		EventBus.getInstance().dispatchEvent(new ServerUpdateCompletionEvent(nextVersion));

		// Exit
		System.exit(0);
	}

	public static void startServer()
			throws InvocationTargetException, UnknownHostException, IOException, NoSuchAlgorithmException {
		// Server configuration
		File serverConf = new File("server.conf");
		if (!serverConf.exists()) {
			Files.writeString(serverConf.toPath(),
					"api-port=6\n" + "director-port=6969\n" + "game-port=6968\n" + "chat-port=6972\n"
							+ "allow-registration=true\n" + "give-all-avatars=true\n" + "give-all-mods=true\n"
							+ "give-all-clothes=true\n" + "give-all-wings=true\n" + "give-all-sanctuary-types=true\n"
							+ "give-all-furniture=true\n" + "give-all-currency=true\n" + "give-all-resources=true\n"
							+ "discovery-server-address=localhost\n" + "encrypt-api=false\n" + "encrypt-chat=true\n"
							+ "encrypt-game=false\n\nvpn-user-whitelist=vpn-whitelist\n" + "vpn-ipv4-banlist=\n"
							+ "vpn-ipv6-banlist=");
		}

		// Parse properties
		HashMap<String, String> properties = new HashMap<String, String>();
		for (String line : Files.readAllLines(serverConf.toPath())) {
			String key = line;
			String value = "";
			if (key.contains("=")) {
				value = key.substring(key.indexOf("=") + 1);
				key = key.substring(0, key.indexOf("="));
			}
			properties.put(key, value);
		}

		// Load or generate keys for JWT signatures
		File publicKey = new File("publickey.pem");
		File privateKey = new File("privatekey.pem");
		if (!publicKey.exists() || !privateKey.exists()) {
			// Generate new keys
			KeyPair pair = KeyPairGenerator.getInstance("RSA").generateKeyPair();

			// Save keys
			Files.writeString(publicKey.toPath(), pemEncode(pair.getPublic().getEncoded(), "PUBLIC"));
			Files.writeString(privateKey.toPath(), pemEncode(pair.getPrivate().getEncoded(), "PRIVATE"));
		}
		// Load keys
		KeyFactory fac = KeyFactory.getInstance("RSA");
		try {
			EmuFeral.privateKey = fac
					.generatePrivate(new PKCS8EncodedKeySpec(pemDecode(Files.readString(privateKey.toPath()))));
			EmuFeral.publicKey = fac
					.generatePublic(new X509EncodedKeySpec(pemDecode(Files.readString(publicKey.toPath()))));
		} catch (InvalidKeySpecException | IOException e1) {
			throw new RuntimeException(e1);
		}

		// Load properties into memory
		allowRegistration = properties.getOrDefault("allow-registration", "true").equals("true");
		giveAllAvatars = properties.getOrDefault("give-all-avatars", "true").equals("true");
		giveAllMods = properties.getOrDefault("give-all-mods", "true").equals("true");
		giveAllClothes = properties.getOrDefault("give-all-clothes", "true").equals("true");
		giveAllWings = properties.getOrDefault("give-all-wings", "true").equals("true");
		giveAllSanctuaryTypes = properties.getOrDefault("give-all-sanctuary-types", "true").equals("true");
		giveAllFurnitureItems = properties.getOrDefault("give-all-furniture", "true").equals("true");
		giveAllResources = properties.getOrDefault("give-all-resources", "true").equals("true");
		giveAllCurrency = properties.getOrDefault("give-all-currency", "true").equals("true");
		encryptChat = properties.getOrDefault("encrypt-chat", "false").equals("true")
				&& new File("keystore.jks").exists() && new File("keystore.jks.password").exists();
		encryptGame = properties.getOrDefault("encrypt-game", "false").equals("true")
				&& new File("keystore.jks").exists() && new File("keystore.jks.password").exists();
		discoveryAddress = properties.getOrDefault("discovery-server-address", discoveryAddress);

		// Start the servers
		System.out.println("Starting Emulated Feral API server...");

		//
		// Start API server
		ConnectiveServerFactory factory;
		try {
			factory = new ConnectiveServerFactory().setPort(Integer.parseInt(properties.get("api-port")))
					.setOption(ConnectiveServerFactory.OPTION_AUTOSTART)
					.setOption(ConnectiveServerFactory.OPTION_ASSIGN_PORT);

			if (properties.getOrDefault("encrypt-api", "false").equals("true") && new File("keystore.jks").exists()
					&& new File("keystore.jks.password").exists()) {
				factory = factory.setImplementation(ConnectiveHTTPSServer.class);
			}

			apiServer = factory.build();
		} catch (Exception e) {
			System.err.println("Unable to start on port " + Integer.parseInt(properties.get("api-port"))
					+ "! Switching to debug mode!");
			System.err.println("If you are not attempting to debug the server, please run as root.");

			factory = new ConnectiveServerFactory().setPort(6970).setOption(ConnectiveServerFactory.OPTION_AUTOSTART)
					.setOption(ConnectiveServerFactory.OPTION_ASSIGN_PORT);
			if (properties.getOrDefault("encrypt-api", "false").equals("true") && new File("keystore.jks").exists()
					&& new File("keystore.jks.password").exists()) {
				factory = factory.setImplementation(ConnectiveHTTPSServer.class);
			}

			apiServer = factory.build();
		}

		// API processors
		apiServer.registerProcessor(new UserHandler());
		apiServer.registerProcessor(new XPDetailsHandler());
		apiServer.registerProcessor(new SettingsHandler());
		apiServer.registerProcessor(new AuthenticateHandler());
		apiServer.registerProcessor(new UpdateDisplayNameHandler());
		apiServer.registerProcessor(new DisplayNamesRequestHandler());
		apiServer.registerProcessor(new RequestTokenHandler());
		apiServer.registerProcessor(new FallbackAPIProcessor());

		// Allow modules to register handlers
		EventBus.getInstance().dispatchEvent(new APIServerStartupEvent(apiServer));

		//
		// Start director server
		System.out.println("Starting Emulated Feral Director server...");
		directorServer = new ConnectiveServerFactory().setPort(Integer.parseInt(properties.get("director-port")))
				.setOption(ConnectiveServerFactory.OPTION_AUTOSTART)
				.setOption(ConnectiveServerFactory.OPTION_ASSIGN_PORT).build();
		directorServer.registerProcessor(new GameServerRequestHandler());
		EventBus.getInstance().dispatchEvent(new DirectorServerStartupEvent(directorServer));

		//
		// Load game server
		ServerSocket sock;
		System.out.println("Starting Emulated Feral Game server...");
		if (encryptGame)
			try {
				sock = getContext(new File("keystore.jks"),
						Files.readString(Path.of("keystore.jks.password")).toCharArray()).getServerSocketFactory()
						.createServerSocket(Integer.parseInt(properties.get("game-port")), 0,
								InetAddress.getByName("0.0.0.0"));
			} catch (UnrecoverableKeyException | KeyManagementException | NumberFormatException | KeyStoreException
					| NoSuchAlgorithmException | CertificateException | IOException e) {
				sock = new ServerSocket(Integer.parseInt(properties.get("game-port")), 0,
						InetAddress.getByName("0.0.0.0"));
			}
		else
			sock = new ServerSocket(Integer.parseInt(properties.get("game-port")), 0, InetAddress.getByName("0.0.0.0"));
		gameServer = new GameServer(sock);

		// Server settings
		gameServer.whitelistFile = properties.get("vpn-user-whitelist");

		// Download VPN ips
		try {
			String vpnIpv4 = properties.getOrDefault("vpn-ipv4-banlist", "");
			if (!vpnIpv4.isEmpty()) {
				InputStream strm = new URL(vpnIpv4).openStream();
				String data = new String(strm.readAllBytes(), "UTF-8").replace("\r", "");
				for (String line : data.split("\n")) {
					if (line.isBlank() || line.startsWith("#"))
						continue;
					gameServer.vpnIpsV4.add(line);
				}
				strm.close();
			}
			String vpnIpv6 = properties.getOrDefault("vpn-ipv6-banlist", "");
			if (!vpnIpv6.isEmpty()) {
				InputStream strm = new URL(vpnIpv6).openStream();
				String data = new String(strm.readAllBytes(), "UTF-8").replace("\r", "");
				for (String line : data.split("\n")) {
					if (line.isBlank() || line.startsWith("#"))
						continue;
					gameServer.vpnIpsV6.add(line);
				}
				strm.close();
			}
		} catch (IOException e) {
		}

		// Start game server
		gameServer.start();

		//
		// Start chat server
		System.out.println("Starting Emulated Feral Chat server...");
		if (encryptChat)
			try {
				sock = getContext(new File("keystore.jks"),
						Files.readString(Path.of("keystore.jks.password")).toCharArray()).getServerSocketFactory()
						.createServerSocket(Integer.parseInt(properties.getOrDefault("chat-port", "6972")), 0,
								InetAddress.getByName("0.0.0.0"));
			} catch (UnrecoverableKeyException | KeyManagementException | NumberFormatException | KeyStoreException
					| NoSuchAlgorithmException | CertificateException | IOException e) {
				sock = new ServerSocket(Integer.parseInt(properties.getOrDefault("chat-port", "6972")), 0,
						InetAddress.getByName("0.0.0.0"));
			}
		else
			sock = new ServerSocket(Integer.parseInt(properties.getOrDefault("chat-port", "6972")), 0,
					InetAddress.getByName("0.0.0.0"));
		chatServer = new ChatServer(sock);
		chatServer.start();

		// Post-initialize modules
		System.out.println("Post-initializing EmuFeral modules...");
		for (IEmuFeralModule module : ModuleManager.getInstance().getAllModules()) {
			System.out.println("Post-initializing module: " + module.id());
			module.postInit();
		}

		// Log completion
		System.out.println("Successfully started emulated servers.");
	}

	private static SSLContext getContext(File keystore, char[] password)
			throws KeyStoreException, NoSuchAlgorithmException, UnrecoverableKeyException, KeyManagementException,
			CertificateException, FileNotFoundException, IOException {
		KeyStore mainStore = KeyStore.getInstance("JKS");
		mainStore.load(new FileInputStream(keystore), password);

		KeyManagerFactory managerFactory = KeyManagerFactory.getInstance("SunX509");
		managerFactory.init(mainStore, password);

		SSLContext cont = SSLContext.getInstance("TLS");
		cont.init(managerFactory.getKeyManagers(), null, null);

		return cont;
	}

	// PEM parser
	private static byte[] pemDecode(String pem) {
		String base64 = pem.replace("\r", "");

		// Strip header
		while (base64.startsWith("-"))
			base64 = base64.substring(1);
		while (!base64.startsWith("-"))
			base64 = base64.substring(1);
		while (base64.startsWith("-"))
			base64 = base64.substring(1);

		// Clean data
		base64 = base64.replace("\n", "");

		// Strip footer
		while (base64.endsWith("-"))
			base64 = base64.substring(0, base64.length() - 1);
		while (!base64.endsWith("-"))
			base64 = base64.substring(0, base64.length() - 1);
		while (base64.endsWith("-"))
			base64 = base64.substring(0, base64.length() - 1);

		// Decode and return
		return Base64.getDecoder().decode(base64);
	}

	// PEM emitter
	private static String pemEncode(byte[] key, String type) {
		// Generate header
		String PEM = "-----BEGIN " + type + " KEY-----";

		// Generate payload
		String base64 = new String(Base64.getEncoder().encode(key));

		// Generate PEM
		while (true) {
			PEM += "\n";
			boolean done = false;
			for (int i = 0; i < 64; i++) {
				if (base64.isEmpty()) {
					done = true;
					break;
				}
				PEM += base64.substring(0, 1);
				base64 = base64.substring(1);
			}
			if (base64.isEmpty())
				break;
			if (done)
				break;
		}

		// Append footer
		PEM += "\n";
		PEM += "-----END " + type + " KEY-----";

		// Return PEM data
		return PEM;
	}

	// Signature generator
	public static byte[] sign(byte[] data) {
		try {
			Signature sig = Signature.getInstance("Sha256WithRSA");
			sig.initSign(privateKey);
			sig.update(data);
			return sig.sign();
		} catch (SignatureException | NoSuchAlgorithmException | InvalidKeyException e) {
			throw new RuntimeException(e);
		}
	}

	// Signature verification
	public static boolean verify(byte[] data, byte[] signature) {
		try {
			Signature sig = Signature.getInstance("Sha256WithRSA");
			sig.initVerify(publicKey);
			sig.update(data);
			return sig.verify(signature);
		} catch (SignatureException | NoSuchAlgorithmException | InvalidKeyException e) {
			return false;
		}
	}

	// Updater
	private static boolean shouldUpdate(String channel) {
		System.out.println("Checking for updates...");

		try {
			InputStream updateLog = new URL(EmuFeral.DOWNLOAD_BASE_URL + "/" + channel + "/update.info").openStream();
			String update = new String(updateLog.readAllBytes(), "UTF-8").trim();
			updateLog.close();

			if (!SERVER_UPDATE_VERSION.equals(update)) {
				// Download the update list
				System.out.println("Update available, new version: " + update);
				System.out.println("Preparing to update EmuFeral...");
				InputStream strm = new URL(EmuFeral.DOWNLOAD_BASE_URL + "/" + channel + "/" + update + "/update.list")
						.openStream();
				String fileList = new String(strm.readAllBytes(), "UTF-8").trim();
				strm.close();

				// Parse the file list (newline-separated)
				String downloadList = "";
				for (String file : fileList.split("\n")) {
					if (!file.isEmpty()) {
						downloadList += file + "=" + EmuFeral.DOWNLOAD_BASE_URL + "/" + channel + "/" + update + "/"
								+ file + "\n";
					}
				}

				// Save the file, copy jar and run the shutdown timer
				Files.writeString(Path.of("update.list"), downloadList);
				if (!new File("updater.jar").exists())
					Files.copy(Path.of("EmuFeral.jar"), Path.of("updater.jar"));

				// Save new version in memory
				nextVersion = update;

				// Update available
				return true;
			}
		} catch (IOException e) {
		}

		// No update available
		return false;
	}

	public static void fixSanctuaries(PlayerInventory inv, EmuFeralAccount acc) {
		//TODO: This all should probably have its own, personal methods in a sanctuary-related class.

		if (!inv.containsItem("10")) {
			// Broken inventory, delete the sanctuary saves as it will break
			inv.deleteItem("5");
			inv.deleteItem("6");
			inv.deleteItem("201");
		}

		//
		// Looks
		//

		if (EmuFeral.giveAllSanctuaryTypes) {
			// Give missing sanctuary types
			int[] sanctuaryTypes = new int[] { 9588, 12632, 12637, 12964, 21273, 23627, 24122, 25414, 26065, 28431,
					9760, 9764 };
			for (int id : sanctuaryTypes)
				if (!inv.getSanctuaryAccessor().isSanctuaryUnlocked(id))
					inv.getSanctuaryAccessor().unlockSanctuary(id);
		} else {
			// Give default sanctuary if needed
			if (!inv.getSanctuaryAccessor().isSanctuaryUnlocked(9588))
				inv.getSanctuaryAccessor().unlockSanctuary(9588);
		}

		//
		// Remove broken sanctuary files
		//

		for (String id : inv.getSanctuaryAccessor().getSanctuaryLookIDs()) {
			// Get sanctuary object
			JsonObject sancD = inv.getSanctuaryAccessor().getSanctuaryLook(id).get("components").getAsJsonObject();

			// Check data
			if (!sancD.has("SanctuaryLook")) {
				// Delete the entry
				inv.getItem("201").getAsJsonArray().remove(inv.getSanctuaryAccessor().getSanctuaryLook(id));
				continue;
			}

			// Load sanc look info
			JsonObject sanc = sancD.get("SanctuaryLook").getAsJsonObject().get("info").getAsJsonObject();

			// Check validity
			String classId = sanc.get("classInvId").getAsString();
			if (inv.getSanctuaryAccessor().getSanctuaryClassObject(classId) == null) {
				// Delete the entry
				inv.getItem("201").getAsJsonArray().remove(inv.getSanctuaryAccessor().getSanctuaryLook(id));
			}
		}

		//
		// Check look count and add missing look slots
		//

		for (int i = inv.getSanctuaryAccessor().getSanctuaryLookCount(); i < 12; i++) {
			inv.getSanctuaryAccessor().addExtraSanctuarySlot();
		}

		//
		// Fix missing primary slots
		//

		for (int house : inv.getSanctuaryAccessor().getUnlockedHouseTypes()) {
			boolean found = false;

			// Check if there is any primary look saved
			for (String id : inv.getSanctuaryAccessor().getSanctuaryLookIDs()) {
				// Get sanctuary object
				JsonObject sanc = inv.getSanctuaryAccessor().getSanctuaryLook(id);

				if (sanc.get("components").getAsJsonObject().get("SanctuaryLook").getAsJsonObject().get("info")
						.getAsJsonObject().get("houseDefId").getAsInt() != house)
					continue;

				// Check if its a primary slot
				if (sanc.get("components").getAsJsonObject().has("PrimaryLook")) {
					found = true;
					break;
				}
			}

			// Save if needed
			if (!found) {
				inv.getSanctuaryAccessor().addExtraSanctuarySlot();
			}
		}

		//
		// Fix missing name info
		//

		for (String id : inv.getSanctuaryAccessor().getSanctuaryLookIDs()) {
			JsonObject look = inv.getSanctuaryAccessor().getSanctuaryLook(id);
			JsonObject components = look.get("components").getAsJsonObject();

			// Add name component if missing
			if (!components.has("Name")) {
				JsonObject nm = new JsonObject();
				nm.addProperty("name", "");
				components.add("Name", nm);

				inv.setItem("201", inv.getItem("201"));
			}
		}

		//
		// Active sanctuary
		//

		JsonObject activeSanc = inv.getSanctuaryAccessor().getSanctuaryLook(acc.getActiveSanctuaryLook());
		if (activeSanc == null) {
			// Select first primary slot
			for (String id : inv.getSanctuaryAccessor().getSanctuaryLookIDs()) {
				// Get sanctuary object
				JsonObject sanc = inv.getSanctuaryAccessor().getSanctuaryLook(id);

				// Check if its a primary slot
				if (sanc.get("components").getAsJsonObject().has("PrimaryLook")) {
					// Assign active sanctuary
					acc.setActiveSanctuaryLook(id);
					break;
				}
			}
		}

		// Save changes
		for (String change : inv.getAccessor().getItemsToSave())
			inv.setItem(change, inv.getItem(change));
		inv.getAccessor().completedSave();
	}
}
