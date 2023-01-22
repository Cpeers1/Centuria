package org.asf.centuria.networking.http.api.custom;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.util.Base64;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.asf.centuria.Centuria;
import org.asf.centuria.accounts.AccountManager;
import org.asf.centuria.accounts.CenturiaAccount;
import org.asf.centuria.accounts.PlayerInventory;
import org.asf.rats.processors.HttpUploadProcessor;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class PlayerDataDownloadHandler extends HttpUploadProcessor {
	@Override
	public void process(String contentType, Socket client, String method) {
		try {
			Centuria.logger.info("API CALL: " + getRequest().path);

			// Load manager
			AccountManager manager = AccountManager.getInstance();

			// Parse JWT payload
			String token = this.getHeader("Authorization").substring("Bearer ".length());
			if (token.isBlank()) {
				this.setResponseCode(403);
				this.setResponseMessage("Access denied");
				return;
			}

			// Parse token
			if (token.isBlank()) {
				this.setResponseCode(403);
				this.setResponseMessage("Access denied");
				return;
			}

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
			if (!jwtPl.has("exp") || jwtPl.get("exp").getAsLong() < System.currentTimeMillis() / 1000) {
				this.setResponseCode(403);
				this.setResponseMessage("Access denied");
				return;
			}

			JsonObject payload = JsonParser
					.parseString(new String(Base64.getUrlDecoder().decode(token.split("\\.")[1]), "UTF-8"))
					.getAsJsonObject();

			// Find account
			String id = payload.get("uuid").getAsString();

			// Check existence
			if (id == null) {
				// Invalid details
				this.setBody("{\"error\":\"invalid_account\"}");
				this.setResponseCode(422);
				return;
			}

			// Find account
			CenturiaAccount acc = manager.getAccount(id);
			if (acc == null) {
				this.setResponseCode(401);
				this.setResponseMessage("Access denied");
				return;
			}

			// Handle request
			String path = getRequest().path.substring(path().length());
			if (path.isEmpty()) {
				// Invalid request
				this.setBody("{\"error\":\"missing_item\"}");
				this.setResponseCode(400);
				this.setResponseMessage("Bad request");
				return;
			}
			path = path.substring(1);
			if (path.equals("full.zip")) {
				ByteArrayOutputStream strm = new ByteArrayOutputStream();
				try {
					ZipOutputStream invZip = new ZipOutputStream(strm);

					// Add all inventory objects
					addItemToZip(acc.getPlayerInventory(), "1", invZip);
					addItemToZip(acc.getPlayerInventory(), "10", invZip);
					addItemToZip(acc.getPlayerInventory(), "100", invZip);
					addItemToZip(acc.getPlayerInventory(), "102", invZip);
					addItemToZip(acc.getPlayerInventory(), "104", invZip);
					addItemToZip(acc.getPlayerInventory(), "105", invZip);
					addItemToZip(acc.getPlayerInventory(), "110", invZip);
					addItemToZip(acc.getPlayerInventory(), "111", invZip);
					addItemToZip(acc.getPlayerInventory(), "2", invZip);
					addItemToZip(acc.getPlayerInventory(), "201", invZip);
					addItemToZip(acc.getPlayerInventory(), "3", invZip);
					addItemToZip(acc.getPlayerInventory(), "300", invZip);
					addItemToZip(acc.getPlayerInventory(), "302", invZip);
					addItemToZip(acc.getPlayerInventory(), "303", invZip);
					addItemToZip(acc.getPlayerInventory(), "304", invZip);
					addItemToZip(acc.getPlayerInventory(), "311", invZip);
					addItemToZip(acc.getPlayerInventory(), "4", invZip);
					addItemToZip(acc.getPlayerInventory(), "400", invZip);
					addItemToZip(acc.getPlayerInventory(), "5", invZip);
					addItemToZip(acc.getPlayerInventory(), "6", invZip);
					addItemToZip(acc.getPlayerInventory(), "7", invZip);
					addItemToZip(acc.getPlayerInventory(), "8", invZip);
					addItemToZip(acc.getPlayerInventory(), "9", invZip);
					addItemToZip(acc.getPlayerInventory(), "avatars", invZip);
					addItemToZip(acc.getPlayerInventory(), "level", invZip);
					addItemToZip(acc.getPlayerInventory(), "savesettings", invZip);
					invZip.close();
					strm.close();
				} catch (IOException e) {
				}
				getResponse().body = new ByteArrayInputStream(strm.toByteArray());
				getResponse().setHeader("Content-Type", "application/zip", false);
				getResponse().setHeader("Content-Length", Integer.toString(strm.toByteArray().length), false);
				return;
			} else {
				if (!acc.getPlayerInventory().containsItem(path)) {
					// Invalid request
					this.setBody("{\"error\":\"item_not_found\"}");
					this.setResponseCode(404);
					this.setResponseMessage("Not found");
					return;
				} else {
					this.setBody("application/json", acc.getPlayerInventory().getItem(path).toString());
				}
			}
		} catch (Exception e) {
			setResponseCode(500);
			setResponseMessage("Internal Server Error");
			Centuria.logger.error(getRequest().path + " failed: 500: Internal Server Error", e);
		}
	}

	private static void addItemToZip(PlayerInventory inv, String item, ZipOutputStream zipStrm)
			throws UnsupportedEncodingException, IOException {
		if (inv.containsItem(item))
			transferDataToZip(zipStrm, item + ".json", inv.getItem(item).toString().getBytes("UTF-8"));
	}

	private static void transferDataToZip(ZipOutputStream zip, String file, byte[] data) throws IOException {
		zip.putNextEntry(new ZipEntry(file));
		zip.write(data);
		zip.closeEntry();
	}

	@Override
	public boolean supportsGet() {
		return true;
	}

	@Override
	public boolean supportsChildPaths() {
		return true;
	}

	@Override
	public HttpUploadProcessor createNewInstance() {
		return new PlayerDataDownloadHandler();
	}

	@Override
	public String path() {
		return "/centuria/playerdata";
	}

}
