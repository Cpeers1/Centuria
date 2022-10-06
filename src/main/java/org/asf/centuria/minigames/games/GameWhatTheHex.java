package org.asf.centuria.minigames.games;

import java.util.ArrayList;
import java.util.Random;

import org.apache.logging.log4j.MarkerManager;
import org.asf.centuria.Centuria;
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

	private RuneSet first;
	private RuneSet second;
	private RuneSet third;

	private byte firstExtraRotation = 0;
	private byte secondExtraRotation = 0;
	private byte thirdExtraRotation = 0;

	// Doing it binary to conserve memory usage
	private byte[] board = new byte[42];

	// Tool to get the board index of a point
	private int getBoardIndex(int x, int y) {
		int ux = x + 3;
		int uy = y + 3;
		int p = (ux * 7) + uy;
		return p;
	}

	// Tool to get the coordinates of a board index
	private int[] getCoordinates(int index) {
		int ux = 0;
		int uy = 0;
		
		// Calculate ux
		while (index >= 7) {
			ux++;
			index -= 7;
		}
		uy = index;
		
		// Calculate result
		int x = ux - 3;
		int y = uy - 3;		
		return new int[] {x,y};
	}

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
		set.amount = (byte) count;
		set.rotation = (byte) rc;
		set.types = new byte[count];

		// Write
		target.writeInt(rc);
		target.writeInt(count);
		for (int i = 0; i < count; i++) {
			set.types[i] = (byte) (i + 2);
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

	@MinigameMessage("rotateTile")
	public void rotateTile(Player player, XtReader rd) {
		// Rotate tile
		int ind = rd.readInt();
		switch (ind) {
		case 0:
			if (firstExtraRotation + 1 < first.amount)
				firstExtraRotation++;
			else
				firstExtraRotation = 0;
			break;
		case 1:
			if (secondExtraRotation + 1 < second.amount)
				secondExtraRotation++;
			else
				secondExtraRotation = 0;
			break;
		case 2:
			if (thirdExtraRotation + 1 < third.amount)
				thirdExtraRotation++;
			else
				thirdExtraRotation = 0;
			break;
		}
	}

	@MinigameMessage("placeTile")
	public void placeTile(Player player, XtReader rd) {
		// Place tile

		// Read packet
		int ind = rd.readInt();
		int x = rd.readInt();
		int y = rd.readInt();

		// Send packet
		XtWriter wr = new XtWriter();
		wr.writeInt(ind); // index
		wr.writeInt(x); // x
		wr.writeInt(y); // y
		wr.writeInt(1); // ?
		wr.writeInt(0); // ?
		wr.writeInt(0); // ?
		wr.writeInt(0); // ?
		wr.writeInt(2); // ?
		wr.writeInt(0); // ?
		wr.writeInt(0); // ?
		wr.writeInt(0); // ?
		wr.writeInt(1); // ?
		MinigameMessagePacket pk = new MinigameMessagePacket();
		pk.command = "placeTile";
		pk.data = wr.encode().substring(4);
		player.client.sendPacket(pk);

		// 2-rune tiles:
		// 0: lower = center
		// 1: lower = center
		// 2: upper = center
		
		
		// Find tile
		int rot = 0;
		RuneSet tile = null;
		switch (ind) {
		case 0: {
			rot = firstExtraRotation;
			firstExtraRotation = 0;
			tile = first;
			wr = new XtWriter();
			wr.writeInt(ind);
			first = spawnTiles(wr);
			pk = new MinigameMessagePacket();
			pk.command = "spawnTile";
			pk.data = wr.encode().substring(4);
			player.client.sendPacket(pk);
			break;
		}
		case 1: {
			rot = secondExtraRotation;
			secondExtraRotation = 0;
			tile = second;
			wr = new XtWriter();
			wr.writeInt(ind);
			second = spawnTiles(wr);
			pk = new MinigameMessagePacket();
			pk.command = "spawnTile";
			pk.data = wr.encode().substring(4);
			player.client.sendPacket(pk);
			break;
		}
		case 2: {
			rot = thirdExtraRotation;
			thirdExtraRotation = 0;
			tile = third;
			wr = new XtWriter();
			wr.writeInt(ind);
			third = spawnTiles(wr);
			pk = new MinigameMessagePacket();
			pk.command = "spawnTile";
			pk.data = wr.encode().substring(4);
			player.client.sendPacket(pk);
			break;
		}
		}

		Centuria.logger.debug(MarkerManager.getMarker("WhatTheHex"),"Placed: " + x + "/" + y + " (" + tile.rotation + ")");
		
		// Place tiles
		// It seems with 3-rune tiles, the first and second rune are swapped
		if (tile.types.length == 3) {
			byte t1 = tile.types[1];
			tile.types[1] = tile.types[0];
			tile.types[0] = t1; 
		}
		
		// Handle the manual rotation
		if (tile.types.length > 1 && rot > 0) {
			for (int i = 0; i < rot; i++) {
				byte[] newTypes = new byte[tile.types.length];
				
				if (tile.types.length == 2) {
					newTypes[1] = tile.types[0];
					newTypes[0] = tile.types[1];
				} else {
					newTypes[0] = tile.types[1];
					newTypes[1] = tile.types[2];
					newTypes[2] = tile.types[0];
				}
				tile.types = newTypes;
			}
		}
		
		// Place tiles in memory
		switch (tile.rotation) {
		case 0: {
			// Place single tile
			board[this.getBoardIndex(x, y)] = tile.types[0];
			break;
		}
		case 1: {
			// Place double tile (rotation 1)
			board[this.getBoardIndex(x, y)] = tile.types[1];
			board[this.getBoardIndex(x, y + 1)] = tile.types[0];
			break;
		}
		case 2: {
			// Place double tile (rotation 2)
			board[this.getBoardIndex(x - 1, y)] = tile.types[0];
			board[this.getBoardIndex(x, y)] = tile.types[1];
		}
		case 3: {
			// Place double tile (rotation 3)
			board[this.getBoardIndex(x, y)] = tile.types[0];
			board[this.getBoardIndex(x, y - 1)] = tile.types[1];
			break;
		}
		case 6: {
			// Place triple tile (rotation 2)
			board[this.getBoardIndex(x + 1, y + 1)] = tile.types[0];
			board[this.getBoardIndex(x, y)] = tile.types[1];
			board[this.getBoardIndex(x - 1, y - 1)] = tile.types[2];
			break;
		}
		default: {
			tile = tile;
			break;
		}
		}
	}

	@MinigameMessage("startGame")
	public void startGame(Player player, XtReader rd) {
		// Start game

		firstExtraRotation = 0;
		secondExtraRotation = 0;
		thirdExtraRotation = 0;

		// Set up elements
		elements.clear();
		Element flame = new Element();
		flame.elementID = 2;
		flame.currencyRewardAmount = rnd.nextInt(2, 6) * 5;
		flame.currencyRewardType = 2327;
		elements.add(flame);
		Element flora = new Element();
		flora.elementID = 3;
		flora.currencyRewardAmount = rnd.nextInt(2, 6) * 5;
		flora.currencyRewardType = 2327;
		elements.add(flora);
		Element miasma = new Element();
		miasma.elementID = 4;
		miasma.currencyRewardAmount = rnd.nextInt(2, 6) * 5;
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
