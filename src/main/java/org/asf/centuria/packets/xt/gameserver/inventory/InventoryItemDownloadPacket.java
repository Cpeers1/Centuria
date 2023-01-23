package org.asf.centuria.packets.xt.gameserver.inventory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.UUID;

import org.apache.logging.log4j.MarkerManager;
import org.asf.centuria.Centuria;
import org.asf.centuria.accounts.InventoryManager;
import org.asf.centuria.accounts.PlayerInventory;
import org.asf.centuria.accounts.highlevel.ItemAccessor;
import org.asf.centuria.data.XtReader;
import org.asf.centuria.data.XtWriter;
import org.asf.centuria.entities.players.Player;
import org.asf.centuria.networking.smartfox.SmartfoxClient;
import org.asf.centuria.packets.xt.IXtPacket;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class InventoryItemDownloadPacket implements IXtPacket<InventoryItemDownloadPacket> {

	private static final String PACKET_ID = "ilt";

	private String slot = "";

	@Override
	public InventoryItemDownloadPacket instantiate() {
		return new InventoryItemDownloadPacket();
	}

	@Override
	public String id() {
		return PACKET_ID;
	}

	@Override
	public void parse(XtReader reader) {
		slot = reader.read();
	}

	@Override
	public void build(XtWriter writer) {
		writer.writeString(slot);
	}

	@Override
	public boolean handle(SmartfoxClient client) throws IOException {
		Player plr = (Player) client.container;
		PlayerInventory inv = plr.account.getSaveSpecificInventory();

		// Log
		Centuria.logger.debug(MarkerManager.getMarker("ITEMREQUEST"), "Client to server (type: " + slot + ")");

		// Check if inventory is built
		if (!inv.containsItem("1")) {
			InventoryManager.buildInventory(plr, inv);
		}

		if (slot.equals("304")) {
			// Interaction memory
			if (!inv.containsItem("304")) {
				// Create new object
				inv.setItem(slot, new JsonArray());

				// Add levels
				inv.getInteractionMemory().prepareLevel(820); // City Fera
				inv.getInteractionMemory().prepareLevel(2364); // Blood Tundra
				inv.getInteractionMemory().prepareLevel(9687); // Lakeroot
				inv.getInteractionMemory().prepareLevel(3273); // Sunken Thicket
				inv.getInteractionMemory().prepareLevel(2147); // Mugmyre
				inv.getInteractionMemory().prepareLevel(1825); // Shattered Bay
			}
		}

		if (slot.equals("400")) {
			// Override wings lock
			if (!inv.containsItem("400"))
				inv.setItem("400", new JsonArray());
			JsonArray itm = inv.getItem("400").getAsJsonArray();

			// Build entry
			JsonObject obj = new JsonObject();
			obj.addProperty("defId", 22441);
			JsonObject components = new JsonObject();
			JsonObject ts = new JsonObject();
			ts.addProperty("ts", System.currentTimeMillis());
			components.add("Timestamp", ts);
			obj.add("components", components);
			obj.addProperty("id", UUID.randomUUID().toString());
			obj.addProperty("type", 400);
			itm.add(obj);

			// Send the item to the client
			InventoryItemPacket pkt = new InventoryItemPacket();
			pkt.item = itm;
			client.sendPacket(pkt);

			return true;
		}

		if (slot.equals("1")) {
			if (inv.getSaveSettings().giveAllMods) {
				// All body mods
				String[] mods = new String[] { "9808", "9807", "9806", "9805", "9804", "9803", "9802", "9801", "9800",
						"9799", "9754", "9741", "9740", "9739", "9719", "9660", "9659", "9658", "9657", "9656", "7553",
						"7550", "7407", "7406", "7405", "7404", "7403", "7124", "7120", "5639", "5638", "5637", "5636",
						"5635", "5634", "5633", "5632", "5631", "5630", "5629", "5628", "5627", "5626", "5625", "5621",
						"5034", "5033", "5032", "5031", "5030", "5029", "3794", "3793", "3792", "3078", "3077", "3076",
						"3075", "3074", "3073", "3072", "3071", "3070", "3069", "3068", "3067", "30437", "30436",
						"30435", "30434", "30433", "30432", "30431", "30430", "30429", "30428", "29875", "29874",
						"29873", "29872", "29871", "29855", "28593", "28592", "28530", "28529", "28527", "28526",
						"28361", "28360", "28359", "28358", "28357", "28351", "27776", "27763", "27762", "27761",
						"27760", "27759", "27758", "27757", "27756", "27749", "2772", "2771", "2770", "2769", "2768",
						"2767", "2761", "2760", "26488", "26487", "26486", "26485", "26484", "26483", "26482", "26481",
						"26480", "26479", "26478", "26477", "26476", "26475", "26474", "26473", "26472", "26471",
						"25976", "25963", "25962", "25961", "25960", "25959", "25958", "25957", "25956", "25955",
						"25792", "25791", "25790", "25789", "25788", "25787", "25714", "25713", "25712", "25711",
						"25710", "25709", "25675", "25674", "25673", "25672", "25671", "25670", "25524", "25523",
						"25522", "25521", "25520", "25519", "24467", "24408", "24407", "24406", "24405", "24404",
						"23999", "23998", "23981", "23980", "23979", "23978", "23977", "23976", "23975", "23974",
						"23973", "23099", "23098", "23091", "23090", "23089", "23088", "23087", "23086", "23062",
						"23061", "23060", "23059", "23058", "23055", "23054", "23053", "23052", "23051", "22673",
						"22672", "22671", "22670", "22668", "22667", "22666", "22665", "22664", "22663", "22618",
						"22617", "22616", "22615", "22614", "22613", "22612", "22611", "21802", "21585", "2126", "2125",
						"2124", "2123", "2122", "21215", "21214", "21213", "2121", "21209", "2120", "2119", "2118",
						"2117", "2097", "2096", "2095", "2094", "2093", "2092", "2091", "2090", "2089", "2088", "2087",
						"2086", "2085", "2084", "2083", "20455", "20454", "20453", "20452", "20451", "20326", "20325",
						"20324", "20323", "20322", "19479", "19478", "19477", "19476", "19474", "19085", "19084",
						"19083", "19082", "19081", "19080", "19065", "19064", "19063", "19062", "18889", "18888",
						"18887", "18886", "18885", "18884", "18787", "17707", "17706", "17705", "17703", "17702",
						"17701", "16707", "15908", "15219", "15217", "15216", "15215", "15214", "14627", "14626",
						"14625", "14624", "14613", "14612", "14611", "13616", "13613", "12788", "12787", "12786",
						"12785", "12784", "12504", "12455", "12454", "12453", "12117", "12111", "10480", "10479",
						"10478", "10477", "10476" };

				for (String mod : mods) {
					inv.getAvatarAccessor().unlockAvatarPart(Integer.valueOf(mod));
				}

				// Save changes
				for (String change : inv.getAccessor().getItemsToSave())
					inv.setItem(change, inv.getItem(change));
				inv.getAccessor().completedSave();
			}
		}

		// Clothing and dyes
		if (inv.getSaveSettings().giveAllClothes) {
			if (slot.equals("111")) {
				// Give all dyes (80 of each)
				String[] dyes = new String[] { "6255", "6254", "6253", "6252", "6251", "435", "434", "433", "432",
						"431", "430", "429", "428", "427", "426", "369", "368", "31306", "31304", "31303", "31302",
						"31301", "31300", "31299", "31298", "31297", "31288", "30334", "30333", "30332", "297", "28805",
						"28804", "28372", "28371", "28370", "28369", "28368", "28117", "28116", "28115", "28114",
						"27828", "27827", "2704", "2702", "26397", "24149", "24148", "24147", "24146", "24145", "24144",
						"22602", "22601", "17543", "17542", "17541", "17540", "17539", "17538", "17537", "17536",
						"17535", "17534", "17533", "17532", "17531", "17530", "17529", "17528", "10531", "10190",
						"10189", "10188", "10187", "10186", "10185", "10184", "10183", "10182", "10181", "10180",
						"10179", "10178", "10177", "10176", "10175", "10174", "10173", "10172", "10171", "10170",
						"10169", "10168", "10167", "10166", "10165", "10164", "10162", "10161", "10160", "10159",
						"10158", "10157", "10156", "10155", "10154", "10153", "10152", "10151", "10150", "10149",
						"10148", "10147", "10146", "10145", "10144", "10143", "10142", "10141", "10140", "10139",
						"10138", "10137", "10136", "10135", "10134", "10133", "10132", "10131", "10130", "10129",
						"10127" };

				for (String dye : dyes) {
					// Add 80
					if (!inv.getDyeAccessor().hasDye(Integer.valueOf(dye))) {
						for (int i = 0; i < 80; i++) {
							inv.getDyeAccessor().addDye(Integer.valueOf(dye));
						}
					}
				}
			}

			if (slot.equals("100")) {
				// Scan clothinghelper and give all clothes
				try {
					// Load helper
					InputStream strm = InventoryItemDownloadPacket.class.getClassLoader()
							.getResourceAsStream("defaultitems/clothinghelper.json");
					JsonObject helper = JsonParser.parseString(new String(strm.readAllBytes(), "UTF-8"))
							.getAsJsonObject().get("Clothing").getAsJsonObject();
					strm.close();

					// Add all clothes (3 of each)
					for (String id : helper.keySet()) {
						if (inv.getClothingAccessor().getClothingCount(Integer.valueOf(id)) < 3) {
							for (int i = inv.getClothingAccessor().getClothingCount(Integer.valueOf(id)); i < 3; i++) {
								inv.getClothingAccessor().addClothing(Integer.valueOf(id), false);
							}
						}
					}
				} catch (IOException e) {
					throw new RuntimeException(e);
				}

				// Save changes
				for (String change : inv.getAccessor().getItemsToSave())
					inv.setItem(change, inv.getItem(change));
				inv.getAccessor().completedSave();
			}
		}

		// Resources
		if (slot.equals("103") && inv.getSaveSettings().giveAllResources) {
			// Give all resources
			String[] ids = ItemAccessor.getItemDefinitionsIn(slot);
			for (String id : ids) {
				int c = inv.getItemAccessor(plr).getCountOfItem(Integer.valueOf(id));
				if (c < 10) {
					inv.getItemAccessor(plr).add(Integer.valueOf(id), 10 - c);
				}
			}
		}

		// Furniture
		if (slot.equals("102") && inv.getSaveSettings().giveAllFurnitureItems) {
			// Scan furniturehelper and give all furniture
			try {
				// Load helper
				InputStream strm = InventoryItemDownloadPacket.class.getClassLoader()
						.getResourceAsStream("defaultitems/furniturehelper.json");
				JsonObject helper = JsonParser.parseString(new String(strm.readAllBytes(), "UTF-8")).getAsJsonObject()
						.get("Furniture").getAsJsonObject();
				strm.close();

				// Add all furniture (6 of each)
				for (String id : helper.keySet()) {
					if (inv.getFurnitureAccessor().getFurnitureCount(Integer.valueOf(id)) < 6) {
						for (int i = inv.getFurnitureAccessor().getFurnitureCount(Integer.valueOf(id)); i < 6; i++) {
							inv.getFurnitureAccessor().addFurniture(Integer.valueOf(id), false);
						}
					}
				}
			} catch (IOException e) {
			}

			// Save changes
			for (String change : inv.getAccessor().getItemsToSave())
				inv.setItem(change, inv.getItem(change));
			inv.getAccessor().completedSave();
		}

		// Emotes
		if (slot.equals("9")) {
			JsonArray item = new JsonArray();

			// Add all emotes
			InventoryManager.addEmote(item, "9122");
			InventoryManager.addEmote(item, "9151");
			InventoryManager.addEmote(item, "9108");
			InventoryManager.addEmote(item, "9121");
			InventoryManager.addEmote(item, "9143");
			InventoryManager.addEmote(item, "9190");
			InventoryManager.addEmote(item, "8930");
			InventoryManager.addEmote(item, "9116");

			// Send the item to the client
			InventoryItemPacket pkt = new InventoryItemPacket();
			pkt.item = item;
			client.sendPacket(pkt);

			return true;
		}

		// Sanctuaries
		if (slot.equals("5") || slot.equals("6") || slot.equals("102") || slot.equals("10") || slot.equals("201")) {
			if (!plr.sanctuaryPreloadCompleted) {
				// Fix missing sancs
				if (!inv.containsItem("201")) {
					inv.deleteItem("10");
					inv.deleteItem("5");
					inv.deleteItem("6");
				}

				// Looks
				if (inv.getSaveSettings().giveAllSanctuaryTypes) {
					// Give missing sanctuary types
					int[] sanctuaryTypes = new int[] { 9588, 12632, 12637, 12964, 21273, 23627, 24122, 25414, 26065,
							28431, 9760, 9764 };
					for (int id : sanctuaryTypes)
						if (!inv.getSanctuaryAccessor().isSanctuaryUnlocked(id))
							inv.getSanctuaryAccessor().unlockSanctuary(id);
				} else {
					// Give default sanctuary if needed
					if (!inv.getSanctuaryAccessor().isSanctuaryUnlocked(9588))
						inv.getSanctuaryAccessor().unlockSanctuary(9588);
				}

				// Check look count and add missing look slots
				for (int i = inv.getSanctuaryAccessor().getSanctuaryLookCount(); i < 12; i++)
					inv.getSanctuaryAccessor().addExtraSanctuarySlot();

				// Active sanc look
				if (plr.account.getSaveSpecificInventory().getSanctuaryAccessor()
						.getSanctuaryLook(plr.account.getActiveSanctuaryLook()) == null) {
					plr.activeSanctuaryLook = inv.getSanctuaryAccessor().getFirstSanctuaryLook().get("id")
							.getAsString();
					plr.account.setActiveSanctuaryLook(plr.activeSanctuaryLook);
				} else
					plr.activeSanctuaryLook = plr.account.getActiveSanctuaryLook();

				// Complete
				plr.sanctuaryPreloadCompleted = true;
			}
		}

		// Load the item
		JsonElement item = inv.getItem(slot.equals("200") ? "avatars" : slot);

		// Repair broken avatars
		if (slot.equals("200")) {
			InventoryManager.fixBrokenAvatars(item.getAsJsonArray(), inv);
		}

		// Currency
		if (slot.equals("104")) {
			JsonArray itm;
			boolean changed = inv.containsItem(slot);
			if (changed)
				itm = inv.getItem(slot).getAsJsonArray();
			else
				itm = new JsonArray();

			if (!inv.getAccessor().hasInventoryObject(slot, 2327)) {
				// Likes

				// Build entry
				JsonObject obj = new JsonObject();
				obj.addProperty("defId", 2327);
				JsonObject components = new JsonObject();
				JsonObject quantity = new JsonObject();
				quantity.addProperty("quantity", (inv.getSaveSettings().giveAllCurrency ? 10000 : 2500));
				components.add("Quantity", quantity);
				JsonObject trade = new JsonObject();
				trade.addProperty("isInTradeList", false);
				components.add("Tradable", trade);
				obj.add("components", components);
				obj.addProperty("id", UUID.randomUUID().toString());
				obj.addProperty("type", 104);

				// Add entry
				itm.add(obj);
				changed = true;
			}

			if (!inv.getAccessor().hasInventoryObject(slot, 14500)) {
				// Star fragments

				// Build entry
				JsonObject obj = new JsonObject();
				obj.addProperty("defId", 14500);
				JsonObject components = new JsonObject();
				JsonObject quantity = new JsonObject();
				quantity.addProperty("quantity", (inv.getSaveSettings().giveAllCurrency ? 10000 : 0));
				components.add("Quantity", quantity);
				JsonObject trade = new JsonObject();
				trade.addProperty("isInTradeList", false);
				components.add("Tradable", trade);
				obj.add("components", components);
				obj.addProperty("id", UUID.randomUUID().toString());
				obj.addProperty("type", 104);

				// Add entry
				itm.add(obj);
				changed = true;
			}

			if (!inv.getAccessor().hasInventoryObject(slot, 8372)) {
				// Lockpicks

				// Build entry
				JsonObject obj = new JsonObject();
				obj.addProperty("defId", 8372);
				JsonObject components = new JsonObject();
				JsonObject quantity = new JsonObject();
				quantity.addProperty("quantity", 0);
				components.add("Quantity", quantity);
				JsonObject trade = new JsonObject();
				trade.addProperty("isInTradeList", false);
				components.add("Tradable", trade);
				obj.add("components", components);
				obj.addProperty("id", UUID.randomUUID().toString());
				obj.addProperty("type", 104);

				// Add entry
				itm.add(obj);
				changed = true;
			}

			// Check trade tags
			for (JsonElement ele : itm) {
				JsonObject obj = ele.getAsJsonObject();
				JsonObject components = obj.get("components").getAsJsonObject();
				if (!components.has("Tradable")) {
					JsonObject trade = new JsonObject();
					trade.addProperty("isInTradeList", false);
					components.add("Tradable", trade);
					changed = true;
				}
			}

			// Creative mode
			if (inv.getSaveSettings().giveAllCurrency) {
				// Add 10k if 0
				if (inv.getCurrencyAccessor().getLikes() <= 100) {
					inv.getCurrencyAccessor().setLikesDirectly(10000);
				}
				if (inv.getCurrencyAccessor().getStarFragments() <= 100) {
					inv.getCurrencyAccessor().setStarFragmentsDirectly(10000);
				}
			}

			if (changed) {
				// Save item
				item = itm;
				inv.setItem(slot, item);
			}
		}

		// Quest progression items
		if (slot.equals("311")) {
			if (inv.getItem("311") == null || inv.getItem("311").getAsJsonArray().isEmpty()) {
				JsonArray itm;
				if (inv.containsItem("311"))
					itm = inv.getItem("311").getAsJsonArray();
				else {
					itm = new JsonArray();

					// Build entry
					JsonObject obj = new JsonObject();
					obj.addProperty("defId", 22781);
					JsonObject components = new JsonObject();
					JsonObject questObject = new JsonObject();
					questObject.add("completedQuests", new JsonArray());
					components.add("SocialExpanseLinearGenericQuestsCompletion", questObject);
					obj.add("components", components);
					obj.addProperty("id", UUID.randomUUID().toString());
					obj.addProperty("type", 311);
					itm.add(obj);
					item = itm;
				}

				// Save item
				inv.setItem(slot, item);
			} else {
				// Fix broken quest progression
				JsonObject progressionMap = plr.account.getSaveSpecificInventory().getAccessor()
						.findInventoryObject("311", 22781).get("components").getAsJsonObject()
						.get("SocialExpanseLinearGenericQuestsCompletion").getAsJsonObject();
				JsonArray arr = progressionMap.get("completedQuests").getAsJsonArray();
				ArrayList<String> completedQuests = new ArrayList<String>();
				arr.forEach(t -> completedQuests.add(t.getAsString()));
				if (completedQuests.contains("25287")) {
					// Fix
					arr.remove(completedQuests.indexOf("25287"));
					plr.account.getSaveSpecificInventory().setItem("311",
							plr.account.getSaveSpecificInventory().getItem("311"));
					item = inv.getItem("311");
					inv.setItem(slot, item);
				}
			}
		}

		// PlayerVars
		if (slot.equals("303")) {
			if (inv.getItem("303") == null || inv.getItem("303").getAsJsonArray().isEmpty()) {
				JsonArray itm;
				if (inv.containsItem("303"))
					itm = inv.getItem("303").getAsJsonArray();
				else
					itm = new JsonArray();

				// Build entry
				item = itm;

				// Save item
				inv.setItem(slot, item);
			}

			// Set defaults
			// inv.getUserVarAccesor().setDefaultPlayerVarValues();
		}

		// PlayerVars
		if (slot.equals("110")) {
			if (inv.getItem("110") == null || inv.getItem("110").getAsJsonArray().isEmpty()) {
				JsonArray itm;
				if (inv.containsItem("110"))
					itm = inv.getItem("110").getAsJsonArray();
				else
					itm = new JsonArray();

				// Build entry
				item = itm;

				// Save item
				inv.setItem(slot, item);
			}

			// Set defaults
			inv.getTwiggleAccesor().giveDefaultTwiggles();
		}

		// Inspirations
		if (slot.equals("8")) {
			if (inv.getItem("8") == null || inv.getItem("8").getAsJsonArray().isEmpty()) {
				JsonArray itm;

				if (inv.containsItem("8"))
					itm = inv.getItem("8").getAsJsonArray();
				else
					itm = new JsonArray();

				// Build entry

				item = itm;

				// Save item
				inv.setItem(slot, item);

				// Set default inspirations

				inv.getInspirationAccessor().giveDefaultInspirations();

				// Reload inv
				itm = inv.getItem("8").getAsJsonArray();

				item = itm;
			}
		}

		// Send the item to the client
		if (item == null)
			item = new JsonArray();

		// Remove invalid items
		if (item instanceof JsonArray) {
			for (JsonElement itm : ((JsonArray) item).deepCopy()) {
				if (itm.isJsonObject() && itm.getAsJsonObject().has("type")
						&& !itm.getAsJsonObject().get("type").getAsString().equals(slot)) {
					((JsonArray) item).remove(itm);
				}
			}
		}

		InventoryItemPacket pkt = new InventoryItemPacket();
		pkt.item = item;
		client.sendPacket(pkt);

		return true;
	}

}
