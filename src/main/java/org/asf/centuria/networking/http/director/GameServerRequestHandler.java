package org.asf.centuria.networking.http.director;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

import org.asf.centuria.Centuria;
import org.asf.connective.RemoteClient;
import org.asf.connective.impl.http_1_1.RemoteClientHttp_1_1;
import org.asf.connective.processors.HttpPushProcessor;

import com.google.gson.JsonObject;

public class GameServerRequestHandler extends HttpPushProcessor {

	@Override
	public void process(String path, String method, RemoteClient client, String contentType) throws IOException {
		// Send response
		String addr = Centuria.discoveryAddress;
		if (addr.equals("localhost") || addr.equals("127.0.0.1")) {
			// Get local IP
			String host = "localhost";
			if (client instanceof RemoteClientHttp_1_1) {
				RemoteClientHttp_1_1 cl = (RemoteClientHttp_1_1) client;
				Socket sock = cl.getSocket();

				// Get interface
				SocketAddress ad = sock.getLocalSocketAddress();
				if (ad instanceof InetSocketAddress) {
					InetSocketAddress iA = (InetSocketAddress) ad;
					host = iA.getAddress().getHostAddress();
					addr = host;
				}
			}
			if (host.equals("localhost"))
				addr = "127.0.0.1";
		}
		JsonObject response = new JsonObject();
		response.addProperty("smartfoxServer", addr); // load discovery address
		setResponseContent("text/json", response.toString());
	}

	@Override
	public HttpPushProcessor createNewInstance() {
		return new GameServerRequestHandler();
	}

	@Override
	public String path() {
		return "/v1/bestGameServer";
	}

	@Override
	public boolean supportsNonPush() {
		return true;
	}

	@Override
	public boolean supportsChildPaths() {
		return true;
	}

}
