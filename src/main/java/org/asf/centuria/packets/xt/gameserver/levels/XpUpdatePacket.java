package org.asf.centuria.packets.xt.gameserver.levels;

import java.io.IOException;
import java.util.ArrayList;

import org.asf.centuria.data.XtReader;
import org.asf.centuria.data.XtWriter;
import org.asf.centuria.networking.smartfox.SmartfoxClient;
import org.asf.centuria.packets.xt.IXtPacket;

/**
 * 
 * XP Update packet
 * 
 * @author Sky Swimmer
 *
 */
public class XpUpdatePacket implements IXtPacket<XpUpdatePacket> {

	public String userId;
	public int totalXp;
	public int addedXp;

	public Level current;
	public Level previous;

	public ArrayList<CompletedLevel> completedLevels = new ArrayList<CompletedLevel>();

	public static class CompletedLevel {
		public int level;
		public int levelUpXp;

		public int levelUpRewardDefId;
		public int levelUpRewardQuantity;
		public String levelUpRewardGiftId;
	}

	public static class Level {
		public int level;
		public int xp;
		public int levelUpXp;
	}

	@Override
	public XpUpdatePacket instantiate() {
		return new XpUpdatePacket();
	}

	@Override
	public String id() {
		return "xpu";
	}

	@Override
	public void parse(XtReader reader) throws IOException {
	}

	@Override
	public void build(XtWriter writer) throws IOException {
		writer.writeInt(DATA_PREFIX); // data prefix

		// Basics
		writer.writeString(userId);
		writer.writeInt(totalXp);
		writer.writeInt(addedXp);

		// Current level
		writer.writeInt(current.level);
		writer.writeInt(current.xp);
		writer.writeInt(current.levelUpXp);

		// Last level
		writer.writeInt(previous.level);
		writer.writeInt(previous.xp);
		writer.writeInt(previous.levelUpXp);

		// Completed levels
		writer.writeInt(completedLevels.size());
		for (CompletedLevel lvl : completedLevels) {
			writer.writeInt(lvl.level);
			writer.writeInt(lvl.levelUpXp);

			writer.writeBoolean(true); // Not a clue- but its not a hasReward as without a reward, even with this
										// false, the level manager completely breaks down and rejects further packets

			writer.writeInt(lvl.levelUpRewardDefId);
			writer.writeInt(lvl.levelUpRewardQuantity);
			writer.writeString(lvl.levelUpRewardGiftId);
		}

		writer.writeString(DATA_SUFFIX);
	}

	@Override
	public boolean handle(SmartfoxClient client) throws IOException {
		return false;
	}

}
