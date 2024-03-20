package org.asf.centuria.networking.chatserver.proxies;

import java.util.ArrayList;

import org.asf.centuria.accounts.CenturiaAccount;
import org.asf.centuria.accounts.PlayerInventory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * 
 * Chat Proxying Information
 * 
 * @author Sky Swimmer
 * 
 */
public class ChatProxyInfo {

	public String displayName;

	public String triggerPrefix;
	public String triggerSuffix;

	public String proxyPronouns;
	public String proxyBio;

	public boolean publiclyVisible = true;

	/**
	 * Finds proxies of users by name
	 * 
	 * @param user      User to retrieve the proxy of
	 * @param proxyName Proxy name
	 * @return ChatProxyInfo instance or null
	 */
	public static ChatProxyInfo ofUser(CenturiaAccount user, String proxyName) {
		// Get save
		PlayerInventory data = user.getSaveSpecificInventory();
		if (data.containsItem("proxy-" + proxyName.toLowerCase())) {
			// Load
			return new ChatProxyInfo().loadJson(data.getItem("proxy-" + proxyName.toLowerCase()).getAsJsonObject());
		}

		// Not found
		return null;
	}

	/**
	 * Checks a proxy exists
	 * 
	 * @param user      User to retrieve the proxy of
	 * @param proxyName Proxy name
	 * @return True if the proxy exists, false otherwise
	 */
	public static boolean proxyExists(CenturiaAccount user, String proxyName) {
		// Get save
		PlayerInventory data = user.getSaveSpecificInventory();
		return data.containsItem("proxy-" + proxyName.toLowerCase());
	}

	/**
	 * Retrieves all proxy names of a user
	 * 
	 * @param user User to retrieve all proxies of
	 * @return Array of ChatProxyInfo instances
	 */
	public static String[] allProxyNamesOfUser(CenturiaAccount user) {
		// Get save
		PlayerInventory data = user.getSaveSpecificInventory();
		if (data.containsItem("proxies-list")) {
			// Load
			JsonArray arr = data.getItem("proxies-list").getAsJsonArray();
			ArrayList<String> lst = new ArrayList<String>();
			for (JsonElement ele : arr)
				lst.add(ele.getAsString());
			return lst.toArray(t -> new String[t]);
		}

		// Not found
		return new String[0];
	}

	/**
	 * Retrieves all proxies of a user
	 * 
	 * @param user User to retrieve all proxies of
	 * @return Array of ChatProxyInfo instances
	 */
	public static ChatProxyInfo[] allOfUser(CenturiaAccount user) {
		// Get save
		PlayerInventory data = user.getSaveSpecificInventory();
		if (data.containsItem("proxies-list")) {
			// Load
			JsonArray arr = data.getItem("proxies-list").getAsJsonArray();
			ArrayList<ChatProxyInfo> lst = new ArrayList<ChatProxyInfo>();
			for (JsonElement ele : arr)
				lst.add(ofUser(user, ele.getAsString()));
			return lst.toArray(t -> new ChatProxyInfo[t]);
		}

		// Not found
		return new ChatProxyInfo[0];
	}

	/**
	 * Saves or creates proxies
	 * 
	 * @param user          User to save the proxy of
	 * @param name          Proxy name to save
	 * @param triggerPrefix Trigger prefix of the new proxy
	 * @return ChatProxyInfo instance
	 */
	public static ChatProxyInfo saveProxy(CenturiaAccount user, String name, String triggerPrefix,
			String triggerSuffix) {
		ChatProxyInfo info = new ChatProxyInfo();
		info.displayName = name;
		info.triggerPrefix = triggerPrefix;
		info.triggerSuffix = triggerSuffix;
		info.proxyBio = "N/A";
		info.proxyPronouns = "N/A";
		return saveProxy(user, info);
	}

	/**
	 * Saves or creates proxies
	 * 
	 * @param user  User to save the proxy of
	 * @param proxy Proxy to save
	 * @return ChatProxyInfo instance
	 */
	public static ChatProxyInfo saveProxy(CenturiaAccount user, ChatProxyInfo proxy) {
		if (!proxy.displayName.matches("^[A-Za-z0-9_\\-. ]+")) {
			// Invalid name
			throw new IllegalArgumentException("Proxy name contains invalid characters");
		}

		// Get save
		PlayerInventory data = user.getSaveSpecificInventory();

		// Check if existing
		if (!proxyExists(user, proxy.displayName)) {
			// Add to list
			JsonArray arr = new JsonArray();
			if (data.containsItem("proxies-list"))
				arr = data.getItem("proxies-list").getAsJsonArray();
			arr.add(proxy.displayName);
			data.setItem("proxies-list", arr);
		}

		// Save proxy
		data.setItem("proxy-" + proxy.displayName.toLowerCase(), proxy.toJson());

		// Return
		return proxy;
	}

	/**
	 * Deletes proxies
	 * 
	 * @param user      User to delete the proxy of
	 * @param proxyName Name of the proxy to delete
	 * @return True if the proxy was deleted, false otherwise
	 */
	public static boolean deleteProxy(CenturiaAccount user, String proxyName) {
		// Get save
		PlayerInventory data = user.getSaveSpecificInventory();

		// Check if existing
		if (!proxyExists(user, proxyName))
			return false;

		// Delete
		data.deleteItem("proxy-" + proxyName.toLowerCase());

		// Remove from list
		if (data.containsItem("proxies-list")) {
			JsonArray arr = data.getItem("proxies-list").getAsJsonArray();
			for (JsonElement ele : arr) {
				if (ele.getAsString().toLowerCase().equals(proxyName.toLowerCase())) {
					// Remove
					arr.remove(ele);
					data.setItem("proxies-list", arr);
					break;
				}
			}
		}

		// Return
		return true;
	}

	public JsonObject toJson() {
		JsonObject obj = new JsonObject();
		obj.addProperty("displayName", displayName);
		obj.addProperty("triggerPrefix", triggerPrefix);
		obj.addProperty("triggerSuffix", triggerSuffix);
		obj.addProperty("proxyPronouns", proxyPronouns);
		obj.addProperty("proxyBio", proxyBio);
		obj.addProperty("publiclyVisible", publiclyVisible);
		return obj;
	}

	public ChatProxyInfo loadJson(JsonObject obj) {
		displayName = obj.get("displayName").getAsString();
		triggerPrefix = obj.get("triggerPrefix").getAsString();
		triggerSuffix = obj.get("triggerSuffix").getAsString();
		proxyPronouns = obj.get("proxyPronouns").getAsString();
		proxyBio = obj.get("proxyBio").getAsString();
		publiclyVisible = obj.get("publiclyVisible").getAsBoolean();
		return this;
	}

}