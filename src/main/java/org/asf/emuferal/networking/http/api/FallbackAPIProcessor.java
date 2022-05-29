package org.asf.emuferal.networking.http.api;

import java.io.ByteArrayOutputStream;
import java.net.Socket;
import java.util.Base64;
import java.util.UUID;

import org.asf.emuferal.EmuFeral;
import org.asf.emuferal.accounts.AccountManager;
import org.asf.rats.ConnectiveHTTPServer;
import org.asf.rats.processors.HttpUploadProcessor;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class FallbackAPIProcessor extends HttpUploadProcessor {

	public static String KeyID = UUID.randomUUID().toString();

	@Override
	public void process(String contentType, Socket client, String method) {
		String path = this.getRequestPath();
		AccountManager manager = AccountManager.getInstance();

		try {
			// Parse body (if present)
			byte[] body = new byte[0];
			if (method.toUpperCase().equals("POST")) {
				ByteArrayOutputStream strm = new ByteArrayOutputStream();
				ConnectiveHTTPServer.transferRequestBody(getHeaders(), getRequestBodyStream(), strm);
				body = strm.toByteArray();
				strm.close();
			}
			if (path.startsWith("/ca/auth/")) {
				// Build the JWT
				JsonObject headers = new JsonObject();
				headers.addProperty("alg", "RS256");
				headers.addProperty("kid", KeyID);
				headers.addProperty("typ", "JWT");
				String headerD = Base64.getUrlEncoder().encodeToString(headers.toString().getBytes("UTF-8"));

				JsonObject payload = new JsonObject();
				payload.addProperty("iat", System.currentTimeMillis() / 1000);
				payload.addProperty("exp", (System.currentTimeMillis() / 1000) + 30);
				payload.addProperty("jti", UUID.randomUUID().toString());
				payload.addProperty("iss", "EmuFeral");
				payload.addProperty("sub", "EmuFeral");
				String payloadD = Base64.getUrlEncoder().encodeToString(payload.toString().getBytes("UTF-8"));

				// Send response
				JsonObject response = new JsonObject();
				response.addProperty("autorization_key", headerD + "." + payloadD + "." + Base64.getUrlEncoder()
						.encodeToString(EmuFeral.sign((headerD + "." + payloadD).getBytes("UTF-8"))));
				setBody(response.toString());
			} else if (path.startsWith("/r/block/")) {
				// Parse JWT payload
				String token = this.getHeader("Authorization").substring("Bearer ".length());

				// Verify signature
				String verifyD = token.split("\\.")[0] + "." + token.split("\\.")[1];
				String sig = token.split("\\.")[2];
				if (!EmuFeral.verify(verifyD.getBytes("UTF-8"), Base64.getUrlDecoder().decode(sig))) {
					this.setResponseCode(401);
					this.setResponseMessage("Access denied");
					return;
				}

				// Verify expiry
				JsonObject jwtPl = JsonParser
						.parseString(new String(Base64.getUrlDecoder().decode(token.split("\\.")[1]), "UTF-8"))
						.getAsJsonObject();
				if (!jwtPl.has("exp") || jwtPl.get("exp").getAsLong() < System.currentTimeMillis() / 1000) {
					this.setResponseCode(401);
					this.setResponseMessage("Access denied");
					return;
				}

				// Request
				String id = path.substring("/r/block/".length());

				// Send response
				JsonObject response = new JsonObject();
				// TODO: blocking users
				response.addProperty("error", "not_blocked");
				setBody(response.toString());
			} else if (path.startsWith("/r/follow/")) {
				// Parse JWT payload
				String token = this.getHeader("Authorization").substring("Bearer ".length());

				// Verify signature
				String verifyD = token.split("\\.")[0] + "." + token.split("\\.")[1];
				String sig = token.split("\\.")[2];
				if (!EmuFeral.verify(verifyD.getBytes("UTF-8"), Base64.getUrlDecoder().decode(sig))) {
					this.setResponseCode(401);
					this.setResponseMessage("Access denied");
					return;
				}

				// log interaction details
				if (System.getProperty("debugMode") != null) {
					System.out
							.println("[API] [r/follow]  Client to server ( path:" + path + " ) ( body: " + body + " )");
				}

				// TODO: Following other players.
			} else if (path.startsWith("/r/followers")) {
				// Parse JWT payload
				String token = this.getHeader("Authorization").substring("Bearer ".length());

				// Verify signature
				String verifyD = token.split("\\.")[0] + "." + token.split("\\.")[1];
				String sig = token.split("\\.")[2];
				if (!EmuFeral.verify(verifyD.getBytes("UTF-8"), Base64.getUrlDecoder().decode(sig))) {
					this.setResponseCode(401);
					this.setResponseMessage("Access denied");
					return;
				}

				// log details
				if (System.getProperty("debugMode") != null) {
					System.out.println(
							"[API] [r/followers]  Client to server ( path:" + path + " ) ( body: " + body + " )");
				}

				// TODO: Retrieving followers.
				// [{"created_at":"2022-03-26 16:24:20","favorite":true,"updated_at":"2022-03-26
				// 18:28:32","uuid":"75d35f12-6614-4793-ba12-a11f0e9819c4"}]
			} else if (path.startsWith("/r/followings")) {
				// Parse JWT payload
				String token = this.getHeader("Authorization").substring("Bearer ".length());

				// Verify signature
				String verifyD = token.split("\\.")[0] + "." + token.split("\\.")[1];
				String sig = token.split("\\.")[2];
				if (!EmuFeral.verify(verifyD.getBytes("UTF-8"), Base64.getUrlDecoder().decode(sig))) {
					this.setResponseCode(401);
					this.setResponseMessage("Access denied");
					return;
				}

				// log details
				if (System.getProperty("debugMode") != null) {
					System.out.println(
							"[API] [r/followings]  Client to server ( path:" + path + " ) ( body: " + body + " )");
				}

				// TODO: Retrieving players being followed.
				// [{"created_at":"2022-03-26 16:24:20","favorite":true,"updated_at":"2022-03-26
				// 18:28:32","uuid":"75d35f12-6614-4793-ba12-a11f0e9819c4"}]
			} else {
				// log details
				if (System.getProperty("debugMode") != null) {
					System.err.println("[API] Unhandled Api Call: ( path:" + path + " ) ( body: " + body + " )");
				}

				setResponseCode(400);
				path = path;
				setBody("{}");
			}
		} catch (Exception e) {
			e = e;
		}
	}

	@Override
	public HttpUploadProcessor createNewInstance() {
		return new FallbackAPIProcessor();
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
