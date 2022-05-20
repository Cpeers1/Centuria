package org.asf.emuferal.packets.xt.gameserver.shops;

import java.io.IOException;

import org.asf.emuferal.data.XtReader;
import org.asf.emuferal.data.XtWriter;
import org.asf.emuferal.networking.smartfox.SmartfoxClient;
import org.asf.emuferal.packets.xt.IXtPacket;

public class ShopList implements IXtPacket<ShopList> {

	private int shopType;
	private String[] items;

	@Override
	public ShopList instantiate() {
		return new ShopList();
	}

	@Override
	public String id() {
		return "$l";
	}

	@Override
	public void parse(XtReader reader) throws IOException {
		shopType = reader.readInt();
	}

	@Override
	public void build(XtWriter writer) throws IOException {
		writer.writeInt(-1); // Data prefix

		writer.writeInt(shopType);
		writer.writeInt(items.length);
		for (String itm : items)
			writer.writeString(itm);

		writer.writeString(""); // Data suffix
	}

	@Override
	public boolean handle(SmartfoxClient client) throws IOException {
		// Shop list

		// Send as much as possible
		// TODO: add more items
		ShopList resp = new ShopList();
		resp.shopType = shopType;
		resp.items = new String[] { "30198", "31878", "31876", "31877", "30554", "30555", "30556", "30557", "16939",
				"18662", "16938", "30816", "30818", "30817", "30819", "14867", "31655", "25793", "30766", "30184",
				"30767", "27780", "30769", "30770", "30768", "30771", "30772", "30773", "30774", "30775", "30776",
				"30781", "30777", "30782", "30778", "30783", "30779", "30784", "30780", "30785", "31832", "31831",
				"31992", "31993", "31994", "32041", "32034", "32033", "32143", "32148", "32144", "32149", "32147",
				"32140", "32145", "32146", "32158", "32141", "32151", "32154", "32156", "32152", "32157", "32142",
				"32153", "32150", "32155", "32159", "32160", "32161" };
		client.sendPacket(resp);
		resp = resp;

		return true;
	}

}
