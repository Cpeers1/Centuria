package org.asf.centuria.minigames.games;

import java.util.ArrayList;
import java.util.Random;

import org.asf.centuria.data.XtReader;
import org.asf.centuria.data.XtWriter;
import org.asf.centuria.entities.players.Player;
import org.asf.centuria.minigames.AbstractMinigame;
import org.asf.centuria.minigames.MinigameMessage;
import org.asf.centuria.packets.xt.gameserver.minigame.MinigameCurrencyPacket;
import org.asf.centuria.packets.xt.gameserver.minigame.MinigameMessagePacket;
import org.asf.centuria.packets.xt.gameserver.minigame.MinigamePrizePacket;

public class GameWhatTheHex extends AbstractMinigame {

	public class Element {
		public int currencyRewardType;
		public int currencyRewardAmount;
		public int elementID;
	}

	public class RuneSet {
		public byte rotation;
		public byte amount;
		public byte[] types;
	}

	private ArrayList<Element> elements = new ArrayList<Element>();
	private Random rnd = new Random();

	private ArrayList<RuneSet> placed = new ArrayList<RuneSet>();

	private RuneSet first;
	private RuneSet second;
	private RuneSet third;
	
	private byte firstExtraRotation;
	private byte secondExtraRotation;
	private byte thirdExtraRotation;

	// Notes

	// 1 = black?
	// 2 = flame
	// 3 = flora
	// 4 = miasma
	// 5 = flame cyclone
	// 6 = flora cyclone
	// 7 = miasma cyclone
	// 8 = black?
	// 9 = bomb
	// 10 = black bomb?

	// 1 runes = 0
	// 2 runes = 1-3
	// 3 runes = 4-11

	// Utility to make spawning easier
	private RuneSet spawnTiles(XtWriter target) {
		RuneSet set = new RuneSet();

		// Select rotation and count
		int rc = rnd.nextInt(0, 12);
		int count = 0;
		if (rc >= 4)
			count = 3;
		else if (rc >= 1)
			count = 2;
		else
			count = 1;
		set.amount = (byte)count;
		set.rotation = (byte)(count == 1 ? rc : (count == 2 ? rc - 1 : rc - 4));
		set.types = new byte[count];

		// Write
		target.writeInt(rc);
		target.writeInt(count);
		for (int i = 0; i < count; i++) {
			set.types[i] = (byte)(i + 2);
			target.writeInt(set.types[i]);
		}

		return set;
	}

	@Override
	public boolean canHandle(int levelID) {
		return levelID == 3272;
	}

	@Override
	public void onJoin(Player player) {
		// Send currency packet
		MinigameCurrencyPacket currency = new MinigameCurrencyPacket();
		currency.Currency = 2709;
		player.client.sendPacket(currency);
	}

	@MinigameMessage("startGame")
	public void startGame(Player player, XtReader rd) {
		// Start game

		// Set up elements
		elements.clear();
		Element flame = new Element();
		flame.elementID = 2;
		flame.currencyRewardAmount = 25;
		flame.currencyRewardType = 2327;
		elements.add(flame);
		Element flora = new Element();
		flora.elementID = 3;
		flora.currencyRewardAmount = 25;
		flora.currencyRewardType = 2327;
		elements.add(flora);
		Element miasma = new Element();
		miasma.elementID = 4;
		miasma.currencyRewardAmount = 10;
		miasma.currencyRewardType = 2327;
		elements.add(miasma);

		// Set up rewards
		int i = 0;
		for (Element ele : elements) {
			MinigamePrizePacket p1 = new MinigamePrizePacket();
			p1.given = false;
			p1.itemDefId = Integer.toString(ele.currencyRewardType); // Likes
			p1.itemCount = ele.currencyRewardAmount;
			p1.prizeIndex1 = i++;
			p1.prizeIndex2 = 0;
			player.client.sendPacket(p1);
		}

		// Start the game
		XtWriter wr = new XtWriter();
		first = spawnTiles(wr);
		second = spawnTiles(wr);
		third = spawnTiles(wr);
		MinigameMessagePacket pk = new MinigameMessagePacket();
		pk.command = "startGame";
		pk.data = wr.encode().substring(4);
		player.client.sendPacket(pk);
	}

	@Override
	public AbstractMinigame instantiate() {
		return new GameWhatTheHex();
	}

}
