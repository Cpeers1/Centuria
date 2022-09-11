package org.asf.centuria.packets.xt.gameserver.quests;

import java.io.IOException;

import org.asf.centuria.data.XtReader;
import org.asf.centuria.data.XtWriter;
import org.asf.centuria.networking.smartfox.SmartfoxClient;
import org.asf.centuria.packets.xt.IXtPacket;

public class QuestGenericLinearQuestCompletePacket implements IXtPacket<QuestGenericLinearQuestCompletePacket> {

	public int questID = 0;

	@Override
	public String id() {
		return "qsxqc";
	}

	@Override
	public void parse(XtReader reader) throws IOException {
	}

	@Override
	public void build(XtWriter writer) throws IOException {
		writer.writeInt(-1); // prefix
		writer.writeInt(questID); // quest ID
		writer.writeString(""); // data suffix
	}

	@Override
	public boolean handle(SmartfoxClient client) throws IOException {
		return false;
	}

	@Override
	public QuestGenericLinearQuestCompletePacket instantiate() {
		return new QuestGenericLinearQuestCompletePacket();
	}

}
