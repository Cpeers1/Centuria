package org.asf.centuria.networking.http.api.custom;

import java.io.InputStream;
import java.net.Socket;
import java.util.HashMap;

import org.asf.centuria.Centuria;
import org.asf.centuria.entities.players.Player;
import org.asf.centuria.packets.xt.gameserver.inventory.InventoryItemDownloadPacket;
import org.asf.rats.processors.HttpUploadProcessor;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class ListPlayersHandler extends HttpUploadProcessor {
	@Override
	public void process(String contentType, Socket client, String method) {
		try {
			// Load spawn helper
			JsonObject helper = null;
			try {
				// Load helper
				InputStream strm = InventoryItemDownloadPacket.class.getClassLoader()
						.getResourceAsStream("spawns.json");
				helper = JsonParser.parseString(new String(strm.readAllBytes(), "UTF-8")).getAsJsonObject().get("Maps")
						.getAsJsonObject();
				strm.close();
			} catch (Exception e) {
			}

			// Send response
			JsonObject response = new JsonObject();
			HashMap<String, Integer> maps = new HashMap<String, Integer>();
			for (Player plr : Centuria.gameServer.getPlayers()) {
				if (!plr.roomReady)
					continue;
				String map = Integer.toString(plr.levelID);
				if (plr.levelID == 25280)
					map = "Tutorial";
				else if (helper.has(Integer.toString(plr.levelID)))
					map = helper.get(Integer.toString(plr.levelID)).getAsString();
				maps.put(map, maps.getOrDefault(map, 0) + 1);
			}
			response.addProperty("online", Centuria.gameServer.getPlayers().length);
			JsonObject mapData = new JsonObject();
			maps.forEach((k, v) -> mapData.addProperty(k, v));
			response.add("maps", mapData);
			setBody("text/json", response.toString());
			getResponse().setHeader("Access-Control-Allow-Origin", "https://aerialworks.ddns.net");
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
		return new ListPlayersHandler();
	}

	@Override
	public String path() {
		return "/centuria/listplayers";
	}

}
