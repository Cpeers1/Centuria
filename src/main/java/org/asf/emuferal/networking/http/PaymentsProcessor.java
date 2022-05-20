package org.asf.emuferal.networking.http;

import java.net.Socket;

import org.asf.rats.processors.HttpUploadProcessor;

public class PaymentsProcessor extends HttpUploadProcessor {

	@Override
	public void process(String contentType, Socket client, String method) {
		String path = this.getRequestPath();
		path = path;
	}

	@Override
	public HttpUploadProcessor createNewInstance() {
		return new PaymentsProcessor();
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
