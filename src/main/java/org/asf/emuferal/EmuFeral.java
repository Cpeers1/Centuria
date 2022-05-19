package org.asf.emuferal;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;

import org.asf.rats.ConnectiveHTTPServer;
import org.asf.rats.ConnectiveServerFactory;

public class EmuFeral {
	public static final String DOWNLOAD_BASE_URL = "https://aerialworks.ddns.net/extra/emuferal";

	public static void main(String[] args) throws InvocationTargetException, IOException {
		// Update configuration
		String currentUpdate = "1.0.0.A2";
		String updateChannel = "alpha";
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
		}

		// Splash message
		System.out.println("--------------------------------------------------------------------");
		System.out.println("                                                                    ");
		System.out.println("                              EmuFeral                              ");
		System.out.println("                       Fer.al Server Emulator                       ");
		System.out.println("                                                                    ");
		System.out.println("                          Version 1.0.0.A2                          "); // not doing this
																									// dynamically as
																									// centering is a
																									// pain
		System.out.println("                                                                    ");
		System.out.println("--------------------------------------------------------------------");
		System.out.println("");

		// Check for updates
		System.out.println("Checking for updates...");
		try {
			InputStream updateLog = new URL(EmuFeral.DOWNLOAD_BASE_URL + "/" + updateChannel + "/update.info")
					.openStream();
			String update = new String(updateLog.readAllBytes(), "UTF-8").trim();
			updateLog.close();

			if (!currentUpdate.equals(update)) {
				// Download the update list
				System.out.println("Update available, new version: " + update);
				System.out.println("Preparing to update EmuFeral...");
				InputStream strm = new URL(
						EmuFeral.DOWNLOAD_BASE_URL + "/" + updateChannel + "/" + update + "/update.list").openStream();
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

		// Start the servers
		System.out.println("Starting Emulated Feral API server...");
		ConnectiveHTTPServer apiServer;
		try {
			apiServer = new ConnectiveServerFactory().setPort(6).setOption(ConnectiveServerFactory.OPTION_AUTOSTART)
					.setOption(ConnectiveServerFactory.OPTION_ASSIGN_PORT).build();
		} catch (Exception e) {
			System.err.println("Unable to start on port 6! Switching to debug mode!");
			System.err.println("If you are not attempting to debug the server, please run as root.");
			apiServer = new ConnectiveServerFactory().setPort(6970).setOption(ConnectiveServerFactory.OPTION_AUTOSTART)
					.setOption(ConnectiveServerFactory.OPTION_ASSIGN_PORT).build();
		}
		apiServer.registerProcessor(new APIProcessor());
		System.out.println("Starting Emulated Feral Director server...");
		ConnectiveHTTPServer directorServer = new ConnectiveServerFactory().setPort(6969)
				.setOption(ConnectiveServerFactory.OPTION_AUTOSTART)
				.setOption(ConnectiveServerFactory.OPTION_ASSIGN_PORT).build();
		directorServer.registerProcessor(new DirectorProcessor());
		System.out.println("Starting Emulated Feral Fake Payment server...");
		ConnectiveHTTPServer paymentServer = new ConnectiveServerFactory().setPort(6971)
				.setOption(ConnectiveServerFactory.OPTION_AUTOSTART)
				.setOption(ConnectiveServerFactory.OPTION_ASSIGN_PORT).build();
		paymentServer.registerProcessor(new PaymentsProcessor());
		System.out.println("Starting Emulated Feral Game server...");
		ServerSocket gameServer = new ServerSocket(6968, 0, InetAddress.getByName("0.0.0.0"));
		new GameServer().run(gameServer);
		System.out.println("Successfully started emulated servers.");

		// Wait for exit
		paymentServer.waitExit();
		directorServer.waitExit();
		apiServer.waitExit();
		gameServer.close();
	}

}
