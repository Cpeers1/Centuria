package org.asf.centuria;

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
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.UUID;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;

import org.asf.connective.https.ConnectiveHTTPSServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.asf.centuria.entities.components.ComponentManager;
import org.asf.centuria.entities.inventoryitems.InventoryItemManager;
import org.asf.centuria.entities.players.Player;
import org.asf.centuria.modules.ICenturiaModule;
import org.asf.centuria.modules.ModuleManager;
import org.asf.centuria.modules.eventbus.EventBus;
import org.asf.centuria.modules.events.accounts.AccountDisconnectEvent;
import org.asf.centuria.modules.events.accounts.AccountDisconnectEvent.DisconnectType;
import org.asf.centuria.modules.events.servers.APIServerStartupEvent;
import org.asf.centuria.modules.events.servers.DirectorServerStartupEvent;
import org.asf.centuria.modules.events.updates.ServerUpdateCompletionEvent;
import org.asf.centuria.modules.events.updates.ServerUpdateEvent;
import org.asf.centuria.modules.events.updates.UpdateCancelEvent;
import org.asf.centuria.networking.chatserver.ChatClient;
import org.asf.centuria.networking.chatserver.ChatServer;
import org.asf.centuria.networking.gameserver.GameServer;
import org.asf.centuria.networking.http.api.FallbackAPIProcessor;
import org.asf.centuria.networking.http.api.GameRegistrationHandler;
import org.asf.centuria.networking.http.api.AuthenticateHandler;
import org.asf.centuria.networking.http.api.DisplayNameValidationHandler;
import org.asf.centuria.networking.http.api.DisplayNamesRequestHandler;
import org.asf.centuria.networking.http.api.RequestTokenHandler;
import org.asf.centuria.networking.http.api.SeasonPassRequestHandler;
import org.asf.centuria.networking.http.api.UpdateDisplayNameHandler;
import org.asf.centuria.networking.http.api.UserHandler;
import org.asf.centuria.networking.http.api.XPDetailsHandler;
import org.asf.centuria.networking.http.api.custom.ChangeDisplayNameHandler;
import org.asf.centuria.networking.http.api.custom.ChangeLoginNameHandler;
import org.asf.centuria.networking.http.api.custom.ChangePasswordHandler;
import org.asf.centuria.networking.http.api.custom.DeleteAccountHandler;
import org.asf.centuria.networking.http.api.custom.ListPlayersHandler;
import org.asf.centuria.networking.http.api.custom.LoginRefreshHandler;
import org.asf.centuria.networking.http.api.custom.PlayerDataDownloadHandler;
import org.asf.centuria.networking.http.api.custom.RegistrationHandler;
import org.asf.centuria.networking.http.api.custom.SaveManagerHandler;
import org.asf.centuria.networking.http.api.custom.UserDetailsHandler;
import org.asf.centuria.networking.http.director.GameServerRequestHandler;
import org.asf.centuria.seasonpasses.SeasonPassManager;
import org.asf.rats.ConnectiveHTTPServer;
import org.asf.rats.ConnectiveServerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class Centuria {
	// Update
	public static String SERVER_UPDATE_VERSION = "1.6.4.B2";
	public static String DOWNLOAD_BASE_URL = "https://aerialworks.ddns.net/extra/centuria";

	// Configuration
	public static Logger logger;
	public static boolean defaultUseManagedSaves = false;
	public static boolean allowRegistration = true;
	public static boolean defaultGiveAllAvatars = true;
	public static boolean defaultGiveAllMods = true;
	public static boolean defaultGiveAllClothes = true;
	public static boolean defaultGiveAllWings = true;
	public static boolean defaultGiveAllFurnitureItems = true;
	public static boolean defaultGiveAllSanctuaryTypes = true;
	public static boolean defaultGiveAllCurrency = true;
	public static boolean defaultGiveAllResources = true;
	public static boolean encryptChat = false;
	public static boolean encryptGame = false;
	public static boolean debugMode = false;
	public static String discoveryAddress = "localhost";
	public static String spawnBehaviour;

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

	// Messages
	private static String NIL_UUID = new UUID(0, 0).toString();

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
		System.out.println("                              Centuria                              ");
		System.out.println("                       Fer.al Server Emulator                       ");
		System.out.println("                                                                    ");
		System.out.println("                          Version 1.6.4.B2                          "); // not doing this
																									// dynamically as
																									// centering is a
																									// pain
		System.out.println("                                                                    ");
		System.out.println("--------------------------------------------------------------------");
		System.out.println("");

		// Setup logging
		if (System.getProperty("debugMode") != null) {
			System.setProperty("log4j2.configurationFile", Centuria.class.getResource("/log4j2-ide.xml").toString());
			debugMode = true;
		} else {
			System.setProperty("log4j2.configurationFile", Centuria.class.getResource("/log4j2.xml").toString());
		}
		logger = LogManager.getLogger("CENTURIA");

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
			if (Boolean.parseBoolean(properties.getOrDefault("runtime-auto-update", "false")) && !Centuria.debugMode) {
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
		if (!disableUpdater && !Centuria.debugMode) {
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

		ComponentManager.registerAllComponents();
		InventoryItemManager.registerAllItems();

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
						for (Player plr : Centuria.gameServer.getPlayers()) {
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
		for (Player plr : Centuria.gameServer.getPlayers()) {
			plr.client.sendPacket("%xt%ua%-1%__FORCE_RELOGIN__%");
		}

		// Inform the game server to disconnect with maintenance
		Centuria.gameServer.shutdown = true;
		Centuria.gameServer.maintenance = true;

		// Wait a bit
		int i = 0;
		while (Centuria.gameServer.getPlayers().length != 0) {
			i++;
			if (i == 30)
				break;
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
			}
		}
		for (Player plr : Centuria.gameServer.getPlayers()) {
			// Dispatch event
			EventBus.getInstance().dispatchEvent(new AccountDisconnectEvent(plr.account, "Server has been shut down.",
					DisconnectType.SERVER_SHUTDOWN));

			plr.client.disconnect();
		}

		// Wait for log off and exit
		int l = 0;
		while (Centuria.gameServer.getPlayers().length != 0) {
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
							+ "allow-registration=true\n" + "give-all-avatars=false\n" + "give-all-mods=false\n"
							+ "give-all-clothes=false\n" + "give-all-wings=false\n" + "give-all-sanctuary-types=false\n"
							+ "give-all-furniture=false\n" + "give-all-currency=false\n" + "give-all-resources=false\n"
							+ "server-spawn-behaviour=random\ndefault-save-behaviour=single\n"
							+ "discovery-server-address=localhost\n" + "encrypt-api=false\n" + "encrypt-chat=true\n"
							+ "encrypt-game=false\nencrypt-director=false\n" + "debug-mode=false\n"
							+ "\nvpn-user-whitelist=vpn-whitelist\n" + "vpn-ipv4-banlist=\n" + "vpn-ipv6-banlist=");
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
			Centuria.privateKey = fac
					.generatePrivate(new PKCS8EncodedKeySpec(pemDecode(Files.readString(privateKey.toPath()))));
			Centuria.publicKey = fac
					.generatePublic(new X509EncodedKeySpec(pemDecode(Files.readString(publicKey.toPath()))));
		} catch (InvalidKeySpecException | IOException e1) {
			throw new RuntimeException(e1);
		}

		// Load properties into memory
		allowRegistration = properties.getOrDefault("allow-registration", "true").equals("true");
		defaultGiveAllAvatars = properties.getOrDefault("give-all-avatars", "true").equals("true");
		defaultGiveAllMods = properties.getOrDefault("give-all-mods", "true").equals("true");
		defaultGiveAllClothes = properties.getOrDefault("give-all-clothes", "true").equals("true");
		defaultGiveAllWings = properties.getOrDefault("give-all-wings", "true").equals("true");
		defaultGiveAllSanctuaryTypes = properties.getOrDefault("give-all-sanctuary-types", "true").equals("true");
		defaultGiveAllFurnitureItems = properties.getOrDefault("give-all-furniture", "true").equals("true");
		defaultGiveAllResources = properties.getOrDefault("give-all-resources", "true").equals("true");
		defaultGiveAllCurrency = properties.getOrDefault("give-all-currency", "true").equals("true");
		encryptChat = properties.getOrDefault("encrypt-chat", "false").equals("true")
				&& new File("keystore.jks").exists() && new File("keystore.jks.password").exists();
		encryptGame = properties.getOrDefault("encrypt-game", "false").equals("true")
				&& new File("keystore.jks").exists() && new File("keystore.jks.password").exists();
		discoveryAddress = properties.getOrDefault("discovery-server-address", discoveryAddress);
		debugMode = properties.getOrDefault("debug-mode", "false").equals("true");
		spawnBehaviour = properties.getOrDefault("server-spawn-behaviour", "random");
		defaultUseManagedSaves = properties.getOrDefault("default-save-behaviour", "single").equals("managed");
		if (spawnBehaviour == null)
			spawnBehaviour = "random";
		if (System.getProperty("debugMode", "false").equals("true"))
			debugMode = true;

		// Create default save settings if needed
		File defaultSaveSettingsFile = new File("savemanager.json");
		if (!defaultSaveSettingsFile.exists()) {
			JsonObject defaultSaveSettings = new JsonObject();

			// Create settings and deduct from server config
			String defaultSaveName = "default";
			if (defaultGiveAllAvatars && defaultGiveAllMods && defaultGiveAllWings && defaultGiveAllSanctuaryTypes) {
				defaultSaveName = "creative";
			} else if (!defaultGiveAllAvatars && !defaultGiveAllMods && !defaultGiveAllClothes && !defaultGiveAllWings
					&& !defaultGiveAllSanctuaryTypes && !defaultGiveAllFurnitureItems && !defaultGiveAllResources
					&& !defaultGiveAllCurrency) {
				defaultSaveName = "experience";
			}

			// Create save array
			JsonObject saves = new JsonObject();
			JsonObject save = new JsonObject();
			save.addProperty("sanctuaryLimitOverride", -1);
			save.addProperty("giveAllAvatars", defaultGiveAllAvatars);
			save.addProperty("giveAllClothes", defaultGiveAllClothes);
			save.addProperty("giveAllMods", defaultGiveAllMods);
			save.addProperty("giveAllWings", defaultGiveAllWings);
			save.addProperty("giveAllFurnitureItems", defaultGiveAllFurnitureItems);
			save.addProperty("giveAllSanctuaryTypes", defaultGiveAllSanctuaryTypes);
			save.addProperty("giveAllCurrency", defaultGiveAllCurrency);
			save.addProperty("giveAllResources", defaultGiveAllResources);
			saves.add(defaultSaveName, save);

			// Set basics
			defaultSaveSettings.addProperty("defaultSaveName", defaultSaveName);
			defaultSaveSettings.addProperty("migrationSaveName", defaultSaveName);
			defaultSaveSettings.add("saves", saves);

			try {
				Files.writeString(defaultSaveSettingsFile.toPath(),
						new Gson().newBuilder().setPrettyPrinting().create().toJson(defaultSaveSettings));
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		// Load season passes
		SeasonPassManager.getCurrentPass();

		// Start the servers
		Centuria.logger.info("Starting API server on port " + Integer.parseInt(properties.get("api-port")) + "...");

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
			Centuria.logger.fatal("Unable to start on port " + Integer.parseInt(properties.get("api-port"))
					+ "! Switching to debug mode!");
			Centuria.logger.fatal("If you are not attempting to debug the server, please run as root.");

			factory = new ConnectiveServerFactory().setPort(6970).setOption(ConnectiveServerFactory.OPTION_AUTOSTART)
					.setOption(ConnectiveServerFactory.OPTION_ASSIGN_PORT);
			if (properties.getOrDefault("encrypt-api", "false").equals("true") && new File("keystore.jks").exists()
					&& new File("keystore.jks.password").exists()) {
				factory = factory.setImplementation(ConnectiveHTTPSServer.class);
			}

			apiServer = factory.build();
		}

		// Allow modules to register handlers
		EventBus.getInstance().dispatchEvent(new APIServerStartupEvent(apiServer));

		// API processors
		apiServer.registerProcessor(new UserHandler());
		apiServer.registerProcessor(new XPDetailsHandler());
		apiServer.registerProcessor(new AuthenticateHandler());
		apiServer.registerProcessor(new UpdateDisplayNameHandler());
		apiServer.registerProcessor(new DisplayNamesRequestHandler());
		apiServer.registerProcessor(new DisplayNameValidationHandler());
		apiServer.registerProcessor(new RequestTokenHandler());
		apiServer.registerProcessor(new GameRegistrationHandler());
		apiServer.registerProcessor(new SeasonPassRequestHandler());

		// Custom API
		apiServer.registerProcessor(new LoginRefreshHandler());
		apiServer.registerProcessor(new ChangePasswordHandler());
		apiServer.registerProcessor(new ChangeDisplayNameHandler());
		apiServer.registerProcessor(new ChangeLoginNameHandler());
		apiServer.registerProcessor(new DeleteAccountHandler());
		apiServer.registerProcessor(new UserDetailsHandler());
		apiServer.registerProcessor(new ListPlayersHandler());
		apiServer.registerProcessor(new RegistrationHandler());
		apiServer.registerProcessor(new PlayerDataDownloadHandler());
		apiServer.registerProcessor(new SaveManagerHandler());

		// Fallback
		apiServer.registerProcessor(new FallbackAPIProcessor());

		//
		// Debug API
		if (System.getProperty("debugAPI") != null) {
			Centuria.logger.info("Starting debug api...");
			factory = new ConnectiveServerFactory().setPort(Integer.parseInt(System.getProperty("debugAPI")))
					.setOption(ConnectiveServerFactory.OPTION_AUTOSTART)
					.setOption(ConnectiveServerFactory.OPTION_ASSIGN_PORT);
			var apiServer = factory.build();

			// Allow modules to register handlers
			EventBus.getInstance().dispatchEvent(new APIServerStartupEvent(apiServer));

			// API processors
			apiServer.registerProcessor(new UserHandler());
			apiServer.registerProcessor(new XPDetailsHandler());
			apiServer.registerProcessor(new AuthenticateHandler());
			apiServer.registerProcessor(new UpdateDisplayNameHandler());
			apiServer.registerProcessor(new DisplayNamesRequestHandler());
			apiServer.registerProcessor(new DisplayNameValidationHandler());
			apiServer.registerProcessor(new RequestTokenHandler());
			apiServer.registerProcessor(new GameRegistrationHandler());
			apiServer.registerProcessor(new SeasonPassRequestHandler());

			// Custom API
			apiServer.registerProcessor(new LoginRefreshHandler());
			apiServer.registerProcessor(new ChangePasswordHandler());
			apiServer.registerProcessor(new ChangeDisplayNameHandler());
			apiServer.registerProcessor(new ChangeLoginNameHandler());
			apiServer.registerProcessor(new DeleteAccountHandler());
			apiServer.registerProcessor(new UserDetailsHandler());
			apiServer.registerProcessor(new ListPlayersHandler());
			apiServer.registerProcessor(new RegistrationHandler());
			apiServer.registerProcessor(new PlayerDataDownloadHandler());
			apiServer.registerProcessor(new SaveManagerHandler());

			// Fallback
			apiServer.registerProcessor(new FallbackAPIProcessor());
		}

		//
		// Start director server
		Centuria.logger
				.info("Starting Director server on port " + Integer.parseInt(properties.get("director-port")) + "...");
		factory = new ConnectiveServerFactory().setPort(Integer.parseInt(properties.get("director-port")))
				.setOption(ConnectiveServerFactory.OPTION_AUTOSTART)
				.setOption(ConnectiveServerFactory.OPTION_ASSIGN_PORT);

		if (properties.getOrDefault("encrypt-director", "false").equals("true") && new File("keystore.jks").exists()
				&& new File("keystore.jks.password").exists()) {
			factory = factory.setImplementation(ConnectiveHTTPSServer.class);
		}
		directorServer = factory.build();
		directorServer.registerProcessor(new GameServerRequestHandler());
		EventBus.getInstance().dispatchEvent(new DirectorServerStartupEvent(directorServer));

		//
		// Load game server
		ServerSocket sock;
		Centuria.logger.info("Starting Game server on port " + Integer.parseInt(properties.get("game-port")) + "...");
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
		for (ICenturiaModule module : ModuleManager.getInstance().getAllModules()) {
			gameServer = module.replaceGameServer(sock);
			if (gameServer != null)
				break;
		}
		if (gameServer == null)
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
		Centuria.logger.info("Starting Chat server on port " + Integer.parseInt(properties.get("chat-port")) + "...");
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
		for (ICenturiaModule module : ModuleManager.getInstance().getAllModules()) {
			chatServer = module.replaceChatServer(sock);
			if (chatServer != null)
				break;
		}
		if (chatServer == null)
			chatServer = new ChatServer(sock);
		chatServer.start();

		// Post-initialize modules
		Centuria.logger.info("Post-initializing Centuria modules...");
		for (ICenturiaModule module : ModuleManager.getInstance().getAllModules()) {
			Centuria.logger.info("Post-initializing module: " + module.id());
			module.postInit();
		}

		// Log completion
		Centuria.logger.info("Successfully started emulated servers.");
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

	/**
	 * Sends a system message to a player
	 * 
	 * @param player  Player to send the message to
	 * @param message Message to send
	 */
	public static void systemMessage(Player player, String message) {
		systemMessage(player, message, false);
	}

	/**
	 * Sends a system message to a player
	 * 
	 * @param player  Player to send the message to
	 * @param message Message to send
	 * @param inDm    True for DM message, false otherwise
	 */
	public static void systemMessage(Player player, String message, boolean inDm) {
		if (player == null)
			return;
		ChatClient client = chatServer.getClient(player.account.getAccountID());
		if (client != null) {
			if (inDm && !client.isInRoom("SYSTEM")) {
				// Build response
				JsonObject res = new JsonObject();
				res.addProperty("eventId", "conversations.openPrivate");
				res.addProperty("conversationId", NIL_UUID);
				res.addProperty("success", true);
				client.sendPacket(res);
				res = new JsonObject();
				res.addProperty("conversationId", NIL_UUID);
				res.addProperty("eventId", "conversations.create");
				res.addProperty("success", true);
				client.sendPacket(res);
				client.joinRoom(NIL_UUID, true);
			}

			// Send response
			JsonObject res = new JsonObject();
			res.addProperty("conversationType", inDm ? "private" : "room");
			res.addProperty("conversationId", inDm ? NIL_UUID : "room_" + player.levelID);
			res.addProperty("message", message);
			res.addProperty("source", NIL_UUID);
			res.addProperty("sentAt", LocalDateTime.now().toString());
			res.addProperty("eventId", "chat.postMessage");
			res.addProperty("success", true);
			client.sendPacket(res);
		}
	}

	// Updater
	private static boolean shouldUpdate(String channel) {
		Centuria.logger.info("Checking for updates...");

		try {
			InputStream updateLog = new URL(Centuria.DOWNLOAD_BASE_URL + "/" + channel + "/update.info").openStream();
			String update = new String(updateLog.readAllBytes(), "UTF-8").trim();
			updateLog.close();

			if (!SERVER_UPDATE_VERSION.equals(update)) {
				// Download the update list
				Centuria.logger.info("Update available, new version: " + update);
				Centuria.logger.info("Preparing to update Centuria...");
				InputStream strm = new URL(Centuria.DOWNLOAD_BASE_URL + "/" + channel + "/" + update + "/update.list")
						.openStream();
				String fileList = new String(strm.readAllBytes(), "UTF-8").trim();
				strm.close();

				// Parse the file list (newline-separated)
				String downloadList = "";
				for (String file : fileList.split("\n")) {
					if (!file.isEmpty()) {
						downloadList += file + "=" + Centuria.DOWNLOAD_BASE_URL + "/" + channel + "/" + update + "/"
								+ file + "\n";
					}
				}

				// Save the file, copy jar and run the shutdown timer
				Files.writeString(Path.of("update.list"), downloadList);
				if (!new File("updater.jar").exists())
					Files.copy(Path.of("Centuria.jar"), Path.of("updater.jar"));

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
}
