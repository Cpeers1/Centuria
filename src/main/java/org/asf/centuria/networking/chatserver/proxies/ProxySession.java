package org.asf.centuria.networking.chatserver.proxies;

import java.util.HashMap;

public class ProxySession {

	public static class RoomProxySession {

		public boolean sticky;
		public String lastUsedProxyName;

	}

	public HashMap<String, RoomProxySession> roomSessions = new HashMap<String, RoomProxySession>();

}