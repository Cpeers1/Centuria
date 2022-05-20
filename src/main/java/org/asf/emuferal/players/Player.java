package org.asf.emuferal.players;

import org.asf.emuferal.accounts.EmuFeralAccount;
import org.asf.emuferal.networking.smartfox.SmartfoxClient;

public class Player {
	
	public SmartfoxClient client;
	public EmuFeralAccount account;

	public String activeLook;
	public String activeSanctuaryLook;

	public int pendingLookDefID = 8254;
	public String pendingLookID = null;
	public String room = null;

	public String respawn = null;

}
