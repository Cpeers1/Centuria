package org.asf.centuria.minigames.games;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.asf.centuria.Centuria;
import org.asf.centuria.accounts.highlevel.ItemAccessor;
import org.asf.centuria.data.XtReader;
import org.asf.centuria.data.XtWriter;
import org.asf.centuria.entities.players.Player;
import org.asf.centuria.levelevents.LevelEvent;
import org.asf.centuria.levelevents.LevelEventBus;
import org.asf.centuria.minigames.AbstractMinigame;
import org.asf.centuria.minigames.MinigameMessage;
import org.asf.centuria.packets.xt.gameserver.minigame.MinigameMessagePacket;

public class GameKinoParlor extends AbstractMinigame {

	private enum GameType {
		QueensDuel, FourCrows, MothsAndFlames, LunarPhases
	}

	Random random;
	GameType gameType;
	int wager;
	boolean doubleUpTaken;
	List<Integer> additionalParameters;
	static final int likesID = 2327;

	QueensDuel queensDuel;
	FourCrows fourCrows;
	MothsAndFlames mothsAndFlames;
	LunarPhases lunarPhases;

	public GameKinoParlor() {
		random = new Random();
		additionalParameters = new ArrayList<>();

		queensDuel = new QueensDuel();
		fourCrows = new FourCrows();
		mothsAndFlames = new MothsAndFlames();
		lunarPhases = new LunarPhases();
	}

	private void sendGameCommand(String command, String argument, Player player) {
		String[] oneElement = new String[1];
		oneElement[0] = argument;
		sendGameCommand(command, oneElement, player);
	}

	private void sendGameCommand(String command, String[] arguments, Player player) {
		if (command == "multiplierResults") {
			ItemAccessor acc = player.account.getSaveSpecificInventory().getItemAccessor(player);
			acc.add(likesID, wager * Integer.parseInt(arguments[0]));

			LevelEventBus.dispatch(
					new LevelEvent("levelevents.minigames.kinoparlor", new String[] { "won a game" }, player));
		}

		Centuria.logger.debug("from server - " + command);
		XtWriter xw = new XtWriter();

		for (String argument : arguments) {
			xw.writeString(argument);
			Centuria.logger.debug("from server - " + argument);
		}

		MinigameMessagePacket p = new MinigameMessagePacket();
		p.command = command;
		p.data = xw.encode().substring(4);
		player.client.sendPacket(p);
	}

	@Override
	public AbstractMinigame instantiate() {
		return new GameKinoParlor();
	}

	@Override
	public boolean canHandle(int levelID) {
		return levelID == 7789 || levelID == 12174;
	}

	@Override
	public void onJoin(Player player) {
	}

	@Override
	public void onExit(Player player) {
	}

	@MinigameMessage("loadGame")
	public void loadGame(Player player, XtReader rd) {
		gameType = GameType.values()[rd.readInt() - 1];
		Centuria.logger.debug(gameType.toString());

		int maximumWager = 1;

		// generate additional parameters
		switch (gameType) {
		case QueensDuel:
			additionalParameters = queensDuel.getParams();
			maximumWager = 100;
			break;
		case FourCrows:
			additionalParameters = fourCrows.getParams();
			maximumWager = 200;
			break;
		case MothsAndFlames:
			additionalParameters = mothsAndFlames.getParams();
			maximumWager = 100;
			break;
		case LunarPhases:
			additionalParameters = lunarPhases.getParams();
			maximumWager = 200;
			break;
		default:
			break;
		}

		// load game
		List<Integer> loadGameArgs = new ArrayList<>();
		loadGameArgs.add(maximumWager);
		loadGameArgs.add(additionalParameters.size());
		loadGameArgs.addAll(additionalParameters);
		List<String> loadGameArgsStrings = loadGameArgs.stream().map(Object::toString)
				.collect(Collectors.toUnmodifiableList());

		sendGameCommand("loadGame", loadGameArgsStrings.toArray(new String[0]), player);
	}

	@MinigameMessage("leaveGame")
	public void leaveGame(Player player, XtReader rd) {
		additionalParameters = new ArrayList<>();

		queensDuel = new QueensDuel();
		fourCrows = new FourCrows();
		mothsAndFlames = new MothsAndFlames();
		lunarPhases = new LunarPhases();
	}

	@MinigameMessage("placeWager")
	public void placeWager(Player player, XtReader rd) {

		wager = rd.readInt();

		ItemAccessor acc = player.account.getSaveSpecificInventory().getItemAccessor(player);
		acc.remove(likesID, wager);

		// accept wager
		sendGameCommand("wagerAccepted", String.valueOf(wager), player);
	}

