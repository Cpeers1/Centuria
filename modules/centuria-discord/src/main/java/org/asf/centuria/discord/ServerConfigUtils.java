package org.asf.centuria.discord;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

public class ServerConfigUtils {

	private static JsonObject config;
	static {
		// Load config path as file object
		File serversConfigFile = new File("discordservers.json");

		// Read if existing
		if (serversConfigFile.exists())
			try {
				config = JsonParser.parseString(Files.readString(serversConfigFile.toPath())).getAsJsonObject();
			} catch (JsonSyntaxException | IOException e) {
				// Create new
				config = new JsonObject();
				saveConfig();
			}
		else {
			// Create new
			config = new JsonObject();
			saveConfig();
		}
	}

	private static void saveConfig() {
		// Load config path as file object
		File serversConfigFile = new File("discordservers.json");

		// Save to disk
		try {
			Files.writeString(serversConfigFile.toPath(), config.toString());
		} catch (IOException e) {
		}
	}

	/**
	 * Retrieves or creates a server config
	 * 
	 * @param guildID Server guild ID
	 * @return JsonObject instance
	 */
	public static JsonObject getServerConfig(String guildID) {
		// Find existing
		if (config.has(guildID))
			return config.getAsJsonObject(guildID).getAsJsonObject();

		// Create new
		JsonObject server = new JsonObject();
		config.add(guildID, server);
		saveConfig();
		return server;
	}

	/**
	 * Saves a server config
	 * 
	 * @param guildID Server guild ID
	 * @param config  Server config object
	 */
	public static void saveServerConfig(String guildID, JsonObject config) {
		saveConfig();
		if (ServerConfigUtils.config.has(guildID))
			ServerConfigUtils.config.remove(guildID);
		ServerConfigUtils.config.add(guildID, config);
	}

}
