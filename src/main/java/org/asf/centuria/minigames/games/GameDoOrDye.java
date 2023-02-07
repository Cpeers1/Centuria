package org.asf.centuria.minigames.games;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.asf.centuria.Centuria;
import org.asf.centuria.data.XtReader;
import org.asf.centuria.data.XtWriter;
import org.asf.centuria.entities.players.Player;
import org.asf.centuria.entities.uservars.UserVarValue;
import org.asf.centuria.enums.minigames.CodeColor;
import org.asf.centuria.minigames.AbstractMinigame;
import org.asf.centuria.minigames.MinigameMessage;
import org.asf.centuria.packets.xt.gameserver.inventory.InventoryItemDownloadPacket;
import org.asf.centuria.packets.xt.gameserver.inventory.InventoryItemPacket;
import org.asf.centuria.packets.xt.gameserver.minigame.MinigameCurrencyPacket;
import org.asf.centuria.packets.xt.gameserver.minigame.MinigameMessagePacket;
import org.asf.centuria.util.CombinationSum;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class GameDoOrDye extends AbstractMinigame {

    public int level;
    public ArrayList<CodeColor> solution = new ArrayList<>();
    public ArrayList<CodeColor> dyesOnScreen = new ArrayList<>();
    public ArrayList<CodeColor> availableDyes = new ArrayList<>();
    public int startingIngredientCount;
    public int scorePerIngredient;
    public boolean multipleGuesses;

    
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

        //reset
        availableDyes.clear();
        solution.clear();
        dyesOnScreen.clear();
        multipleGuesses = false;
        
        //load level def from json
        try {
            InputStream strm = InventoryItemDownloadPacket.class.getClassLoader().getResourceAsStream("minigames/dod_levels.json");
			JsonObject helper = JsonParser.parseString(new String(strm.readAllBytes(), "UTF-8")).getAsJsonArray().get(level).getAsJsonObject();
            strm.close();

            int codeLength = helper.get("codeLength").getAsInt();
            int colors = helper.get("colors").getAsInt();
            boolean allowRepeatColors = helper.get("allowRepeatColors").getAsBoolean();
            startingIngredientCount = helper.get("startingIngredientCount").getAsInt();
            scorePerIngredient = helper.get("scorePerIngredient").getAsInt();

            //load available dyes from sum
            int[] arr = {1,2,4,8,16,32,64,128,256};
            List<List<Integer>> result = new ArrayList<>(); 
            result = CombinationSum.Sum(arr, colors);
            List<Integer> finalResult = result.get(0);

            for(int i=0; i < finalResult.size(); i++) {
                CodeColor color = CodeColor.valueOf(finalResult.get(i));
                availableDyes.add(color);
            }

            //generate solution
            if (allowRepeatColors){
                for (int i = 0; i < codeLength; i++) {
                    int index = (int)(Math.random() * codeLength);
                    solution.add(availableDyes.get(index));
                }
            }
            else{
                ArrayList<CodeColor> used = new ArrayList<CodeColor>();
                while (solution.size() < codeLength) {
                    int index = (int)(Math.random() * codeLength);
                    if (used.contains(availableDyes.get(index)) == false){
                        solution.add(availableDyes.get(index));
                        used.add(availableDyes.get(index));
                    }
                }
            }
            Centuria.logger.debug("solution: " + solution);
        }
        catch (IOException e){
        }
        
        //add to screen
        for (int i = 0; i < availableDyes.size(); i++) {
            for (int y = 0; y < startingIngredientCount; y++) {
                dyesOnScreen.add(availableDyes.get(i));
            }
        }
        
        Centuria.logger.debug("on screen "+ dyesOnScreen.toString());
        Centuria.logger.debug("solution "+ solution.toString());
        

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
        Centuria.logger.debug("time:" + levelTimer + " code:" + sequence.toString());

        //Handle

        //Remove used dyes
        for (int i = 0; i < sequence.size(); i++) {
            if (dyesOnScreen.contains(sequence.get(i))){
                dyesOnScreen.remove(sequence.get(i));
            }
        }
        
        //Right ingredient right order
        int correctPositons = 0;
        for(int i = 0; i < sequence.size(); i++) {
            if (sequence.get(i).equals(solution.get(i))){
                correctPositons++;
            }
        }

        //Right ingredient wrong order
        int wrongPositions = 0;
        for(int i = 0; i < sequence.size(); i++) {
            if (!(sequence.get(i).equals(solution.get(i))) && solution.contains(sequence.get(i))){
                wrongPositions++;
            }
        }

        //Check if win
        boolean win = false;
        if (sequence.equals(solution)){
            win = true;
        }

        //Check if lose
        boolean lose = false;
        if (!dyesOnScreen.containsAll(solution)){
            lose = true;
        }

        //Send hint
        XtWriter wr = new XtWriter();
        wr.writeInt(correctPositons); // CorrectPositions
        wr.writeInt(wrongPositions); // WrongPositions
        MinigameMessagePacket pk = new MinigameMessagePacket();
        pk.command = "hint";
        pk.data = wr.encode().substring(4);
        plr.client.sendPacket(pk);

        Centuria.logger.debug("CorrectPositions:" + correctPositons + " WrongPositions:" + wrongPositions);
        Centuria.logger.debug(dyesOnScreen.toString());
        
        if (lose & !win) {
            XtWriter wr2 = new XtWriter();
            wr2.writeInt(solution.size()); // length
            solution.forEach((n) -> wr2.writeInt(n.getValue())); //code

            MinigameMessagePacket pk2 = new MinigameMessagePacket();
            pk2.command = "endLevelLose";
            pk2.data = wr2.encode().substring(4);
            plr.client.sendPacket(pk2);
        }
        
        if (win){
            // Unlock level
            UserVarValue unlock = plr.account.getSaveSpecificInventory().getUserVarAccesor().getPlayerVarValue(9100, 0);
            int value = 0;
            if (unlock != null){
                value = unlock.value;
            }
            if (level > value) {
                plr.account.getSaveSpecificInventory().getUserVarAccesor().setPlayerVarValue(9100, 0, level);
		    }
            
            //Calculate score
            int ingredientScore = dyesOnScreen.size() * scorePerIngredient;
            int firstGuessBonus = multipleGuesses ? 0 : 100;
            int lastIngredientBonus = lose ? 100 : 0;
            int totalScore = ingredientScore + firstGuessBonus + lastIngredientBonus;
            
            //Save score
            UserVarValue var = plr.account.getSaveSpecificInventory().getUserVarAccesor().getPlayerVarValue(9101, level);
            int value2 = 0;
            if (var != null){
                value2 = var.value;
            }
            if (totalScore > value2) {
                plr.account.getSaveSpecificInventory().getUserVarAccesor().setPlayerVarValue(9101, level, totalScore);
		    }
            // Update client
            InventoryItemPacket pkt = new InventoryItemPacket();
            pkt.item = plr.account.getSaveSpecificInventory().getItem("303");
            plr.client.sendPacket(pkt);
            
            //Send win
            XtWriter wr1 = new XtWriter();
            wr1.writeInt(ingredientScore); // IngredientScore
            wr1.writeInt(0); // TimeScore
            wr1.writeInt(lastIngredientBonus); // LastIngredientBonus
            wr1.writeInt(firstGuessBonus); //firstGuessBonus
            wr1.writeString("null"); //?
            MinigameMessagePacket pk1 = new MinigameMessagePacket();
            pk1.command = "endLevelWin";
            pk1.data = wr1.encode().substring(4);
            plr.client.sendPacket(pk1);
        }

        if (!lose & !win){
            multipleGuesses = true;
        }
        
    }

    @Override
	public AbstractMinigame instantiate() {
		return new GameDoOrDye();
	}
    
    


}