	@MinigameMessage("doubleUp")
	public void doubleUp(Player player, XtReader rd) {
		doubleUpTaken = rd.readInt() == 1;
		if (doubleUpTaken) {
			wager *= 2;
			sendGameCommand("doubleUpResults", String.valueOf(1), player);
		} else {
			LevelEventBus.dispatch(
					new LevelEvent("levelevents.minigames.kinoparlor", new String[] { "won a game" }, player));

			ItemAccessor acc = player.account.getSaveSpecificInventory().getItemAccessor(player);
			acc.add(likesID, wager * 2);
			sendGameCommand("doubleUpResults", String.valueOf(0), player);
		}
	}

	/*
	 * @MinigameMessage("multiplier") public void multiplier(Player player, XtReader
	 * rd){ multiplier = rd.readInt(); sendGameCommand("multiplierResults",
	 * String.valueOf(multiplier), player); }
	 */
	@MinigameMessage("gameCommand")
	public void gameCommand(Player player, XtReader rd) {

		String command = rd.read();
		String[] gameCommandParameters = rd.readRemaining().split("%");

		Centuria.logger.debug(command);
		for (String parameter : gameCommandParameters) {
			Centuria.logger.debug(parameter);
		}

		switch (gameType) {
		case QueensDuel:
			queensDuel.command(player, rd, command, gameCommandParameters);
			break;
		case FourCrows:
			fourCrows.command(player, rd, command, gameCommandParameters);
			break;
		case MothsAndFlames:
			mothsAndFlames.command(player, rd, command, gameCommandParameters);
			break;
		case LunarPhases:
			lunarPhases.command(player, rd, command, gameCommandParameters);
			break;
		default:
			break;
		}
	}

	private class QueensDuel {

		int[] playersRoll;
		int[] kinosRoll;
		int[][] kinosLanes;

		int turn;
		int tieCount;
		int winCount;
		int loseCount;

		static QueensDuelDiceFace[] diceFaces = { QueensDuelDiceFace.Sword, QueensDuelDiceFace.Mask,
				QueensDuelDiceFace.Helmet, QueensDuelDiceFace.Mask, QueensDuelDiceFace.Crown,
				QueensDuelDiceFace.Sword };

		static final float[] binomcdf = { 0.0156f, 0.1094f, 0.3438f, 0.6562f, 0.8906f, 0.9844f, 1f }; // binomial
																										// cumulative
																										// distribution
																										// function, 6
																										// trials,
																										// probability
																										// 0.5

		private enum QueensDuelDiceFace {
			Sword, Mask, Helmet, Crown
		}

		public QueensDuel() {
			playersRoll = new int[6]; // each element is a digit representing a dice face
			kinosRoll = new int[6];
			kinosLanes = new int[][] { { 9, 9, 9, 9 }, { 9, 9, 9, 9 }, { 9, 9, 9, 9 } }; // each element is a dice from
																							// the roll
			turn = 0;
			tieCount = 0;
			winCount = 0;
			loseCount = 0;
		}

		private List<Integer> getParams() {
			return Arrays.asList(0);
		}

		private int getDiceFaceScore(QueensDuelDiceFace face) {
			switch (face) {
			case Crown:
				return 3;
			case Helmet:
				return 2;
			case Mask:
				return 1;
			case Sword:
				return 1;
			default:
				return 0;
			}
		}

		private int getLaneScore(int[] lane, int[] rolls) {
			List<QueensDuelDiceFace> laneToFaces = new ArrayList<>();
			for (int face : lane) {
				if (face != 9) {
					laneToFaces.add(diceFaces[rolls[face] - 1]);
				}
			}

			return getLaneScore(laneToFaces);
		}

		private int getLaneScore(List<QueensDuelDiceFace> lane) {
			int score = 0;
			for (QueensDuelDiceFace face : lane) {
				score += getDiceFaceScore(face);
			}
			return score;
		}

		private float getBinomcdfDiff(List<QueensDuelDiceFace> lane, QueensDuelDiceFace newFace) {
			int prevScore = getLaneScore(lane);
			int newScore = prevScore + getDiceFaceScore(newFace);

			return binomcdf[newScore] - binomcdf[prevScore];
		}

		private boolean canAddToLane(List<QueensDuelDiceFace> lane, QueensDuelDiceFace newFace) {
			if (newFace != QueensDuelDiceFace.Sword) {
				return !(lane.contains(QueensDuelDiceFace.Crown) || lane.contains(QueensDuelDiceFace.Helmet)
						|| lane.contains(QueensDuelDiceFace.Mask));
			} else {
				return !lane.isEmpty() && lane.size() < 4;
			}
		}

		private float absoluteWinRate(int[] rolls) {
			List<List<QueensDuelDiceFace>> bestLanes = bestLaneConfiguration(rolls);

			float totalWinRate = 0;
			for (List<QueensDuelDiceFace> bestLane : bestLanes) {
				totalWinRate += binomcdf[getLaneScore(bestLane)];
			}

			return totalWinRate / bestLanes.size();
		}

