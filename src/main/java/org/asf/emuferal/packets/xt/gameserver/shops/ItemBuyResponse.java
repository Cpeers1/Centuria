package org.asf.emuferal.packets.xt.gameserver.shops;

import java.io.IOException;
import java.util.ArrayList;

import org.asf.emuferal.data.XtReader;
import org.asf.emuferal.data.XtWriter;
import org.asf.emuferal.networking.smartfox.SmartfoxClient;
import org.asf.emuferal.packets.xt.IXtPacket;

public class ItemBuyResponse implements IXtPacket<ItemBuyResponse> {

	public ItemBuyStatus status;
	public ArrayList<BoughtItemInfo> items = new ArrayList<BoughtItemInfo>();
	public ArrayList<BoughtItemInfo> eurekaItems = new ArrayList<BoughtItemInfo>();

	public static enum ItemBuyStatus {

		SUCCESS(1), // successful purchase
		UNAVAILABLE(2), // item unavailable
		FULL_INVENTORY(3), // inventory full
		UNAFFORDABLE(4), // player cannot afford this
		LEVEL_LOCKED(6), // the player needs to be at a specific level to unlock this item
		UNKNOWN_ERROR(-1);

		private int status;

		private ItemBuyStatus(int status) {
			this.status = status;
		}

		public static ItemBuyStatus getByStatus(int status) {
			for (ItemBuyStatus state : ItemBuyStatus.values()) {
				if (state.status == status)
					return state;
			}
			return ItemBuyStatus.UNKNOWN_ERROR;
		}

		public int getStatus() {
			return status;
		}

	}

	public static class BoughtItemInfo {
		public String itemID;
		public int count;
	}

	@Override
	public ItemBuyResponse instantiate() {
		return new ItemBuyResponse();
	}

	@Override
	public String id() {
		return "$b";
	}

	@Override
	public void parse(XtReader reader) throws IOException {
	}

	@Override
	public void build(XtWriter writer) throws IOException {
		writer.writeInt(-1); // Data prefix

		writer.writeInt(status.getStatus());
		writer.writeInt(items.size());
		items.forEach(t -> {
			writer.writeString(t.itemID);
			writer.writeInt(t.count);
		});
		if (!eurekaItems.isEmpty()) {
			writer.writeInt(eurekaItems.size());
			eurekaItems.forEach(t -> {
				writer.writeString(t.itemID);
				writer.writeInt(t.count);
			});
		}

		writer.writeString(""); // Data suffix
	}

	@Override
	public boolean handle(SmartfoxClient client) throws IOException {
		return false;
	}

}
