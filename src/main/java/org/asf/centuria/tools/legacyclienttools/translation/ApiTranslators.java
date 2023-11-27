package org.asf.centuria.tools.legacyclienttools.translation;

import java.io.InputStream;
import java.util.UUID;

public class ApiTranslators {

	/**
	 * Translates the request paths of API calls
	 * 
	 * @param path Input path
	 * @return Translated request path
	 */
	public static String translateApiRequestPath(String path) {
		// Player
		if (path.startsWith("/r/player/")) {
			String plr = path.substring("/r/player/".length());
			if (plr.contains("/")) {
				plr = plr.substring(0, plr.indexOf("/"));
				try {
					UUID.fromString(plr);
					path = path.substring("/r/player/".length());
					path = path.substring(path.indexOf("/") + 1);
					path = "/r/" + path;
				} catch (Exception e) {
				}
			}
		}

		// Return
		return path;
	}

	/**
	 * Translates API request bodies
	 * 
	 * @param path Request function path
	 * @param body Request body
	 * @return New body bytes
	 */
	public static byte[] translateApiRequestBody(String path, byte[] body) {
		// TODO Auto-generated method stub
		return body;
	}

	/**
	 * Translates API response bodies
	 * 
	 * @param path Request function path
	 * @param body Response body
	 * @return New body stream
	 */
	public static InputStream translateApiResponseBody(String path, InputStream body) {
		// TODO Auto-generated method stub
		return body;
	}

}
