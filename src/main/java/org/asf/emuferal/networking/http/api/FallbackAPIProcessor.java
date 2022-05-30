package org.asf.emuferal.networking.http.api;

import java.io.ByteArrayOutputStream;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.TimeZone;
import java.util.UUID;

import org.asf.emuferal.EmuFeral;
import org.asf.emuferal.accounts.AccountManager;
import org.asf.emuferal.accounts.EmuFeralAccount;
import org.asf.emuferal.friendlist.FriendListEntry;
import org.asf.emuferal.friendlist.FriendListManager;
import org.asf.rats.ConnectiveHTTPServer;
import org.asf.rats.processors.HttpUploadProcessor;

import com.google.gson.JsonArray;
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
				
	            JsonObject payload = JsonParser
	                    .parseString(new String(Base64.getUrlDecoder().decode(token.split("\\.")[1]), "UTF-8"))
	                    .getAsJsonObject();

	            // Find account
	            EmuFeralAccount acc = manager.getAccount(payload.get("uuid").getAsString());
	            if (acc == null) {
	                this.setResponseCode(401);
	                this.setResponseMessage("Access denied");
	                return;
	            }
	            
				String sourcePlayerID = acc.getAccountID();
				
				// log details
				if (System.getProperty("debugMode") != null) {
					System.out.println("[API] [r/block] inbound: ( source: " + sourcePlayerID + " )");
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
				
	             // Verify expiry
	            JsonObject jwtPl = JsonParser
	                    .parseString(new String(Base64.getUrlDecoder().decode(token.split("\\.")[1]), "UTF-8"))
	                    .getAsJsonObject();
	            if (!jwtPl.has("exp") || jwtPl.get("exp").getAsLong() < System.currentTimeMillis() / 1000) {
	                this.setResponseCode(401);
	                this.setResponseMessage("Access denied");
	                return;
	            }

	            JsonObject payload = JsonParser
	                    .parseString(new String(Base64.getUrlDecoder().decode(token.split("\\.")[1]), "UTF-8"))
	                    .getAsJsonObject();

	            // Find account
	            EmuFeralAccount acc = manager.getAccount(payload.get("uuid").getAsString());
	            if (acc == null) {
	                this.setResponseCode(401);
	                this.setResponseMessage("Access denied");
	                return;
	            }
				
				String targetPlayerID = path.split("/")[3];
				String sourcePlayerID = acc.getAccountID();
				
				// log details
				if (System.getProperty("debugMode") != null) {
					System.out.println("[API] [r/follow] inbound: ( source: " + sourcePlayerID + ", target: " + targetPlayerID + " )");
				}
				
				//open friend list
				FriendListManager.getInstance().openFriendList(sourcePlayerID);
				FriendListManager.getInstance().openFriendList(targetPlayerID);
				
				if(!FriendListManager.getInstance().getPlayerIsFollowing(sourcePlayerID, targetPlayerID))
				{
					//doing date stuff ahead of time
					SimpleDateFormat fmt = new SimpleDateFormat("YYYY-MM-dd'T'HH:mm:ss.'0Z'");
					fmt.setTimeZone(TimeZone.getTimeZone("UTC"));
					String createdAt = fmt.format(new Date());
					
					//add player to friend list
					FriendListEntry entry = new FriendListEntry();
					entry.addedAt = createdAt;
					entry.playerID = targetPlayerID;
					entry.favorite = false;
					
					FriendListManager.getInstance().addFollowingPlayer(sourcePlayerID, entry);
					
					//for the other player, add them to followers
					FriendListEntry followerEntry = new FriendListEntry();
					followerEntry.addedAt = createdAt;
					followerEntry.playerID = sourcePlayerID;
					followerEntry.favorite = false;
					
					FriendListManager.getInstance().addFollowerPlayer(targetPlayerID, followerEntry);
				}
				else
				{
					//must be trying to remove friend					
					FriendListManager.getInstance().removeFollowingPlayer(sourcePlayerID, targetPlayerID);
					FriendListManager.getInstance().removeFollowerPlayer(targetPlayerID, sourcePlayerID);
				}
							
				//construct response packet	
				JsonObject response = new JsonObject();
				response.addProperty("error", ""); // no error?
				setBody(response.toString());		
				
				// log interaction details
				if (System.getProperty("debugMode") != null) {
					System.out.println("[API] [r/follow] outbound: ( " + response.toString() + " )");
				}
				
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
				
	             // Verify expiry
	            JsonObject jwtPl = JsonParser
	                    .parseString(new String(Base64.getUrlDecoder().decode(token.split("\\.")[1]), "UTF-8"))
	                    .getAsJsonObject();
	            if (!jwtPl.has("exp") || jwtPl.get("exp").getAsLong() < System.currentTimeMillis() / 1000) {
	                this.setResponseCode(401);
	                this.setResponseMessage("Access denied");
	                return;
	            }

	            JsonObject payload = JsonParser
	                    .parseString(new String(Base64.getUrlDecoder().decode(token.split("\\.")[1]), "UTF-8"))
	                    .getAsJsonObject();
	            
	            // Find account
	            EmuFeralAccount acc = manager.getAccount(payload.get("uuid").getAsString());
	            if (acc == null) {
	                this.setResponseCode(401);
	                this.setResponseMessage("Access denied");
	                return;
	            }
	            
				String sourcePlayerID = acc.getAccountID();

				// log details
				if (System.getProperty("debugMode") != null) {
					System.out.println(
							"[API] [r/followers]  Client to server ( source: " + sourcePlayerID + " )");
				}

				// TODO: UPDATED AT FIELD.
				
				//open friend list
				FriendListManager.getInstance().openFriendList(sourcePlayerID);
				
				//get followers list
				var followerList = FriendListManager.getInstance().getFollowerList(sourcePlayerID);
				
				//make new json Array
				JsonArray jsonArray = new JsonArray();
				
				for(FriendListEntry entry : followerList)
				{
					//make a new json object for the entry
					var newJsonObject = new JsonObject();
					newJsonObject.addProperty("created_at", entry.addedAt);
					newJsonObject.addProperty("favorite", entry.favorite);
					
					SimpleDateFormat fmt = new SimpleDateFormat("YYYY-MM-dd'T'HH:mm:ss.'0Z'");
					fmt.setTimeZone(TimeZone.getTimeZone("UTC"));
					String updatedAt = fmt.format(new Date());
					
					newJsonObject.addProperty("updated_at", updatedAt);
					newJsonObject.addProperty("uuid", entry.playerID);
					
					jsonArray.add(newJsonObject);
				}
				
				//send response packet			
				setBody(jsonArray.toString());	
				
				// log interaction details
				if (System.getProperty("debugMode") != null) {
					System.out.println("[API] [r/followers] outbound: ( " + jsonArray.toString() + " )");
				}

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
				
	             // Verify expiry
	            JsonObject jwtPl = JsonParser
	                    .parseString(new String(Base64.getUrlDecoder().decode(token.split("\\.")[1]), "UTF-8"))
	                    .getAsJsonObject();
	            if (!jwtPl.has("exp") || jwtPl.get("exp").getAsLong() < System.currentTimeMillis() / 1000) {
	                this.setResponseCode(401);
	                this.setResponseMessage("Access denied");
	                return;
	            }

	            JsonObject payload = JsonParser
	                    .parseString(new String(Base64.getUrlDecoder().decode(token.split("\\.")[1]), "UTF-8"))
	                    .getAsJsonObject();
	            
	            // Find account
	            EmuFeralAccount acc = manager.getAccount(payload.get("uuid").getAsString());
	            if (acc == null) {
	                this.setResponseCode(401);
	                this.setResponseMessage("Access denied");
	                return;
	            }
	            
				String sourcePlayerID = acc.getAccountID();

				// log details
				if (System.getProperty("debugMode") != null) {
					System.out.println(
							"[API] [r/followings]  Client to server ( source: " + sourcePlayerID + " )");
				}
				
				//open friend list
				FriendListManager.getInstance().openFriendList(sourcePlayerID);
				
				//get following list
				var followingList = FriendListManager.getInstance().getFollowingList(sourcePlayerID);
				
				//make new json Array
				JsonArray jsonArray = new JsonArray();
				
				for(FriendListEntry entry : followingList)
				{
					//make a new json object for the entry
					var newJsonObject = new JsonObject();
					newJsonObject.addProperty("created_at", entry.addedAt);
					newJsonObject.addProperty("favorite", entry.favorite);
					
					SimpleDateFormat fmt = new SimpleDateFormat("YYYY-MM-dd'T'HH:mm:ss.'0Z'");
					fmt.setTimeZone(TimeZone.getTimeZone("UTC"));
					String updatedAt = fmt.format(new Date());
					
					newJsonObject.addProperty("updated_at", updatedAt);
					newJsonObject.addProperty("uuid", entry.playerID);
					
					jsonArray.add(newJsonObject);
				}
				
				//send response packet			
				setBody(jsonArray.toString());	
				
				// log interaction details
				if (System.getProperty("debugMode") != null) {
					System.out.println("[API] [r/followings] outbound: ( " + jsonArray.toString() + " )");
				}
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
			if (System.getProperty("debugMode") != null) {
				System.err.println("[FALLBACKAPI] ERROR : " + e.getMessage() + " )");
			}
			
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
