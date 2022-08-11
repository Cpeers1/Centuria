package org.asf.centuria.ipbans.impl;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.asf.centuria.ipbans.IpBanManager;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

public class FileBasedIpBanManager extends IpBanManager {

	@Override
	public synchronized void banIP(String ip) {
		// Load bans
		JsonArray bans;
		File banList = new File("bans.json");
		if (banList.exists()) {
			try {
				bans = JsonParser.parseString(Files.readString(banList.toPath())).getAsJsonArray();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		} else
			bans = new JsonArray();

		// Check existing ban
		for (JsonElement ele : bans) {
			if (ele.getAsString().equalsIgnoreCase(ip))
				return;
		}

		// Add ban
		bans.add(ip);
		try {
			Files.writeString(banList.toPath(), bans.toString());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public synchronized void unbanIP(String ip) {
		// Load bans
		JsonArray bans;
		File banList = new File("bans.json");
		if (banList.exists()) {
			try {
				bans = JsonParser.parseString(Files.readString(banList.toPath())).getAsJsonArray();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		} else
			bans = new JsonArray();

		// Check ban
		for (JsonElement ele : bans) {
			if (ele.getAsString().equalsIgnoreCase(ip)) {
				// Remove ban
				bans.remove(ele);

				// Save
				try {
					Files.writeString(banList.toPath(), bans.toString());
				} catch (IOException e) {
					throw new RuntimeException(e);
				}

				return;
			}
		}
	}

	@Override
	public boolean isIPBanned(String ip) {
		// Load bans
		JsonArray bans;
		File banList = new File("bans.json");
		if (banList.exists()) {
			try {
				bans = JsonParser.parseString(Files.readString(banList.toPath())).getAsJsonArray();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		} else
			return false;

		// Check ban
		for (JsonElement ele : bans) {
			if (ele.getAsString().equalsIgnoreCase(ip))
				return true;
		}

		// IP isnt banned
		return false;
	}

}
