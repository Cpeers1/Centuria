package org.asf.emuferal;

import java.io.File;
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
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HashMap;

import org.asf.emuferal.networking.gameserver.GameServer;
import org.asf.emuferal.networking.http.APIProcessor;
import org.asf.emuferal.networking.http.DirectorProcessor;
import org.asf.emuferal.networking.http.PaymentsProcessor;
import org.asf.rats.ConnectiveHTTPServer;
import org.asf.rats.ConnectiveServerFactory;

public class EmuFeral {
	// Update
	public static final String SERVER_UPDATE_VERSION = "1.0.0.A3";
	public static final String DOWNLOAD_BASE_URL = "https://aerialworks.ddns.net/extra/emuferal";

	// Configuration
	public static boolean allowRegistration = true;
	public static String discoveryAddress = "localhost";

	// Servers
	private static ConnectiveHTTPServer apiServer;
	private static ConnectiveHTTPServer directorServer;
	private static ConnectiveHTTPServer paymentServer;
	private static GameServer gameServer;

	// Keys
	private static PrivateKey privateKey;
	private static PublicKey publicKey;

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
		}

		// Splash message
		System.out.println("--------------------------------------------------------------------");
		System.out.println("                                                                    ");
		System.out.println("                              EmuFeral                              ");
		System.out.println("                       Fer.al Server Emulator                       ");
		System.out.println("                                                                    ");
		System.out.println("                          Version 1.0.0.A3                          "); // not doing this
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
		paymentServer.waitExit();
		directorServer.waitExit();
		apiServer.waitExit();
		gameServer.getServerSocket().close();
	}

	public static void startServer()
			throws InvocationTargetException, UnknownHostException, IOException, NoSuchAlgorithmException {
		// Server configuration
		File serverConf = new File("server.conf");
		if (!serverConf.exists()) {
			Files.writeString(serverConf.toPath(), "api-port=6\n" + "director-port=6969\n" + "payments-port=6971\n"
					+ "game-port=6968\n" + "allow-registration=true\ndiscovery-server-address=localhost\n");
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

		// Start the servers
		discoveryAddress = properties.getOrDefault("discovery-server-address", discoveryAddress);
		System.out.println("Starting Emulated Feral API server...");
		try {
			apiServer = new ConnectiveServerFactory().setPort(Integer.parseInt(properties.get("api-port")))
					.setOption(ConnectiveServerFactory.OPTION_AUTOSTART)
					.setOption(ConnectiveServerFactory.OPTION_ASSIGN_PORT).build();
		} catch (Exception e) {
			System.err.println("Unable to start on port " + Integer.parseInt(properties.get("api-port"))
					+ "! Switching to debug mode!");
			System.err.println("If you are not attempting to debug the server, please run as root.");
			apiServer = new ConnectiveServerFactory().setPort(6970).setOption(ConnectiveServerFactory.OPTION_AUTOSTART)
					.setOption(ConnectiveServerFactory.OPTION_ASSIGN_PORT).build();
		}
		apiServer.registerProcessor(new APIProcessor());
		System.out.println("Starting Emulated Feral Director server...");
		ConnectiveHTTPServer directorServer = new ConnectiveServerFactory()
				.setPort(Integer.parseInt(properties.get("director-port")))
				.setOption(ConnectiveServerFactory.OPTION_AUTOSTART)
				.setOption(ConnectiveServerFactory.OPTION_ASSIGN_PORT).build();
		directorServer.registerProcessor(new DirectorProcessor());
		System.out.println("Starting Emulated Feral Fake Payment server...");
		paymentServer = new ConnectiveServerFactory().setPort(Integer.parseInt(properties.get("payments-port")))
				.setOption(ConnectiveServerFactory.OPTION_AUTOSTART)
				.setOption(ConnectiveServerFactory.OPTION_ASSIGN_PORT).build();
		paymentServer.registerProcessor(new PaymentsProcessor());
		System.out.println("Starting Emulated Feral Game server...");
		ServerSocket sock = new ServerSocket(Integer.parseInt(properties.get("game-port")), 0,
				InetAddress.getByName("0.0.0.0"));
		allowRegistration = properties.get("allow-registration").equals("true");
		gameServer = new GameServer(sock);
		gameServer.start();
		System.out.println("Successfully started emulated servers.");
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