		private List<List<QueensDuelDiceFace>> bestLaneConfiguration(int[] rolls) {
			List<QueensDuelDiceFace> rolledFaces = new ArrayList<>();
			for (int roll : rolls) {
				rolledFaces.add(diceFaces[roll - 1]);
			}

			List<List<QueensDuelDiceFace>> lanes = new ArrayList<>();
			lanes.add(new ArrayList<>());
			lanes.add(new ArrayList<>());
			lanes.add(new ArrayList<>());

			while (true) {

				float highestBinomcdfDiff = -1f;
				int faceIndex = -1;
				int laneIndex = -1;

				boolean allDiceDeployed = true;

				for (int f = 0; f < rolledFaces.size(); f++) {
					for (int l = 0; l < lanes.size(); l++) {

						List<QueensDuelDiceFace> lane = lanes.get(l);
						QueensDuelDiceFace rolledFace = rolledFaces.get(f);

						if (rolledFace != null && canAddToLane(lane, rolledFace)
								&& getBinomcdfDiff(lane, rolledFace) > highestBinomcdfDiff) {

							highestBinomcdfDiff = getBinomcdfDiff(lane, rolledFace);
							faceIndex = f;
							laneIndex = l;
							allDiceDeployed = false;

						}
					}
				}

				if (allDiceDeployed) {
					break;
				}

				if (faceIndex != -1 && laneIndex != -1) {
					Centuria.logger.debug(Integer.toString(laneIndex) + " " + rolledFaces.get(faceIndex).toString()
							+ " " + Float.toString(highestBinomcdfDiff));

					lanes.get(laneIndex).add(rolledFaces.get(faceIndex));
					rolledFaces.set(faceIndex, null);
				}

			}

			return lanes;
		}

		private void generateKinosMove() {

			kinosRoll = new int[6];
			kinosLanes = new int[][] { { 9, 9, 9, 9 }, { 9, 9, 9, 9 }, { 9, 9, 9, 9 } };

			int[] kinosFirstRoll = new int[6];
			int[] kinosSecondRoll = new int[6];

			// Kino's first roll
			for (int i = 0; i < 6; i++) {
				kinosFirstRoll[i] = (char) random.nextInt(1, 7);
			}

			// Kino's second roll
			for (int i = 0; i < 6; i++) {
				kinosSecondRoll[i] = (char) random.nextInt(1, 7);
			}

			// I have to determine how Kino chooses to reroll their dice
			// pretending they have knowledge on what the reroll will be,
			// because the alternative is a complex game algorithm with
			// exponential time complexity
			float highestAbsWinRate = 0;
			for (int i = 0; i < 64; i++) { // 2^6
				BitSet reroll = BitSet.valueOf(new long[i]);
				int[] kinosCurrChoice = new int[6];

				for (int j = 0; j < 6; j++) {
					if (reroll.get(j)) {
						kinosCurrChoice[j] = kinosSecondRoll[j];
					} else {
						kinosCurrChoice[j] = kinosFirstRoll[j];
					}
				}

				float absWinRate = absoluteWinRate(kinosCurrChoice);
				if (absWinRate > highestAbsWinRate) {
					kinosRoll = kinosCurrChoice;
					highestAbsWinRate = absWinRate;
				}
			}

			// convert the lane configuration into a string containing dice indices
			List<List<QueensDuelDiceFace>> laneConfig = bestLaneConfiguration(kinosRoll);
			for (List<QueensDuelDiceFace> lane : laneConfig) {
				Centuria.logger.debug("new lane");
				for (QueensDuelDiceFace face : lane) {
					Centuria.logger.debug(face.toString());
				}
			}

			int[] remainingKinoDice = kinosRoll.clone();

			for (int i = 0; i < laneConfig.size(); i++) {

				List<QueensDuelDiceFace> lane = laneConfig.get(i);
				Collections.sort(lane, Collections.reverseOrder());

				for (int j = 0; j < lane.size(); j++) {

					QueensDuelDiceFace selectedFace = lane.get(j);

					for (int k = 0; k < remainingKinoDice.length; k++) {
						if (remainingKinoDice[k] != -1 && diceFaces[remainingKinoDice[k] - 1] == selectedFace) {
							kinosLanes[i][j] = k;
							remainingKinoDice[k] = -1;
							break;
						}
					}
				}
			}

		}

		private String intArrayToString(int[] arr) {
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < arr.length; i++) {
				sb.append(arr[i]);
			}
			return sb.toString();
		}

		private int[] stringToIntArray(String str) {
			int[] arr = new int[str.length()];
			for (int i = 0; i < str.length(); i++) {
				arr[i] = Character.getNumericValue(str.charAt(i));
			}
			return arr;
		}

