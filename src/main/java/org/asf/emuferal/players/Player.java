package org.asf.emuferal.players;

import org.asf.emuferal.networking.smartfox.SmartfoxClient;

public class Player {

	public int userID;
	public boolean isNew;
	public String userUUID;
	public String loginName;
	public String displayName;
	public SmartfoxClient client;

	public String activeLook;
	public String activeSanctuaryLook;

	public int pendingLookDefID = 8254;
	public String pendingLookID = null;
	public String respawn = null;
	public String room = null;

}
