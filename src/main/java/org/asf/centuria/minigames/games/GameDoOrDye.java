package org.asf.centuria.minigames.games;

import org.asf.centuria.Centuria;
import org.asf.centuria.data.XtReader;
import org.asf.centuria.data.XtWriter;
import org.asf.centuria.entities.players.Player;
import org.asf.centuria.enums.minigames.CodeColor;
import org.asf.centuria.minigames.AbstractMinigame;
import org.asf.centuria.minigames.MinigameMessage;
import org.asf.centuria.packets.xt.gameserver.minigame.MinigameCurrencyPacket;
import org.asf.centuria.packets.xt.gameserver.minigame.MinigameMessagePacket;
import org.asf.centuria.packets.xt.gameserver.minigame.MinigamePrizePacket;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

public class GameDoOrDye extends AbstractMinigame {

    public int level;
    public ArrayList<CodeColor> solution = new ArrayList<>();
    public ArrayList<CodeColor> dyesOnScreen = new ArrayList<>();
    public int scorePerIngredient;
    public boolean multipleGuesses = false;

    
    @Override
	public boolean canHandle(int levelID) {
		return levelID == 5054;
	}

    @Override
	public void onJoin(Player plr) {
		MinigameCurrencyPacket currency = new MinigameCurrencyPacket();
		currency.Currency = 1141;
		plr.client.sendPacket(currency);
	}

    @Override
    public void onExit(Player plr) {
    }

    @MinigameMessage("startLevel")
	public void startLevel(Player plr, XtReader rd) {
		//Deserialize
        level = rd.readInt();

        ArrayList<CodeColor> availableDyes = new ArrayList<>();
        int startingIngredientCount = 0;
        solution.clear();

        switch (level+1){
            case 1:
                Collections.addAll(solution, CodeColor.Cyan, CodeColor.Red);
                Collections.addAll(availableDyes, CodeColor.Cyan, CodeColor.Red);
                startingIngredientCount = 12;
                scorePerIngredient = 7;
                break;
            case 2:
                Collections.addAll(solution, CodeColor.Purple, CodeColor.Red);
                Collections.addAll(availableDyes, CodeColor.Purple, CodeColor.Red);
                startingIngredientCount = 5;
                scorePerIngredient = 10;
                break;
            case 3:
                Collections.addAll(solution, CodeColor.Red, CodeColor.Yellow);
                Collections.addAll(availableDyes, CodeColor.Red, CodeColor.Yellow, CodeColor.Purple);
                startingIngredientCount = 9;
                scorePerIngredient = 6;
                break;
            case 4:
                Collections.addAll(solution, CodeColor.Green, CodeColor.Brown);
                Collections.addAll(availableDyes, CodeColor.Green, CodeColor.Brown);
                startingIngredientCount = 4;
                scorePerIngredient = 8;
                break;
            case 5:
                Collections.addAll(solution, CodeColor.Cyan, CodeColor.Purple, CodeColor.Brown);
                Collections.addAll(availableDyes, CodeColor.Cyan, CodeColor.Purple, CodeColor.Brown);
                startingIngredientCount = 12;
                scorePerIngredient = 7;
                break;
        }

        dyesOnScreen.clear();
        for (int i = 0; i < availableDyes.size(); i++) {
            for (int y = 0; y < startingIngredientCount; y++) {
                dyesOnScreen.add(availableDyes.get(i));
            }
        }
        Centuria.logger.info(dyesOnScreen.toString());

        MinigameMessagePacket msg = new MinigameMessagePacket();
		msg.command = "startLevel";
		msg.data = Integer.toString(level);
		plr.client.sendPacket(msg);
	}

    @MinigameMessage("guess")
    public void handleGuess(Player plr, XtReader rd) throws IOException {
        //Deserialize
        int levelTimer = rd.readInt();
        int codeSize = rd.readInt();
        
        ArrayList<CodeColor> sequence = new ArrayList<>();
        for (int i = 0; i < codeSize; i++) {
            CodeColor color = CodeColor.valueOf(rd.readInt());
            sequence.add(color);
        }
        Centuria.logger.info("time:" + levelTimer + " code:" + sequence.toString());

        //Handle

        //Remove used dyes
        for (int i = 0; i < sequence.size(); i++) {
            if (dyesOnScreen.contains(sequence.get(i))){
                dyesOnScreen.remove(sequence.get(i));
            }
        }
        
        //Get correct and wrong positions
        int correctPositons = 0;
        for(int i = 0; i < sequence.size(); i++) {
            if (sequence.get(i).equals(solution.get(i))){
                correctPositons++;
            }
        }
        int wrongPositions = sequence.size() - correctPositons;
        //Check if win
        boolean win = false;
        if (wrongPositions == 0){
            win = true;
        }
        //Check if lose
        boolean lose = false;
        if (!dyesOnScreen.containsAll(solution)){
            lose = true;
        }
        if (!win & !lose){
            multipleGuesses = true;
        }

        //Send hint
        XtWriter wr = new XtWriter();
        wr.writeInt(correctPositons); // CorrectPositions
        wr.writeInt(wrongPositions); // WrongPositions
        MinigameMessagePacket pk = new MinigameMessagePacket();
        pk.command = "hint";
        pk.data = wr.encode().substring(4);
        plr.client.sendPacket(pk);

        Centuria.logger.info("CorrectPositions:" + correctPositons + " WrongPositions:" + wrongPositions);
        Centuria.logger.info(dyesOnScreen.toString());
        
        if (win){
            //Calculate score
            int ingredientScore = dyesOnScreen.size() * scorePerIngredient;
            int firstGuessBonus = multipleGuesses ? 0 : 100;
            int totalScore = ingredientScore + firstGuessBonus;
            
            //Save progress (broken)
            // String data = "%xt%zs%9101%" + level + "%" + totalScore + "%";
		    // plr.client.getServer().handlePacket(data, plr.client);
            
            XtWriter wr1 = new XtWriter();
            wr1.writeInt(ingredientScore); // IngredientScore
            wr1.writeInt(0); // TimeScore
            wr1.writeInt(0); // LastIngredientBonus
            wr1.writeInt(firstGuessBonus); //firstGuessBonus
            wr1.writeString("null"); //?
            MinigameMessagePacket pk1 = new MinigameMessagePacket();
            pk1.command = "endLevelWin";
            pk1.data = wr1.encode().substring(4);
            plr.client.sendPacket(pk1);
        }

        if (lose) {
            XtWriter wr2 = new XtWriter();
            wr2.writeInt(solution.size()); // length
            solution.forEach((n) -> wr2.writeInt(n.getValue())); //code

            MinigameMessagePacket pk2 = new MinigameMessagePacket();
            pk2.command = "endLevelLose";
            pk2.data = wr2.encode().substring(4);
            plr.client.sendPacket(pk2);
        }
        
    }

    @Override
	public AbstractMinigame instantiate() {
		return new GameDoOrDye();
	}

}