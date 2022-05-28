package org.asf.emuferal.networking.http;

import java.lang.reflect.InvocationTargetException;
import java.net.Socket;

import org.asf.emuferal.EmuFeral;
import org.asf.rats.ConnectiveHTTPServer;
import org.asf.rats.ConnectiveServerFactory;
import org.asf.rats.processors.HttpUploadProcessor;

import com.google.gson.JsonObject;

public class DirectorProcessor extends HttpUploadProcessor {

	public static void main(String[] args) throws NumberFormatException, InvocationTargetException {
		ConnectiveHTTPServer directorServer = new ConnectiveServerFactory().setPort(Integer.parseInt(args[0]))
				.setOption(ConnectiveServerFactory.OPTION_AUTOSTART)
				.setOption(ConnectiveServerFactory.OPTION_ASSIGN_PORT).build();
		directorServer.registerProcessor(new DirectorProcessor());
	}

	public void process(String contentType, Socket client, String method) {
		String path = this.getRequestPath();
		
		try {
			switch (path) {
			case "/v1/bestGameServer": {
				// Send response
				JsonObject response = new JsonObject();
				response.addProperty("smartfoxServer", EmuFeral.discoveryAddress); // load discovery address
				setBody(response.toString());
				break;
			}
			}
		} catch (Exception e) {
		}
	}

	@Override
	public HttpUploadProcessor createNewInstance() {
		return new DirectorProcessor();
	}

	@Override
	public String path() {
		return "/";
	}

	@Override
	public boolean supportsGet() {
		return true;
	}

	@Override
	public boolean supportsChildPaths() {
		return true;
	}

}
