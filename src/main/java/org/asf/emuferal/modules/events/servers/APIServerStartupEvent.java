package org.asf.emuferal.modules.events.servers;

import java.util.Map;

import org.asf.emuferal.modules.eventbus.EventObject;
import org.asf.emuferal.modules.eventbus.EventPath;
import org.asf.rats.ConnectiveHTTPServer;

/**
 * 
 * API Startup Event - used to handle startup of the API server and register
 * handlers via modules.
 * 
 * @author Sky Swimmer - AerialWorks Software Foundation
 *
 */
@EventPath("api.startup")
public class APIServerStartupEvent extends EventObject {

	private ConnectiveHTTPServer server;

	public APIServerStartupEvent(ConnectiveHTTPServer server) {
		this.server = server;
	}

	@Override
	public String eventPath() {
		return "api.startup";
	}

	/**
	 * Retrieves the API server
	 * 
	 * @return ConnectiveHTTPServer instance
	 */
	public ConnectiveHTTPServer getServer() {
		return server;
	}

	@Override
	public Map<String, String> eventProperties() {
		return Map.of();
	}

}
