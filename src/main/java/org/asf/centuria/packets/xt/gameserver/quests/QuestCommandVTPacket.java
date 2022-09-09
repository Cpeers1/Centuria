package org.asf.centuria.packets.xt.gameserver.quests;

import java.io.IOException;
import java.util.ArrayList;

import org.asf.centuria.data.XtReader;
import org.asf.centuria.data.XtWriter;
import org.asf.centuria.networking.smartfox.SmartfoxClient;
import org.asf.centuria.packets.xt.IXtPacket;

public class QuestCommandVTPacket implements IXtPacket<QuestCommandVTPacket> {

	public int type = 1;
	public String id = "";
	public ArrayList<String> params = new ArrayList<String>();

	@Override
	public String id() {
		return "qcmdVT";
	}

	@Override
	public void parse(XtReader reader) throws IOException {
	}

	@Override
	public void build(XtWriter writer) throws IOException {
		writer.writeInt(-1); // prefix
		writer.writeInt(type); // command type
		writer.writeString(id); // object id
		params.forEach(t -> writer.writeString(t)); // parameters
		writer.writeString(""); // data suffix
	}

	@Override
	public boolean handle(SmartfoxClient client) throws IOException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public QuestCommandVTPacket instantiate() {
		return new QuestCommandVTPacket();
	}

}
