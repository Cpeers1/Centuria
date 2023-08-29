package org.asf.centuria.modules.events.servers;

import org.asf.centuria.modules.eventbus.EventObject;
import org.asf.centuria.modules.eventbus.EventPath;
import org.asf.connective.ConnectiveHttpServer;

/**
 * 
 * Director Startup Event - used to handle startup of the director server and
 * register handlers via modules.
 * 
 * @author Sky Swimmer - AerialWorks Software Foundation
 *
 */
@EventPath("director.startup")
public class DirectorServerStartupEvent extends EventObject {

	private ConnectiveHttpServer server;

	public DirectorServerStartupEvent(ConnectiveHttpServer server) {
		this.server = server;
	}

	@Override
	public String eventPath() {
		return "director.startup";
	}

	/**
	 * Retrieves the Director server
	 * 
	 * @return ConnectiveHttpServer instance
	 */
	public ConnectiveHttpServer getServer() {
		return server;
	}

}
