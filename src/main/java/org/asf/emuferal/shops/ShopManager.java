package org.asf.emuferal.shops;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

import org.asf.emuferal.accounts.EmuFeralAccount;
import org.asf.emuferal.modules.IEmuFeralModule;
import org.asf.emuferal.modules.ModuleManager;
import org.asf.emuferal.shops.info.ShopInfo;
import org.asf.emuferal.shops.info.ShopItem;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class ShopManager {

	private static HashMap<String, ShopInfo> shops = new HashMap<String, ShopInfo>();

	private static void loadShopData(JsonObject shopsJson) {
		shopsJson.keySet().forEach((shopId) -> {
			JsonObject shopObject = shopsJson.get(shopId).getAsJsonObject();

			// Load into object
			ShopInfo shop;
			if (!shops.containsKey(shopId)) {
				// Create
				shop = new ShopInfo();

				// Save in memory
				shops.put(shopId, shop);
			} else {
				// Transform
				shop = shops.get(shopId);

				// Clear old content list if needed
				if (shopObject.has("contents"))
					shop.contents.clear();
			}

			// Load object name if present
			if (shopObject.has("object"))
				shop.object = shopObject.get("object").getAsString();

			// Load stock info if present
			if (shopObject.has("restockTime"))
				shop.restockTime = shopObject.get("restockTime").getAsInt();

			// Load content if present
			if (shopObject.has("contents")) {
				JsonObject contents = shopObject.get("contents").getAsJsonObject();
				contents.keySet().forEach((sId) -> {
					JsonObject item = contents.get(sId).getAsJsonObject();

					// Build object
					ShopItem itm = new ShopItem();
					itm.objectName = item.get("object").getAsString();

					// Load stock info
					itm.stock = item.get("stock").getAsInt();

					// Load costs
					JsonObject costs = item.get("cost").getAsJsonObject();
					costs.keySet().forEach((id) -> itm.cost.put(id, costs.get(id).getAsInt()));

					// Load items
					JsonObject items = item.get("items").getAsJsonObject();
					items.keySet().forEach((id) -> itm.items.put(id, items.get(id).getAsInt()));

					// Add object
					shop.contents.put(sId, itm);
				});
			}
		});
	}

	static {
		try {
			// Load the helper
			InputStream strm = ShopManager.class.getClassLoader().getResourceAsStream("shops.json");
			JsonObject helper = JsonParser.parseString(new String(strm.readAllBytes(), "UTF-8")).getAsJsonObject()
					.get("Shops").getAsJsonObject();
			strm.close();

			// Load the shops into memory
			loadShopData(helper);

			// Load transformers
			loadTransformers(ShopManager.class);

			// Load module transformers
			for (IEmuFeralModule module : ModuleManager.getInstance().getAllModules()) {
				loadTransformers(module.getClass());
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private static void loadTransformers(Class<?> cls) {
		URL source = cls.getProtectionDomain().getCodeSource().getLocation();

		// Generate a base URL
		String baseURL = "";
		String fileName = "";
		try {
			File sourceFile = new File(source.toURI());
			fileName = sourceFile.getName();
			if (sourceFile.isDirectory()) {
				baseURL = source + (source.toString().endsWith("/") ? "" : "/");
			} else {
				baseURL = "jar:" + source + "!/";
			}
		} catch (Exception e) {
			return;
		}

		try {
			// Find the transformer document
			InputStream strm = new URL(baseURL + "shoptransformers/index.json").openStream();
			JsonArray index = JsonParser.parseString(new String(strm.readAllBytes(), "UTF-8")).getAsJsonArray();
			strm.close();

			// Load all transformers
			for (JsonElement ele : index) {
				try {
					// Find the transformer document
					strm = new URL(baseURL + "shoptransformers/" + ele.getAsString()).openStream();
					JsonObject transformer = JsonParser.parseString(new String(strm.readAllBytes(), "UTF-8"))
							.getAsJsonObject();
					strm.close();

					// Load transformer
					loadShopData(transformer);
				} catch (Exception e) {
					System.err.println("Transformer failed to load: " + ele.getAsString() + " (" + fileName + "): "
							+ e.getClass().getSimpleName() + (e.getMessage() != null ? ": " + e.getMessage() : ""));
					e.printStackTrace();
				}
			}
		} catch (Exception e) {
		}
	}

	/**
	 * Retrieves the amount of times a player purchased a specific item
	 * 
	 * @param player Player to check the purchase count of
	 * @param shopId Shop ID
	 * @param itemId Item defID
	 * @return Amount of purchases a player made of a specific item, this resets if
	 *         the shop in question restocks
	 */
	public static int getPurchaseCount(EmuFeralAccount player, String shopId, String itemId) {
		// Check inventory object
		if (!player.getPlayerInventory().containsItem("purchaselog") || !shops.containsKey(shopId))
			return 0;

		// Find item
		JsonObject log = player.getPlayerInventory().getItem("purchaselog").getAsJsonObject();
		if (log.has(shopId)) {
			JsonObject shopLog = log.get(shopId).getAsJsonObject();

			// Check restock
			int restock = shops.get(shopId).restockTime;
			if (restock != -1) {
				// Check restock timestamp
				long activationTime = shopLog.get("activationTime").getAsLong();
				if ((activationTime + (long) (restock * 1000)) < System.currentTimeMillis()) {
					// Restock

					// Delete the purchases from memory
					log.remove(shopId);

					// Save data
					player.getPlayerInventory().setItem("purchaselog", log);

					// Return 0 as the shop has been restocked
					return 0;
				}
			}

			// Find item
			shopLog = shopLog.get("items").getAsJsonObject();
			if (shopLog.has(itemId)) {
				return shopLog.get(itemId).getAsInt();
			}
		}

		// Not found
		return 0;
	}

	/**
	 * Retrieves the contents of a specific shop
	 * 
	 * @param player Player to create the list for
	 * @param shopId Shop content defID
	 * @return Array of items in the shop
	 */
	public static String[] getShopContents(EmuFeralAccount player, String shopId) {
		if (!shops.containsKey(shopId))
			return new String[0];

		// Store in memory for easy access
		ShopInfo shop = shops.get(shopId);

		// Create list of items
		ArrayList<String> items = new ArrayList<String>();
		shop.contents.forEach((id, item) -> {
			if (item.stock != -1) {
				// Check stock
				int purchaseCount = getPurchaseCount(player, shopId, id);
				if (purchaseCount > item.stock) {
					// Out of stock
					return;
				}
			}

			// Add item
			items.add(id);
		});

		return items.toArray(t -> new String[t]);
	}

	/**
	 * Checks if the given ID is a valid shop ID
	 * 
	 * @param id ID to check
	 * @return True if the ID is a valid shop ID, false otherwise
	 */
	public static boolean isShop(String id) {
		return shops.containsKey(id);
	}

}
