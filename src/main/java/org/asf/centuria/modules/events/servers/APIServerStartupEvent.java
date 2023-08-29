package org.asf.centuria.modules.events.servers;

import org.asf.centuria.modules.eventbus.EventObject;
import org.asf.centuria.modules.eventbus.EventPath;
import org.asf.connective.ConnectiveHttpServer;

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

	private ConnectiveHttpServer server;

	public APIServerStartupEvent(ConnectiveHttpServer server) {
		this.server = server;
	}

	@Override
	public String eventPath() {
		return "api.startup";
	}

	/**
	 * Retrieves the API server
	 * 
	 * @return ConnectiveHttpServer instance
	 */
	public ConnectiveHttpServer getServer() {
		return server;
	}

}
