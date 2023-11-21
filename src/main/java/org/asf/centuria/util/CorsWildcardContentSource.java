package org.asf.centuria.util;

import java.io.IOException;

import org.asf.connective.ConnectiveHttpServer;
import org.asf.connective.ContentSource;
import org.asf.connective.RemoteClient;
import org.asf.connective.objects.HttpRequest;
import org.asf.connective.objects.HttpResponse;

public class CorsWildcardContentSource extends ContentSource {

	@Override
	public boolean process(String path, HttpRequest request, HttpResponse response, RemoteClient client,
			ConnectiveHttpServer server) throws IOException {
		response.addHeader("Access-Control-Allow-Origin", "*");
		return getParent().process(path, request, response, client, server);
	}

}
