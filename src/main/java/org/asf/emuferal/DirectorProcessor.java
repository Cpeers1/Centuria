package org.asf.emuferal;

import java.io.ByteArrayOutputStream;
import java.net.Socket;

import org.asf.rats.ConnectiveHTTPServer;
import org.asf.rats.processors.HttpUploadProcessor;

import com.google.gson.JsonObject;

public class DirectorProcessor extends HttpUploadProcessor {

	public void process(String contentType, Socket client, String method) {
		String path = this.getRequestPath();

		try {
			byte[] body = new byte[0];
			if (method.toUpperCase().equals("POST")) {
				ByteArrayOutputStream strm = new ByteArrayOutputStream();
				ConnectiveHTTPServer.transferRequestBody(getHeaders(), getRequestBodyStream(), strm);
				body = strm.toByteArray();
				strm.close();
			}

			switch (path) {
			case "/v1/bestGameServer": {
				// Send response
				JsonObject response = new JsonObject();
				response.addProperty("smartfoxServer", EmuFeral.discoveryAddress);
				setBody(response.toString());
				break;
			}
			default:
				path = path;
				break;
			}
		} catch (Exception e) {
			e = e;
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