		private void command(Player player, XtReader rd, String command, String[] gameCommandParameters) {
			switch (command) {
			case "requestRoll":

				String reroll = gameCommandParameters[0];
				turn = Integer.parseInt(gameCommandParameters[1]);

				// generate the player's and kino's roll
				for (int i = 0; i < 6; i++) {
					if (reroll.charAt(i) == '1') {
						playersRoll[i] = random.nextInt(1, 7);
					}
				}

				if (turn == 1) { // Kino's reroll is pregenerated
					generateKinosMove();
				}

				// convert rolls to string
				String roll = intArrayToString(playersRoll) + intArrayToString(kinosRoll);
				String[] rollResults = { "rollResults", roll };
				sendGameCommand("gameResponse", rollResults, player);

				// convert lanes to string
				String[] opponentSelection = new String[4];
				opponentSelection[0] = "opponentSelection";
				for (int i = 0; i < 3; i++) {
					opponentSelection[i + 1] = intArrayToString(kinosLanes[i]);
				}
				sendGameCommand("gameResponse", opponentSelection, player);

				break;
			case "requestCompare":

				tieCount = 0;
				winCount = 0; // from the player's persepctive
				loseCount = 0;
				for (int i = 0; i < 3; i++) {
					if (getLaneScore(stringToIntArray(gameCommandParameters[i]),
							playersRoll) > getLaneScore(kinosLanes[i], kinosRoll)) {
						winCount++;
					} else if (getLaneScore(stringToIntArray(gameCommandParameters[i]),
							playersRoll) < getLaneScore(kinosLanes[i], kinosRoll)) {
						loseCount++;
					} else {
						tieCount++;
					}
				}

				String result;
				if (tieCount > 1 || winCount == loseCount) {
					result = "tie";
				} else if (winCount > loseCount) {
					result = "win";
				} else {
					result = "lose";
				}

				String[] compareResults = { "compareResults", result }; // "win", "lose" or "tie"
				sendGameCommand("gameResponse", compareResults, player);
				break;
			case "requestTieRoll":
				tieCount = 1;

				int[] kinosTieBreakingRolls = new int[tieCount];
				int[] playersTieBreakingRolls = new int[tieCount];

				int newTieCount = 0;

				for (int i = 0; i < tieCount; i++) {
					kinosTieBreakingRolls[i] = random.nextInt(1, 7);
					playersTieBreakingRolls[i] = random.nextInt(1, 7);

					if (diceFaces[playersTieBreakingRolls[i] - 1].ordinal() > diceFaces[kinosTieBreakingRolls[i] - 1]
							.ordinal()) {

						winCount++;
					} else if (diceFaces[playersTieBreakingRolls[i] - 1]
							.ordinal() < diceFaces[kinosTieBreakingRolls[i] - 1].ordinal()) {

						loseCount++;
					} else {
						newTieCount++;
					}
				}

				String tieResult;
				if (winCount > loseCount) {
					tieResult = "win";
				} else {
					tieResult = "lose";
				}

				String[] tieResponse = new String[tieCount * 2 + 3];
				tieResponse[0] = "tieResponse";
				tieResponse[1] = Integer.toString(tieCount);
				tieResponse[tieCount * 2 + 2] = tieResult;

				for (int i = 0; i < tieCount; i++) {
					tieResponse[2 * (i + 1)] = Integer.toString(playersTieBreakingRolls[i]);
					tieResponse[2 * (i + 1) + 1] = Integer.toString(kinosTieBreakingRolls[i]);
				}
				sendGameCommand("gameResponse", tieResponse, player);

				tieCount = newTieCount;

				break;
			default:
				break;
			}
		}
	}

	private class FourCrows {

		List<Integer> playersHand; // array of indices from the card deck
		List<Integer> kinosHand;
		List<FourCrowsCardType> playersPlayedCards;
		List<FourCrowsCardType> kinosPlayedCards;

		static FourCrowsCardType[] deck;
		static final float[] binompdf = { 0.07776f, 0.2592f, 0.3456f, 0.2304f, 0.0768f, 0f }; // binomial point
																								// distribution
																								// function, 5 trials,
																								// probability 0.4, last
																								// value set to 0
		static final int[] suitOfCards = { 2, 4, 4, 2, 2 };
		static final int numCardsPerPlayer = 5;
		static int totalNumCards;

		int turn = 0;

		static {
			totalNumCards = 0;

			List<FourCrowsCardType> deckList = new ArrayList<>();
			for (int i = 0; i < suitOfCards.length; i++) {
				totalNumCards += suitOfCards[i];
				for (int j = 0; j < suitOfCards[i]; j++) {
					deckList.add(FourCrowsCardType.values()[i]);
					Centuria.logger.debug(FourCrowsCardType.values()[i].toString());
				}
			}
			deck = deckList.toArray(new FourCrowsCardType[0]);
		}

		enum FourCrowsCardType {
			Positive2(2), Positive1(1), Negative1(-1), Negative2(-2), Skip(0);

			private final int value;

			private FourCrowsCardType(int value) {
				this.value = value;
			}

			public int getVal() {
				return value;
			}
		}

		public FourCrows() {
			playersHand = new ArrayList<>();
			kinosHand = new ArrayList<>();
			playersPlayedCards = new ArrayList<>();
			kinosPlayedCards = new ArrayList<>();
		}

		private List<FourCrowsCardType> listOfUndealtPlayerCards() {
			List<FourCrowsCardType> undealtCards = new ArrayList<>(Arrays.asList(deck));

			for (int card : kinosHand) {
				undealtCards.remove(deck[card]);
			}
			for (FourCrowsCardType card : playersPlayedCards) {
				undealtCards.remove(card);
			}
			for (FourCrowsCardType card : kinosPlayedCards) {
				undealtCards.remove(card);
			}

			return undealtCards;
		}

		private float getChangeInPlayerWinProb(FourCrowsCardType deal) {
			int prevCrows = getNumCrows();
			int nextCrows = getNumCrows() + deal.getVal();
			nextCrows = Math.max(0, Math.min(nextCrows, 5));

			return binompdf[nextCrows] - binompdf[prevCrows];
		}

		private float getChangeInKinoWinProb(FourCrowsCardType playerDeal, FourCrowsCardType kinoDeal) {
			int prevCrows = getNumCrows() + playerDeal.getVal();
			prevCrows = Math.max(0, Math.min(prevCrows, 5));
			int nextCrows = prevCrows + kinoDeal.getVal();
			nextCrows = Math.max(0, Math.min(nextCrows, 5));

			return binompdf[prevCrows] - binompdf[nextCrows];
		}

		private FourCrowsCardType playersMostLikelyDeal() {
			List<FourCrowsCardType> undealtCards = listOfUndealtPlayerCards();
			Map<FourCrowsCardType, Float> cardFreq = new HashMap<>();
			for (FourCrowsCardType card : undealtCards) {
				cardFreq.put(card, cardFreq.getOrDefault(card, 0f) + 1f);
			}

			float highestWinProb = -1f;
			FourCrowsCardType mostLikelyDeal = null;
			for (FourCrowsCardType card : cardFreq.keySet()) {
				float currProb = cardFreq.get(card) * getChangeInPlayerWinProb(card);
				if (currProb > highestWinProb) {
					highestWinProb = currProb;
					mostLikelyDeal = card;
				}
			}

			return mostLikelyDeal;
		}

		private int getKinosPlay() {
			FourCrowsCardType mostLikelyDeal = playersMostLikelyDeal();
			float highestWinProb = -1f;
			int kinosPlay = -1; // index of card in hand

			for (int i = 0; i < kinosHand.size(); i++) {
				FourCrowsCardType currCard = deck[kinosHand.get(i)];
				float currWinProb = getChangeInKinoWinProb(mostLikelyDeal, currCard)
						* (float) Math.pow(Math.abs(currCard.getVal()) + 1f, -1f / turn);
				if (currWinProb > highestWinProb) {
					highestWinProb = currWinProb;
					kinosPlay = i;
				}
			}

			kinosPlayedCards.add(deck[kinosHand.get(kinosPlay)]);
			kinosHand.remove(kinosPlay);

			return kinosPlay;
		}

		private int getNumCrows() {
			int numCrows = 0;
			for (int i = 0; i < playersPlayedCards.size(); i++) {
				Centuria.logger.debug(playersPlayedCards.get(i).toString() + " " + kinosPlayedCards.get(i).toString());
				numCrows += playersPlayedCards.get(i).getVal();
				numCrows += kinosPlayedCards.get(i).getVal();
				if (numCrows < 0) {
					numCrows = 0;
				}
			}
			return numCrows;
			// return numCrows < 5 ? numCrows : 0;
		}

		private List<Integer> getParams() {
			// [0] Positive2 cards - 2
			// [1] Positive1 cards - 4
			// [2] Negative1 cards - 4
			// [3] Negative2 cards - 2
			// [4] Skip cards - 2

			return IntStream.of(suitOfCards).boxed().collect(Collectors.toList());
		}

		private void command(Player player, XtReader rd, String command, String[] gameCommandParameters) {
			switch (command) {
			case "requestDeal":

				playersHand = new ArrayList<>();
				kinosHand = new ArrayList<>();
				playersPlayedCards = new ArrayList<>();
				kinosPlayedCards = new ArrayList<>();

				String[] dealCardsResponse = new String[2 * numCardsPerPlayer + 3];
				dealCardsResponse[0] = "dealCardsResponse";

				// dealIndexes
				dealCardsResponse[1] = Integer.toString(2 * numCardsPerPlayer);

				// generate the player's and Kino's decks of cards
				List<Integer> cardIndices = new ArrayList<>();
				for (int i = 0; i < totalNumCards; i++) {
					cardIndices.add(i);
				}
				Collections.shuffle(cardIndices);
				cardIndices = cardIndices.subList(0, 2 * numCardsPerPlayer);
				for (int i = 0; i < numCardsPerPlayer; i++) {
					playersHand.add(cardIndices.get(i));
					kinosHand.add(cardIndices.get(numCardsPerPlayer + i));
				}

				// write the decks to the packet
				for (int i = 0; i < 2 * numCardsPerPlayer; i++) {
					dealCardsResponse[i + 2] = Integer.toString(cardIndices.get(i));
				}

				// dealerPlay
				dealCardsResponse[2 * numCardsPerPlayer + 2] = Integer.toString(getKinosPlay());

				sendGameCommand("gameResponse", dealCardsResponse, player);

				turn = 0;
				break;
			case "playerPlay":

				turn++;

				int playersPlay = Integer.parseInt(gameCommandParameters[0]); // index of card in hand
				playersPlayedCards.add(deck[playersHand.get(playersPlay)]);
				playersHand.remove(playersPlay);

				String[] dealerResponse = new String[2];
				dealerResponse[0] = "dealerResponse";
				dealerResponse[1] = Integer.toString(getKinosPlay()); // dealerPlay
				sendGameCommand("gameResponse", dealerResponse, player);

				Centuria.logger.debug(Integer.toString(getNumCrows()) + " crows");
				if (getNumCrows() > 4 || kinosHand.size() == 0) {
					sendGameCommand("multiplierResults", Integer.toString(getNumCrows()), player);
				}

				break;
			default:
				break;
			}
		}
	}

	private class MothsAndFlames {

		int[] roll;
		int moths;
		int fires;
		// 1 - fire
		// 2 - moth
		// 3 - moth
		// 4 - moth
		// 5 - fire
		// 6 - fire

		public MothsAndFlames() {
			roll = new int[3];
			moths = 0;
			fires = 0;
		}

		private List<Integer> getParams() {
			return Arrays.asList(0);
		}

		private void command(Player player, XtReader rd, String command, String[] gameCommandParameters) {
			switch (command) {
			case "requestRoll":
				roll = new int[3];
				moths = 0;
				fires = 0;

				StringBuilder sb = new StringBuilder();

				for (int i = 0; i < 3; i++) {
					roll[i] = random.nextInt(6) + 1; // Generate a random number between 1 and 6 (inclusive)
					sb.append(roll[i]);
					switch (roll[i]) {
					case 1:
						fires++;
						break;
					case 5:
						fires++;
						break;
					case 6:
						fires++;
						break;
					case 2:
						moths++;
						break;
					case 3:
						moths++;
						break;
					case 4:
						moths++;
						break;
					}
				}

				String[] rollResults = { "rollResults", sb.toString() };
				sendGameCommand("gameResponse", rollResults, player);

				break;
			case "playerPlay":
				// 0 - 3 moths
				// 1 - 2 moths
				// 2 - 2 fire
				// 3 - 3 fire

				int guess = Integer.parseInt(gameCommandParameters[0]);
				Centuria.logger
						.debug(Integer.toString(moths) + " " + Integer.toString(fires) + " " + Integer.toString(guess));
				if ((guess == 0 && moths == 3) || (guess == 2 && fires == 3)) {
					sendGameCommand("multiplierResults", "3", player);
				} else if ((guess == 1 && moths >= 2) || (guess == 3 && fires >= 2)) {
					sendGameCommand("multiplierResults", "2", player);
				} else {
					sendGameCommand("multiplierResults", "0", player);
				}

				break;
			default:
				break;
			}
		}
	}

	private class LunarPhases {

		List<LunarPhasesTile> playersHand; // array of indices from the card deck
		List<LunarPhasesTile> kinosHand;
		List<LunarPhasesTile> playersPlayedCards;
		List<LunarPhasesTile> kinosPlayedCards;
		List<LunarPhasesTile> removeDeck; // cards that are withdrawn from the deck are temporarily placed here
		List<LunarPhasesTile> deck;

		static final int[] suitOfCards = { 4, 4, 4, 4, 4, 4, 4, 4 };
		static final int numCardsPerPlayer = 5;
		static int totalNumCards;

		class LunarPhasesTile {
			final private LunarPhasesTileType tileType;
			final private int value;

			public LunarPhasesTile(int Value) {
				switch (Value) {
				case 0:
					tileType = LunarPhasesTileType.New;
					break;
				case 1:
					tileType = LunarPhasesTileType.Crescent;
					break;
				case 2:
					tileType = LunarPhasesTileType.Quarter;
					break;
				case 3:
					tileType = LunarPhasesTileType.Gibbous;
					break;
				case 4:
					tileType = LunarPhasesTileType.Full;
					break;
				case 5:
					tileType = LunarPhasesTileType.Gibbous;
					break;
				case 6:
					tileType = LunarPhasesTileType.Quarter;
					break;
				case 7:
					tileType = LunarPhasesTileType.Crescent;
					break;
				default:
					tileType = null;
					break;
				}
				value = Value;
			}

			public LunarPhasesTileType getTileType() {
				return tileType;
			}

			public int getValue() {
				return value;
			}
		}

		enum LunarPhasesTileType {
			New, Crescent, Quarter, Gibbous, Full
		}

		public LunarPhases() {
			playersHand = new ArrayList<>();
			kinosHand = new ArrayList<>();
			playersPlayedCards = new ArrayList<>();
			kinosPlayedCards = new ArrayList<>();
			removeDeck = new ArrayList<>();
			deck = new ArrayList<>();

			totalNumCards = 0;
		}

		private void removeCardsFromDeck() {
			for (LunarPhasesTile removeCard : removeDeck) {
				deck.remove(removeCard);
			}
			removeDeck.clear();
		}

		private int kinoPlayRandCard() {
			int kinoDraw = random.nextInt(deck.size());
			LunarPhasesTile kinoCard = deck.get(kinoDraw);

			while (removeDeck.contains(kinoCard)) {
				kinoDraw = random.nextInt(deck.size());
				kinoCard = deck.get(kinoDraw);
			}

			kinosPlayedCards.add(kinoCard);
			removeDeck.add(kinoCard);
			return kinoDraw;
		}

		private int kinoGiveRandCard() {
			int kinoDraw = random.nextInt(deck.size());
			LunarPhasesTile kinoCard = deck.get(kinoDraw);

			while (removeDeck.contains(kinoCard)) {
				kinoDraw = random.nextInt(deck.size());
				kinoCard = deck.get(kinoDraw);
			}

			kinosHand.add(kinoCard);
			removeDeck.add(kinoCard);
			return kinoDraw;
		}

		private int playerPlayRandCard() {
			int playerDraw = random.nextInt(deck.size());
			LunarPhasesTile playerCard = deck.get(playerDraw);

			while (removeDeck.contains(playerCard)) {
				playerDraw = random.nextInt(deck.size());
				playerCard = deck.get(playerDraw);
			}

			playersPlayedCards.add(playerCard);
			removeDeck.add(playerCard);
			return playerDraw;
		}

		private int playerGiveRandCard() {
			int playerDraw = random.nextInt(deck.size());
			LunarPhasesTile playerCard = deck.get(playerDraw);

			while (removeDeck.contains(playerCard)) {
				playerDraw = random.nextInt(deck.size());
				playerCard = deck.get(playerDraw);
			}

			playersHand.add(playerCard);
			removeDeck.add(playerCard);
			return playerDraw;
		}

		private int getKinosFirstPick() {

			LunarPhasesTile bestCard = null;
			int bestCardIdx = -1;
			int highestDistFromPlayer = -100;
			int playerPos = getPlayerPos();

			for (int i = 0; i < 3; i++) {

				LunarPhasesTile currCard = kinosHand.get(i);
				int currDist = getNewKinoPos(currCard) - playerPos;

				if (currDist > highestDistFromPlayer) {
					currDist = highestDistFromPlayer;
					bestCard = currCard;
					bestCardIdx = i;
				}
			}

			kinosPlayedCards.add(bestCard);
			removeDeck.add(bestCard);
			return bestCardIdx;
		}

		private int getNewKinoPos(LunarPhasesTile newTile) {
			int pos = 0;
			int[] cardFreq = new int[8];
			List<LunarPhasesTile> kinosPlayedCardsAfter = new ArrayList<>(kinosPlayedCards);
			kinosPlayedCardsAfter.add(newTile);

			for (LunarPhasesTile card : kinosPlayedCardsAfter) {
				cardFreq[card.getValue()]++;
				pos = (pos + card.getValue()) % 8;
			}

			if (cardFreq[4] >= 3) {
				return 9;
			} else if ((cardFreq[1] > 0 && cardFreq[7] > 0) || (cardFreq[3] > 0 && cardFreq[5] > 0)) {
				return 8;
			} else {
				return pos;
			}
		}

		private int getKinoPos() {
			int pos = 0;
			int[] cardFreq = new int[8];
			for (LunarPhasesTile card : kinosPlayedCards) {
				// Centuria.logger.debug(card.getTileType().toString() + " -kino card- " +
				// Integer.toString(card.getValue()));

				cardFreq[card.getValue()]++;
				pos = (pos + card.getValue()) % 8;
			}

			if (cardFreq[4] >= 3) {
				return 9;
			} else if ((cardFreq[1] > 0 && cardFreq[7] > 0) || (cardFreq[3] > 0 && cardFreq[5] > 0)) {
				return 8;
			} else {
				return pos;
			}
		}

		private int getPlayerPos() {
			int pos = 0;
			int[] cardFreq = new int[8];
			for (LunarPhasesTile card : playersPlayedCards) {
				// Centuria.logger.debug(card.getTileType().toString() + " -player card- " +
				// Integer.toString(card.getValue()));

				cardFreq[card.getValue()]++;
				pos = (pos + card.getValue()) % 8;
			}

			if (cardFreq[4] >= 3) {
				return 9;
			} else if ((cardFreq[1] > 0 && cardFreq[7] > 0) || (cardFreq[3] > 0 && cardFreq[5] > 0)) {
				return 8;
			} else {
				return pos;
			}
		}

		private int getKinosNextPick() {
			LunarPhasesTile kinoCard = kinosHand.get(3);

			if (getNewKinoPos(kinoCard) > getKinoPos()) { // simpler than actually doing the math
				kinosPlayedCards.add(kinoCard);
				return 1;
			} else {
				return 0;
			}
		}

		private List<Integer> getParams() {

			return Arrays.asList(0);
		}

		private void command(Player player, XtReader rd, String command, String[] gameCommandParameters) {
			switch (command) {
			case "requestDeal":

				playersHand = new ArrayList<>();
				kinosHand = new ArrayList<>();
				playersPlayedCards = new ArrayList<>();
				kinosPlayedCards = new ArrayList<>();
				removeDeck = new ArrayList<>();

				// generate the deck.
				deck = new ArrayList<>();
				for (int i = 0; i < suitOfCards.length; i++) {
					totalNumCards += suitOfCards[i];
					for (int j = 0; j < suitOfCards[i]; j++) {
						deck.add(new LunarPhasesTile(i));
					}
				}

				// generate the packet
				String[] dealCardsResponse = new String[2 * numCardsPerPlayer + 4];
				dealCardsResponse[0] = "dealCardsResponse";

				dealCardsResponse[1] = Integer.toString(2 * numCardsPerPlayer); // number of elements
				dealCardsResponse[2] = Integer.toString(playerPlayRandCard());

				for (int i = 1; i < numCardsPerPlayer; i++) {
					dealCardsResponse[i + 2] = Integer.toString(playerGiveRandCard());
				}

				dealCardsResponse[7] = Integer.toString(kinoPlayRandCard());

				for (int i = 1; i < numCardsPerPlayer; i++) {
					dealCardsResponse[i + 7] = Integer.toString(kinoGiveRandCard());
				}

				// dealerPlay
				dealCardsResponse[2 * numCardsPerPlayer + 2] = "0";
				dealCardsResponse[2 * numCardsPerPlayer + 3] = "0";

				sendGameCommand("gameResponse", dealCardsResponse, player);
				removeCardsFromDeck();
				break;
			case "playerPlay":

				int playersPlay = Integer.parseInt(gameCommandParameters[0]); // index of card in hand
				playersPlayedCards.add(playersHand.get(playersPlay)); // do not remove card from deck

				String[] playerResponse = new String[3];
				playerResponse[0] = "playerResponse";
				playerResponse[1] = Integer.toString(playersPlay);
				playerResponse[2] = Integer.toString(getKinosFirstPick()); // dealerPlay
				sendGameCommand("gameResponse", playerResponse, player);
				removeCardsFromDeck();

				Centuria.logger.debug(Integer.toString(getPlayerPos()) + " is the player's position");
				Centuria.logger.debug(Integer.toString(getKinoPos()) + " is Kino's position");

				break;
			case "drawTile":
				int drawOrStand = Integer.parseInt(gameCommandParameters[0]);
				if (drawOrStand == 1) { // draw tile
					playersPlayedCards.add(playersHand.get(3));
				}

				String[] drawResponse = new String[3];
				drawResponse[0] = "drawResponse";
				drawResponse[1] = gameCommandParameters[0];
				drawResponse[2] = Integer.toString(getKinosNextPick());
				sendGameCommand("gameResponse", drawResponse, player);

				break;
			case "requestCompare":
				if (getPlayerPos() == 9) {
					sendGameCommand("multiplierResults", "5", player);
				} else if (getPlayerPos() > getKinoPos()) {
					if (getPlayerPos() == 8) {
						sendGameCommand("multiplierResults", "3", player);
					} else {
						sendGameCommand("multiplierResults", "2", player);
					}
				} else if (getPlayerPos() == getKinoPos()) {
					String[] compareResults = { "compareResults", "tie" }; // "win", "lose" or "tie"
					sendGameCommand("gameResponse", compareResults, player);
				} else if (getPlayerPos() < getKinoPos()) {
					String[] compareResults = { "compareResults", "lose" }; // "win", "lose" or "tie"
					sendGameCommand("gameResponse", compareResults, player);
				}
				break;
			case "requestTieRoll":
				String[] tieResponse = new String[5];
				tieResponse[0] = "tieResponse";
				tieResponse[1] = "1";
				tieResponse[2] = Integer.toString(playerPlayRandCard());
				tieResponse[3] = Integer.toString(kinoPlayRandCard());
				if (getPlayerPos() > getKinoPos()) {
					tieResponse[4] = "win";
				} else {
					tieResponse[4] = "lose";
				}

				sendGameCommand("gameResponse", tieResponse, player);
				break;
			default:
				break;
			}
		}
	}

}
