package org.asf.centuria.discord;

import java.util.ArrayList;

import org.asf.centuria.accounts.CenturiaAccount;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

public class UserIpBlockUtils {

	/**
	 * Checks if the given user has blocked the given IP address
	 * 
	 * @param account Account owning the block list
	 * @param ip      IP to check
	 * @return True if blocked, false otherwise
	 */
	public static boolean isBlocked(CenturiaAccount account, String ip) {
		if (account.getPlayerInventory().containsItem("blockedips")) {
			// Load item
			JsonArray arr = account.getPlayerInventory().getItem("blockedips").getAsJsonArray();

			// Loop through array, find the IP
			for (JsonElement ele : arr)
				if (ele.getAsString().equals(ip))
					return true;
		}
		return false;
	}

	/**
	 * Retrieves an array of blocked addresses
	 * 
	 * @param account Account owning the block list
	 * @return Array of blocked IPs
	 */
	public static String[] getBlockedIps(CenturiaAccount account) {
		if (account.getPlayerInventory().containsItem("blockedips")) {
			// Load item
			JsonArray arr = account.getPlayerInventory().getItem("blockedips").getAsJsonArray();

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
	 * Blocks the given IP address
	 * 
	 * @param account Account owning the block list
	 * @param ip      IP to block
	 */
	public static void blockIp(CenturiaAccount account, String ip) {
		if (!isBlocked(account, ip)) {
			// Create list if not present
			if (!account.getPlayerInventory().containsItem("blockedips"))
				account.getPlayerInventory().setItem("blockedips", new JsonArray());

			// Load list
			JsonArray arr = account.getPlayerInventory().getItem("blockedips").getAsJsonArray();

			// Add item
			arr.add(ip);

			// Save
			account.getPlayerInventory().setItem("blockedips", arr);
		}
	}

	/**
	 * Unblock the given IP address
	 * 
	 * @param account Account owning the block list
	 * @param ip      IP to unblock
	 */
	public static void unblockIp(CenturiaAccount account, String ip) {
		if (isBlocked(account, ip)) {
			// Load list
			JsonArray arr = account.getPlayerInventory().getItem("blockedips").getAsJsonArray();

			// Find and remove item
			for (JsonElement ele : arr)
				if (ele.getAsString().equals(ip)) {
					arr.remove(ele);
					break;
				}

			// Save
			account.getPlayerInventory().setItem("blockedips", arr);
		}
	}

}
