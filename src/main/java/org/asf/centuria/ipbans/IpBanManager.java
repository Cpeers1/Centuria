package org.asf.centuria.ipbans;

import org.asf.centuria.ipbans.impl.FileBasedIpBanManager;

public abstract class IpBanManager {

	protected static IpBanManager implementation = new FileBasedIpBanManager();

	/**
	 * Retrieves the IP ban manager
	 * 
	 * @return IpBanManager instance
	 */
	public static IpBanManager getInstance() {
		return implementation;
	}

	/**
	 * Bans a IP address
	 * 
	 * @param ip IP to ban
	 */
	public abstract void banIP(String ip);

	/**
	 * Removes a IP address ban
	 * 
	 * @param ip IP to unban
	 */
	public abstract void unbanIP(String ip);

	/**
	 * Checksc if a IP is banned
	 * 
	 * @param ip IP to check
	 */
	public abstract boolean isIPBanned(String ip);

}
