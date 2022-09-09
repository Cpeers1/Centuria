package org.asf.centuria.networking.http.api;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.TimeZone;
import java.util.UUID;

import org.asf.centuria.Centuria;
import org.asf.centuria.accounts.AccountManager;
import org.asf.centuria.accounts.CenturiaAccount;
import org.asf.centuria.entities.players.Player;
import org.asf.centuria.social.SocialEntry;
import org.asf.centuria.social.SocialManager;
import org.asf.rats.ConnectiveHTTPServer;
import org.asf.rats.processors.HttpUploadProcessor;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

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
				payload.addProperty("exp", (System.currentTimeMillis() / 1000) + (1 * 24 * 60));
				payload.addProperty("jti", UUID.randomUUID().toString());
				payload.addProperty("iss", "Centuria");
				payload.addProperty("sub", "Centuria");
				String payloadD = Base64.getUrlEncoder().encodeToString(payload.toString().getBytes("UTF-8"));

				// Send response
				JsonObject response = new JsonObject();
				response.addProperty("autorization_key", headerD + "." + payloadD + "." + Base64.getUrlEncoder()
						.encodeToString(Centuria.sign((headerD + "." + payloadD).getBytes("UTF-8"))));
				setBody(response.toString());
			} else if (path.startsWith("/r/block/")) {
				// Find account
				CenturiaAccount acc = verifyAndGetAcc(manager);
				if (acc == null) {
					this.setResponseCode(401);
					this.setResponseMessage("Access denied");
					return;
				}

				JsonObject response = new JsonObject();
				switch (method.toLowerCase()) {
				case "get": {
					String sourcePlayerID = acc.getAccountID();
					String targetPlayerID = path.substring("/r/block/".length());

					var socialListManager = SocialManager.getInstance();
					// open friend list
					socialListManager.openSocialList(targetPlayerID);
					if (socialListManager.getPlayerIsBlocked(sourcePlayerID, targetPlayerID)) {
						SimpleDateFormat fmt = new SimpleDateFormat("YYYY-MM-dd'T'HH:mm:ss.'0Z'");
						fmt.setTimeZone(TimeZone.getTimeZone("UTC"));
						String createdAt = fmt.format(new Date());

						response.addProperty("created_at", createdAt);
					} else {
						response.addProperty("error", "not_blocked");
					}

					if (Centuria.debugMode) {
						System.out.println("[API] [r/block] [" + method + "] | Processed get block status request ");
					}

					break;
				}
				case "post": {
					String sourcePlayerID = acc.getAccountID();
					String targetPlayerID = path.substring("/r/block/".length());

					var socialListManager = SocialManager.getInstance();

					// open friend list
					socialListManager.openSocialList(sourcePlayerID);

					// check existing block
					if (socialListManager.getPlayerIsBlocked(sourcePlayerID, targetPlayerID)) {
						// error
						setResponseCode(200);
						setResponseMessage("No Content");
						setBody("{\"error\":\"already_blocked\"}");
						return;
					}

					// add player is blocked.
					socialListManager.setBlockedPlayer(sourcePlayerID, targetPlayerID, true);

					if (Centuria.debugMode) {
						System.out.println("[API] [r/block] [" + method + "] | Processed block Request ");
					}

					setResponseCode(201);
					setResponseMessage("No Content");
					setBody("");

					break;
				}
				case "delete": {
					String sourcePlayerID = acc.getAccountID();
					String targetPlayerID = path.substring("/r/block/".length());

					var socialListManager = SocialManager.getInstance();

					// open friend list
					socialListManager.openSocialList(sourcePlayerID);

					// log details
					if (Centuria.debugMode) {
						System.out.println("[API] [r/block] [" + method + "] | Processed Unblock Request ");
					}

					// check block
					if (!socialListManager.getPlayerIsBlocked(sourcePlayerID, targetPlayerID)) {
						// error
						setResponseCode(200);
						setResponseMessage("No Content");
						setBody("{\"error\":\"not_blocked\"}");
						return;
					}

					// unblock
					socialListManager.setBlockedPlayer(sourcePlayerID, targetPlayerID, false);

					break;
				}
				default: {
					// log details
					if (Centuria.debugMode) {
						System.err.println("[API] [r/block] [" + method + "] | Unhandled method ");
					}
				}
				}

				setBody(response.toString());
			} else if (path.startsWith("/r/follow/")) {

				// Find account
				CenturiaAccount acc = verifyAndGetAcc(manager);
				if (acc == null) {
					this.setResponseCode(401);
					this.setResponseMessage("Access denied");
					return;
				}

				String targetPlayerID = path.split("/")[3];
				String sourcePlayerID = acc.getAccountID();

				// log details
				if (Centuria.debugMode) {
					System.out.println("[API] [r/follow] [" + method + "] inbound: ( source: " + sourcePlayerID
							+ ", target: " + targetPlayerID + " )");
				}

				// open friend list
				SocialManager.getInstance().openSocialList(sourcePlayerID);
				SocialManager.getInstance().openSocialList(targetPlayerID);

				switch (method.toLowerCase()) {
				case "post": {
					// log interaction details
					if (Centuria.debugMode) {
						System.out.println("[API] [r/follow] Processed friend request, sending 201... ");
					}

					// check follow state
					if (SocialManager.getInstance().getPlayerIsFollowing(sourcePlayerID, targetPlayerID)) {
						// error
						setResponseCode(200);
						setResponseMessage("No Content");
						setBody("{\"error\":\"already_following\"}");
						return;
					}

					// check follow count
					if (SocialManager.getInstance().getFollowingPlayers(sourcePlayerID).length > 1000) {
						// error
						setResponseCode(200);
						setResponseMessage("No Content");
						setBody("{\"error\":\"limit_reached\"}");
						return;
					}

					SocialManager.getInstance().setFollowingPlayer(sourcePlayerID, targetPlayerID, true);
					SocialManager.getInstance().setFollowerPlayer(targetPlayerID, sourcePlayerID, true);

					setResponseCode(201);
					setResponseMessage("No Content");

					// inform the client if possible
					Player plr = acc.getOnlinePlayerInstance();
					if (plr != null)
						plr.client.sendPacket("%xt%rfosu%-1%" + targetPlayerID + "%"
								+ (Centuria.gameServer.getPlayer(targetPlayerID) == null ? "-1" : "1") + "%");

					break;
				}
				case "delete": {
					// check follow state
					if (!SocialManager.getInstance().getPlayerIsFollowing(sourcePlayerID, targetPlayerID)) {
						// error
						setResponseCode(200);
						setResponseMessage("No Content");
						setBody("{\"error\":\"not_following\"}");
						return;
					}

					// must be trying to remove friend
					SocialManager.getInstance().setFollowingPlayer(sourcePlayerID, targetPlayerID, false);
					SocialManager.getInstance().setFollowerPlayer(targetPlayerID, sourcePlayerID, false);
					break;
				}
				default: {
					// log details
					if (Centuria.debugMode) {
						System.err.println("[API] [r/follow] [" + method + "] | Unhandled method ");
					}
				}
				}
			} else if (path.startsWith("/r/followers")) {

				// verify token and get the account.
				var acc = verifyAndGetAcc(manager);
				if (acc == null) {
					this.setResponseCode(401);
					this.setResponseMessage("Access denied");
					return;
				}

				String sourcePlayerID = acc.getAccountID();

				// log details
				if (Centuria.debugMode) {
					System.out.println(
							"[API] [r/followers] [" + method + "] Client to server ( source: " + sourcePlayerID + " )");
				}

				// TODO: UPDATED AT FIELD.

				// open friend list
				SocialManager.getInstance().openSocialList(sourcePlayerID);

				// get followers list
				var followerList = SocialManager.getInstance().getFollowerPlayers(sourcePlayerID);

				// make new json Array
				JsonArray jsonArray = new JsonArray();

				for (SocialEntry entry : followerList) {
					// make a new json object for the entry
					var newJsonObject = new JsonObject();
					newJsonObject.addProperty("created_at", entry.addedAt);
					newJsonObject.addProperty("favorite", entry.favorite);
					newJsonObject.addProperty("updated_at", entry.updatedAt);
					newJsonObject.addProperty("uuid", entry.playerID);

					jsonArray.add(newJsonObject);
				}

				// send response packet
				setBody(jsonArray.toString());

				// log interaction details
				if (Centuria.debugMode) {
					System.out.println("[API] [r/followers] outbound: ( " + jsonArray.toString() + " )");
				}

			} else if (path.startsWith("/r/followings")) {

				// verify token and get the account.
				var acc = verifyAndGetAcc(manager);

				if (acc == null) {
					this.setResponseCode(401);
					this.setResponseMessage("Access denied");
					return;
				}

				String sourcePlayerID = acc.getAccountID();

				// log details
				if (Centuria.debugMode) {
					System.out.println("[API] [r/followings] [" + method + "]  Client to server ( source: "
							+ sourcePlayerID + " )");
				}

				// open friend list
				SocialManager.getInstance().openSocialList(sourcePlayerID);

				// get following list
				var followingList = SocialManager.getInstance().getFollowingPlayers(sourcePlayerID);

				// make new json Array
				JsonArray jsonArray = new JsonArray();

				for (SocialEntry entry : followingList) {
					// make a new json object for the entry
					var newJsonObject = new JsonObject();
					newJsonObject.addProperty("created_at", entry.addedAt);
					newJsonObject.addProperty("favorite", entry.favorite);
					newJsonObject.addProperty("updated_at", entry.updatedAt);
					newJsonObject.addProperty("uuid", entry.playerID);

					jsonArray.add(newJsonObject);
				}

				// send response packet
				setBody(jsonArray.toString());

				// log interaction details
				if (Centuria.debugMode) {
					System.out.println("[API] [r/followings] outbound: ( " + jsonArray.toString() + " )");
				}
			} else if (path.startsWith("/r/favorite")) {

				// verify token and get the account.
				var acc = verifyAndGetAcc(manager);

				if (acc == null) {
					this.setResponseCode(401);
					this.setResponseMessage("Access denied");
					return;
				}

				String targetPlayerID = path.split("/")[3];
				String sourcePlayerID = acc.getAccountID();

				// log details
				if (Centuria.debugMode) {
					System.out.println("[API] [r/favorite] [" + method + "]  Client to server ( source: "
							+ sourcePlayerID + ", target : " + targetPlayerID + ", body: " + body + " )");
				}

				var friendListManager = SocialManager.getInstance();
				// open friend list
				friendListManager.openSocialList(sourcePlayerID);

				switch (method.toLowerCase()) {
				case "post": {
					friendListManager.setFavoritePlayer(sourcePlayerID, targetPlayerID, true);

					setResponseCode(201);
					setResponseMessage("No Content");

					// log details
					if (Centuria.debugMode) {
						System.out.println("[API] [r/favorite] [" + method + "] Handled.");
					}

					break;
				}
				case "delete": {
					// oops.. its kind of the same
					friendListManager.setFavoritePlayer(sourcePlayerID, targetPlayerID, false);

					// log details
					if (Centuria.debugMode) {
						System.out.println("[API] [r/favorite] [" + method + "] Handled.");
					}
					break;
				}
				default: {
					// log details
					if (Centuria.debugMode) {
						System.err.println("[API] [r/favorite] [" + method + "] | Unhandled method ");
					}
				}
				}

			} else if (path.startsWith("/s/desktop")) {

				// dud handler

				// log details
				if (Centuria.debugMode) {
					System.out.println("[API] [/s/desktop] [" + method + "] DUD");
				}
			} else {
				// log details
				if (Centuria.debugMode) {
					System.err.println("[API] Unhandled Api Call: ( path:" + path + " ) ( method: " + method
							+ " ) ( body: " + body + " )");
				}

				setResponseCode(400);
				setBody("{}");
			}
		} catch (Exception e) {
			if (Centuria.debugMode) {
				System.err.println("[FALLBACKAPI] ERROR : " + e.getMessage() + " )");
			}
			Centuria.logger.error(getRequest().path + " failed", e);
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

	private CenturiaAccount verifyAndGetAcc(AccountManager manager)
			throws JsonSyntaxException, UnsupportedEncodingException {
		// Parse JWT payload
		String token = this.getHeader("Authorization").substring("Bearer ".length());

		// Verify signature
		String verifyD = token.split("\\.")[0] + "." + token.split("\\.")[1];
		String sig = token.split("\\.")[2];
		if (!Centuria.verify(verifyD.getBytes("UTF-8"), Base64.getUrlDecoder().decode(sig))) {
			return null;
		}

		// Verify expiry
		JsonObject jwtPl = JsonParser
				.parseString(new String(Base64.getUrlDecoder().decode(token.split("\\.")[1]), "UTF-8"))
				.getAsJsonObject();
		if (!jwtPl.has("exp") || jwtPl.get("exp").getAsLong() < System.currentTimeMillis() / 1000) {
			return null;
		}

		JsonObject payload = JsonParser
				.parseString(new String(Base64.getUrlDecoder().decode(token.split("\\.")[1]), "UTF-8"))
				.getAsJsonObject();

		// Find account
		CenturiaAccount acc = manager.getAccount(payload.get("uuid").getAsString());

		return acc;
	}

}
