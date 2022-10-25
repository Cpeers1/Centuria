package org.asf.centuria.minigames.games;

import org.asf.centuria.data.XtReader;
import org.asf.centuria.entities.players.Player;
import org.asf.centuria.minigames.AbstractMinigame;
import org.asf.centuria.minigames.MinigameMessage;
import org.asf.centuria.packets.xt.gameserver.minigame.MinigameCurrencyPacket;
import org.asf.centuria.packets.xt.gameserver.minigame.MinigameMessagePacket;
import org.asf.centuria.packets.xt.gameserver.minigame.MinigamePrizePacket;;

public class GameTwiggleBuilders extends AbstractMinigame {

	@Override
	public boolean canHandle(int levelID) {
		return levelID == 4111;
	}

	@Override
	public void onJoin(Player plr) {
		MinigameCurrencyPacket currency = new MinigameCurrencyPacket();
		currency.Currency = 9514;
		plr.client.sendPacket(currency);
	}

	@MinigameMessage("startLevel")
	public void startLevel(Player plr, XtReader rd) {
		MinigameMessagePacket msg = new MinigameMessagePacket();
		msg.command = "startLevel";
		int level = rd.readInt();

		// TODO: mechanics

		msg.data = Integer.toString(level);
		plr.client.sendPacket(msg);
	}
	
	@MinigameMessage("requestResults")
	public void requestResults(Player plr, XtReader rd) {
		// TODO: mechanics
		givePrize(plr);		
	}

	@MinigameMessage("endLevel")
	public void endLevel(Player plr, XtReader rd) {
		MinigameMessagePacket msg = new MinigameMessagePacket();
		msg.command = "endLevel";

		msg.data = "30";
		plr.client.sendPacket(msg);
	}

	public void givePrize(Player plr) {
		MinigamePrizePacket prize = new MinigamePrizePacket();
		prize.itemDefId = "2327";
		prize.itemCount = 15;
		prize.given = true;
		prize.prizeIndex1 = 6566;
		prize.prizeIndex2 = 0;
		plr.client.sendPacket(prize);

		MinigamePrizePacket prize1 = new MinigamePrizePacket();
		prize1.itemDefId = "2327";
		prize1.itemCount = 15;
		prize1.given = true;
		prize1.prizeIndex1 = 6567;
		prize1.prizeIndex2 = 1;
		plr.client.sendPacket(prize1);

		MinigamePrizePacket prize2 = new MinigamePrizePacket();
		prize2.itemDefId = "2327";
		prize2.itemCount = 15;
		prize2.given = true;
		prize2.prizeIndex1 = 6572;
		prize2.prizeIndex2 = 2;
		plr.client.sendPacket(prize2);
	}

	@Override
	public AbstractMinigame instantiate() {
		return new GameTwiggleBuilders();
	}

}
