package org.asf.centuria.discord;

import java.util.ArrayList;

import org.asf.centuria.accounts.CenturiaAccount;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

public class UserAllowedIpUtils {

	/**
	 * Checks if the given user has whitelisted the given IP address
	 * 
	 * @param account Account owning the IP whitelist
	 * @param ip      IP to check
	 * @return True if allowed, false otherwise
	 */
	public static boolean isAllowed(CenturiaAccount account, String ip) {
		if (account.getPlayerInventory().containsItem("allowedips")) {
			// Load item
			JsonArray arr = account.getPlayerInventory().getItem("allowedips").getAsJsonArray();

			// Loop through array, find the IP
			for (JsonElement ele : arr)
				if (ele.getAsString().equals(ip))
					return true;
		}
		return false;
	}

	/**
	 * Retrieves an array of whitelisted addresses
	 * 
	 * @param account Account owning the IP whitelist
	 * @return Array of whitelisted IPs
	 */
	public static String[] getAllowedIps(CenturiaAccount account) {
		if (account.getPlayerInventory().containsItem("allowedips")) {
			// Load item
			JsonArray arr = account.getPlayerInventory().getItem("allowedips").getAsJsonArray();

			// Build list
			ArrayList<String> ips = new ArrayList<String>();

			// Loop through array, find the IP
			for (JsonElement ele : arr)
				ips.add(ele.getAsString());

			// Return ips
			return ips.toArray(t -> new String[t]);
		}
		return new String[0];
	}

	/**
	 * Whitelists the given IP address
	 * 
	 * @param account Account owning the IP whitelist
	 * @param ip      IP to whitelist
	 */
	public static void whitelistIp(CenturiaAccount account, String ip) {
		if (!isAllowed(account, ip)) {
			// Create list if not present
			if (!account.getPlayerInventory().containsItem("allowedips"))
				account.getPlayerInventory().setItem("allowedips", new JsonArray());

			// Load list
			JsonArray arr = account.getPlayerInventory().getItem("allowedips").getAsJsonArray();

			// Add item
			arr.add(ip);

			// Save
			account.getPlayerInventory().setItem("allowedips", arr);
		}
	}

	/**
	 * Removes a whitelisted IP address
	 * 
	 * @param account Account owning the IP whitelist
	 * @param ip      IP to remove
	 */
	public static void removeFromWhitelist(CenturiaAccount account, String ip) {
		if (isAllowed(account, ip)) {
			// Load list
			JsonArray arr = account.getPlayerInventory().getItem("allowedips").getAsJsonArray();

			// Find and remove item
			for (JsonElement ele : arr)
				if (ele.getAsString().equals(ip)) {
					arr.remove(ele);
					break;
				}

			// Save
			account.getPlayerInventory().setItem("allowedips", arr);
		}
	}

}
