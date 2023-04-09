package org.asf.centuria.minigames.games;

import java.util.UUID;

import org.asf.centuria.data.XtReader;
import org.asf.centuria.data.XtWriter;
import org.asf.centuria.entities.players.Player;
import org.asf.centuria.minigames.AbstractMinigame;
import org.asf.centuria.minigames.MinigameMessage;
import org.asf.centuria.packets.xt.gameserver.minigame.MinigameMessagePacket;

public class GameDizzywingDispatch extends AbstractMinigame {

    @Override
	public AbstractMinigame instantiate() {
		return new GameDizzywingDispatch();
	}

    @Override
	public boolean canHandle(int levelID) {
		return levelID == 8192;
	}

    @Override
	public void onJoin(Player plr) {
	}

    @Override
	public void onExit(Player plr) {
	}

    @MinigameMessage("startGame")
	public void startGame(Player player, XtReader rd) {

        XtWriter mmData = new XtWriter();
        mmData.writeString(UUID.randomUUID().toString());
        mmData.writeLong(System.currentTimeMillis() / 1000);
        mmData.writeInt(1);
        mmData.writeInt(0);
        mmData.writeInt(500);
        mmData.writeInt(0);

        MinigameMessagePacket mm = new MinigameMessagePacket();
		mm.command = "startGame";
		mm.data = mmData.encode().substring(4);
		player.client.sendPacket(mm);

    }

    @MinigameMessage("move")
	public void move(Player player, XtReader rd) {
    }
    
}
