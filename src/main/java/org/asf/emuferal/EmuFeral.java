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

public class EmuFeral {
	// Update
	public static final String SERVER_UPDATE_VERSION = "1.0.0.A17";
	public static final String DOWNLOAD_BASE_URL = "https://aerialworks.ddns.net/extra/emuferal";

	// Configuration
	public static boolean allowRegistration = true;
	public static boolean giveAllAvatars = true;
	public static boolean giveAllMods = true;
	public static boolean giveAllClothes = true;
	public static boolean giveAllWings = true;
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

	/**
	 * Cancels the update
	 * 
	 * @return True if successful, false otherwise
	 */
	public static boolean cancelUpdate() {
		if (updating) {
			cancelUpdate = true;
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

		// Exit
		System.exit(0);
	}

	public static void main(String[] args) throws InvocationTargetException, IOException, NoSuchAlgorithmException {
		// Update configuration
		String updateChannel = "alpha";
		boolean disableUpdater = false;
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
			disableUpdater = Boolean.parseBoolean(properties.getOrDefault("disable", "false"));

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
						System.out.println("Checking for updates...");
						try {
							InputStream updateLog = new URL(EmuFeral.DOWNLOAD_BASE_URL + "/" + channel + "/update.info")
									.openStream();
							String update = new String(updateLog.readAllBytes(), "UTF-8").trim();
							updateLog.close();

							if (!SERVER_UPDATE_VERSION.equals(update)) {
								// Download the update list
								System.out.println("Update available, new version: " + update);
								System.out.println("Preparing to update EmuFeral...");
								InputStream strm = new URL(
										EmuFeral.DOWNLOAD_BASE_URL + "/" + channel + "/" + update + "/update.list")
										.openStream();
								String fileList = new String(strm.readAllBytes(), "UTF-8").trim();
								strm.close();

								// Parse the file list (newline-separated)
								String downloadList = "";
								for (String file : fileList.split("\n")) {
									if (!file.isEmpty()) {
										downloadList += file + "=" + EmuFeral.DOWNLOAD_BASE_URL + "/" + channel + "/"
												+ update + "/" + file + "\n";
									}
								}

								// Save the file, copy jar and run the shutdown timer
								Files.writeString(Path.of("update.list"), downloadList);
								if (!new File("updater.jar").exists())
									Files.copy(Path.of("EmuFeral.jar"), Path.of("updater.jar"));
								runUpdater(mins);
								return;
							}
						} catch (IOException e) {
						}
					}
				}, "Automatic update thread");
				updater.setDaemon(true);
				updater.start();
			}
		}

		// Splash message
		System.out.println("--------------------------------------------------------------------");
		System.out.println("                                                                    ");
		System.out.println("                              EmuFeral                              ");
		System.out.println("                       Fer.al Server Emulator                       ");
		System.out.println("                                                                    ");
		System.out.println("                         Version: 1.0.0.A17                         "); // not doing this
																									// dynamically as
																									// centering is a
																									// pain
		System.out.println("                                                                    ");
		System.out.println("--------------------------------------------------------------------");
		System.out.println("");

		if (!disableUpdater
				&& (System.getProperty("debugMode") == null || System.getProperty("debugMode").equals("false"))) {
			// Check for updates
			System.out.println("Checking for updates...");
			try {
				InputStream updateLog = new URL(EmuFeral.DOWNLOAD_BASE_URL + "/" + updateChannel + "/update.info")
						.openStream();
				String update = new String(updateLog.readAllBytes(), "UTF-8").trim();
				updateLog.close();

				if (!SERVER_UPDATE_VERSION.equals(update)) {
					// Download the update list
					System.out.println("Update available, new version: " + update);
					System.out.println("Preparing to update EmuFeral...");
					InputStream strm = new URL(
							EmuFeral.DOWNLOAD_BASE_URL + "/" + updateChannel + "/" + update + "/update.list")
							.openStream();
					String fileList = new String(strm.readAllBytes(), "UTF-8").trim();
					strm.close();

					// Parse the file list (newline-separated)
					String downloadList = "";
					for (String file : fileList.split("\n")) {
						if (!file.isEmpty()) {
							downloadList += file + "=" + EmuFeral.DOWNLOAD_BASE_URL + "/" + updateChannel + "/" + update
									+ "/" + file + "\n";
						}
					}

					// Save the file, copy jar and exit
					Files.writeString(Path.of("update.list"), downloadList);
					if (!new File("updater.jar").exists())
						Files.copy(Path.of("EmuFeral.jar"), Path.of("updater.jar"));
					System.exit(0);
				}
			} catch (IOException e) {
			}
		}

		// Start the servers
		startServer();

		// Wait for exit
		directorServer.waitExit();
		apiServer.waitExit();
		chatServer.getServerSocket().close();
		gameServer.getServerSocket().close();
	}

	public static void startServer()
			throws InvocationTargetException, UnknownHostException, IOException, NoSuchAlgorithmException {
		// Server configuration
		File serverConf = new File("server.conf");
		if (!serverConf.exists()) {
			Files.writeString(serverConf.toPath(),
					"api-port=6\n" + "director-port=6969\n" + "game-port=6968\n" + "chat-port=6972\n"
							+ "allow-registration=true\n" + "give-all-avatars=true\n" + "give-all-mods=true\n"
							+ "give-all-clothes=true\n" + "give-all-wings=true\n"
							+ "discovery-server-address=localhost\n" + "encrypt-api=false\n" + "encrypt-chat=true\n"
							+ "encrypt-game=false\n\nvpn-user-whitelist=vpn-whitelist\n"
							+ "vpn-ipv4-banlist=https://raw.githubusercontent.com/ejrv/VPNs/master/vpn-ipv4.txt\n"
							+ "vpn-ipv6-banlist=https://raw.githubusercontent.com/ejrv/VPNs/master/vpn-ipv6.txt");
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

		//
		// Start director server
		System.out.println("Starting Emulated Feral Director server...");
		directorServer = new ConnectiveServerFactory().setPort(Integer.parseInt(properties.get("director-port")))
				.setOption(ConnectiveServerFactory.OPTION_AUTOSTART)
				.setOption(ConnectiveServerFactory.OPTION_ASSIGN_PORT).build();
		directorServer.registerProcessor(new GameServerRequestHandler());

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
			InputStream strm = new URL(properties.getOrDefault("vpn-ipv4-banlist",
					"https://raw.githubusercontent.com/ejrv/VPNs/master/vpn-ipv4.txt")).openStream();
			String data = new String(strm.readAllBytes(), "UTF-8").replace("\r", "");
			for (String line : data.split("\n")) {
				if (line.isBlank() || line.startsWith("#"))
					continue;
				gameServer.vpnIpsV4.add(line);
			}
			strm.close();
			strm = new URL(properties.getOrDefault("vpn-ipv6-banlist",
					"https://raw.githubusercontent.com/ejrv/VPNs/master/vpn-ipv6.txt")).openStream();
			data = new String(strm.readAllBytes(), "UTF-8").replace("\r", "");
			for (String line : data.split("\n")) {
				if (line.isBlank() || line.startsWith("#"))
					continue;
				gameServer.vpnIpsV6.add(line);
			}
			strm.close();
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
}
