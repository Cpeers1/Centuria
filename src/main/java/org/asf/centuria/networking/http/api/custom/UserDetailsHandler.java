package org.asf.centuria.networking.http.api.custom;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.Base64;

import org.asf.centuria.Centuria;
import org.asf.centuria.accounts.AccountManager;
import org.asf.centuria.accounts.CenturiaAccount;
import org.asf.centuria.accounts.SaveMode;
import org.asf.centuria.packets.xt.gameserver.inventory.InventoryItemDownloadPacket;
import org.asf.rats.ConnectiveHTTPServer;
import org.asf.rats.processors.HttpUploadProcessor;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class UserDetailsHandler extends HttpUploadProcessor {
	@Override
	public void process(String contentType, Socket client, String method) {
		try {
			if (!getRequest().method.equalsIgnoreCase("post")) {
				this.setResponseCode(400);
				this.setResponseMessage("Bad request");
				return;
			}

			// Parse body
			ByteArrayOutputStream strm = new ByteArrayOutputStream();
			ConnectiveHTTPServer.transferRequestBody(getHeaders(), getRequestBodyStream(), strm);
			byte[] body = strm.toByteArray();
			strm.close();

			// Load manager
			AccountManager manager = AccountManager.getInstance();

			// Parse body
			JsonObject request = JsonParser.parseString(new String(body, "UTF-8")).getAsJsonObject();
			if (!request.has("id") && !request.has("name")) {
				this.setResponseCode(400);
				this.setResponseMessage("Bad request");
				return;
			}
			String id = null;
			if (request.has("id"))
				id = request.get("id").getAsString();
			else
				id = manager.getUserByDisplayName(request.get("name").getAsString());

			// Find account
			if (id == null) {
				this.setResponseCode(404);
				this.setResponseMessage("Account not found");
				return;
			}
			CenturiaAccount acc = manager.getAccount(id);
			if (acc == null) {
				this.setResponseCode(404);
				this.setResponseMessage("Account not found");
				return;
			}

			// Send response
			JsonObject response = new JsonObject();
			response.addProperty("uuid", id);
			response.addProperty("display_name", acc.getDisplayName());
			response.addProperty("current_level", acc.getLevel().getLevel());

			// Sensitive fields
			// Check authorization
			boolean isSelf = false;
			if (getRequest().headers.containsKey("Authorization")) {
				// Parse JWT payload
				String token = this.getHeader("Authorization").substring("Bearer ".length());
				if (!token.isBlank()) {
					// Verify signature
					String verifyD = token.split("\\.")[0] + "." + token.split("\\.")[1];
					String sig = token.split("\\.")[2];
					if (!Centuria.verify(verifyD.getBytes("UTF-8"), Base64.getUrlDecoder().decode(sig))) {
						this.setResponseCode(403);
						this.setResponseMessage("Access denied");
						return;
					}

					// Verify expiry
					JsonObject jwtPl = JsonParser
							.parseString(new String(Base64.getUrlDecoder().decode(token.split("\\.")[1]), "UTF-8"))
							.getAsJsonObject();
					if (jwtPl.has("exp") && jwtPl.get("exp").getAsLong() >= System.currentTimeMillis() / 1000) {
						JsonObject payload = JsonParser
								.parseString(new String(Base64.getUrlDecoder().decode(token.split("\\.")[1]), "UTF-8"))
								.getAsJsonObject();

						// Find account
						String accId = payload.get("uuid").getAsString();
						if (accId.equals(id))
							isSelf = true;
					}
				}
			}
			if (isSelf) {
				String species = "Kitsune";
				// Find current species
				String lookID = acc.getActiveLook();
				if (acc.getSaveSpecificInventory().containsItem("avatars")) {
					// Find avatar
					JsonArray looks = acc.getSaveSpecificInventory().getItem("avatars").getAsJsonArray();
					for (JsonElement lookEle : looks) {
						JsonObject look = lookEle.getAsJsonObject();
						if (look.get("id").getAsString().equals(lookID)) {
							// Found the avatar, lets find the species
							String defId = look.get("defId").getAsString();

							// Load avatar helper
							try {
								InputStream strm2 = InventoryItemDownloadPacket.class.getClassLoader()
										.getResourceAsStream("defaultitems/avatarhelper.json");
								JsonObject helper = JsonParser.parseString(new String(strm2.readAllBytes(), "UTF-8"))
										.getAsJsonObject().get("Avatars").getAsJsonObject();
								strm2.close();
								for (String aSpecies : helper.keySet()) {
									String aDefID = helper.get(aSpecies).getAsJsonObject().get("defId").getAsString();
									if (aDefID.equals(defId)) {
										// Found the species
										species = aSpecies;
										break;
									}
								}
							} catch (IOException e) {
							}
							break;
						}
					}
				}

				response.addProperty("avatar_species", species);
				response.addProperty("last_login_time", acc.getLastLoginTime());
				response.addProperty("tutorial_completed", !acc.isPlayerNew());
				response.addProperty("is_online", acc.getOnlinePlayerInstance() != null);
				response.addProperty("save_mode", acc.getSaveMode().toString());
				if (acc.isMuted() || acc.isBanned()) {
					// Include penalty information
					JsonObject penaltyObj = new JsonObject();
					penaltyObj.addProperty("type", acc.isMuted() ? "mute" : "ban");
					response.add("penalty", penaltyObj);

					// Check temporary ban/mute
					JsonObject penalty = acc.getSaveSharedInventory().getItem("penalty").getAsJsonObject();
					if (acc.isMuted() && penalty.get("unmuteTimestamp").getAsLong() != -1) {
						penaltyObj.addProperty("is_temporary", true);
						penaltyObj.addProperty("penalty_end_time", penalty.get("unmuteTimestamp").getAsLong());
					} else if (acc.isBanned() && penalty.get("unbanTimestamp").getAsLong() != -1) {
						penaltyObj.addProperty("is_temporary", true);
						penaltyObj.addProperty("penalty_end_time", penalty.get("unbanTimestamp").getAsLong());
					} else {
						penaltyObj.addProperty("isTemporary", false);
					}

					// Add reason
					if (penalty.has("reason")) {
						penaltyObj.addProperty("has_reason", true);
						penaltyObj.addProperty("reason", penalty.get("reason").getAsString());
					} else
						penaltyObj.addProperty("has_reason", false);
				}
				if (acc.getSaveMode() == SaveMode.MANAGED)
					response.addProperty("active_save", acc.getSaveManager().getCurrentActiveSave());
				response.add("current_save_settings", acc.getSaveSpecificInventory().getSaveSettings().writeToObject());
			}
			JsonObject obj = acc.getSaveSpecificInventory().getAccessor().findInventoryObject("avatars",
					acc.getActiveLook());
			if (obj != null)
				response.add("active_look",
						obj.get("components").getAsJsonObject().get("AvatarLook").getAsJsonObject());
			setBody("text/json", response.toString());
		} catch (Exception e) {
			setResponseCode(500);
			setResponseMessage("Internal Server Error");
			Centuria.logger.error(getRequest().path + " failed: 500: Internal Server Error", e);
		}
	}

	@Override
	public boolean supportsGet() {
		return true;
	}

	@Override
	public HttpUploadProcessor createNewInstance() {
		return new UserDetailsHandler();
	}

	@Override
	public String path() {
		return "/centuria/getuser";
	}

}
