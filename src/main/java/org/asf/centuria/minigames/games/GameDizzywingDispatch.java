package org.asf.centuria.minigames.games;

import java.util.Queue;
import java.util.Random;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.asf.centuria.Centuria;
import org.asf.centuria.data.XtReader;
import org.asf.centuria.data.XtWriter;
import org.asf.centuria.entities.players.Player;
import org.asf.centuria.entities.uservars.UserVarValue;
import org.asf.centuria.minigames.AbstractMinigame;
import org.asf.centuria.minigames.MinigameMessage;
import org.asf.centuria.packets.xt.gameserver.inventory.InventoryItemDownloadPacket;
import org.asf.centuria.packets.xt.gameserver.inventory.InventoryItemPacket;
import org.asf.centuria.packets.xt.gameserver.minigame.MinigameMessagePacket;
import org.joml.Vector2i;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class GameDizzywingDispatch extends AbstractMinigame{

    private String currentGameUUID;
    public GameState gameState;
    public int level = 0;
    private int score = 0;
    private int moveCount = 0;
    private int dizzyBirdMeter = 0;

    static private JsonArray specialOrders;
    static private JsonArray specialOrderCountRanges;
    static private JsonArray levelRewards;
    static private JsonArray achievementPuzzles;
    static private JsonArray achievementToUserVarIndexList;

    static {
        // Load level info
		try {
			// Load the helper
			InputStream strm = InventoryItemDownloadPacket.class.getClassLoader()
					.getResourceAsStream("minigames/dizzywingdispatch.json");
			JsonObject helper = JsonParser.parseString(new String(strm.readAllBytes(), "UTF-8")).getAsJsonObject();
			strm.close();

			// Load all level data
			JsonObject componentJSON = helper.getAsJsonArray("components").get(9).getAsJsonObject()
                                        .getAsJsonObject("componentJSON");
			specialOrders = componentJSON.getAsJsonArray("specialOrders");
			specialOrderCountRanges = componentJSON.getAsJsonArray("specialOrderCountRanges");
			levelRewards = componentJSON.getAsJsonArray("levelRewards");
			achievementPuzzles = componentJSON.getAsJsonArray("achievementPuzzles");
			achievementToUserVarIndexList = componentJSON.getAsJsonArray("achievementPuzzles");
            
		} catch (IOException e) {
			// This is very bad, should not start allow the server to continue otherwise
			// things will break HARD
			throw new RuntimeException(e);
		}

        if (Centuria.debugMode) {
            DDVis vis = new DDVis();
            new Thread(() -> vis.frame.setVisible(true)).start();
        }
    }

    public class GameState {



        // helper classes

        public class GridCell {
            private int tileHealth;
            private TileType TileType;
            private BoosterType Booster;

            public GridCell(int health, TileType tileType, BoosterType booster){
                tileHealth = health;
                TileType = tileType;
                Booster = booster;
            }

            public void setTileType(TileType tileType){
                TileType = tileType;
            }

            public TileType getTileType(){
                return TileType;
            }

            public void setBooster(BoosterType booster){
                Booster = booster;
            }
            
            public BoosterType getBooster(){
                return Booster;
            }

            public void setHealth(Integer health){
                tileHealth = health;
            }
            
            public Integer getHealth(){
                return tileHealth;
            }

            public Boolean isBoosted(){
                return Booster != BoosterType.None;
            }
                 
        }

        public class LevelObjectives {
            private int objectivesTracker[][] = { {-1, -1, 0},
                                                {-1, -1, 1},
                                                {-1, -1, 2},
                                                {-1, -1, 3}};

            JsonObject specialOrderData;
            JsonObject specialOrderCountRangeData;

            Integer currScore = 0;

            public LevelObjectives(){
                newLevelNewObjectives();
            }

            public void newLevelNewObjectives(){

                currScore = 0;
                if(level != 0){

                    for(JsonElement ele : specialOrders){
                        JsonObject data = ele.getAsJsonObject();
                        if((level >= data.get("_fromLevelNumber").getAsInt() &&
                        level <= data.get("_toLevelNumber").getAsInt()) || 
                        data.get("_isToLevelInfinite").getAsBoolean()){
                            
                            specialOrderData = data;
                            break;
                        }
                    }

                    for(JsonElement ele : specialOrderCountRanges){
                        JsonObject data = ele.getAsJsonObject();
                        if((level >= data.get("_fromLevelNumber").getAsInt() &&
                        level <= data.get("_toLevelNumber").getAsInt()) || 
                        data.get("_isToLevelInfinite").getAsBoolean()){
                            
                            specialOrderCountRangeData = data;
                            break;
                        }
                    }
                } else {
                    specialOrderData = specialOrders.get(0).getAsJsonObject();
                    specialOrderCountRangeData = specialOrderCountRanges.get(0).getAsJsonObject();
                }

                initObjective(LevelObjectiveType.ScoreRequirement, scoreRequirement());

                if(!diceRoll("_regularLevelAppearancePercent")){

                    Integer clothing = 0;
                    Integer eggs = 0;

                    if(diceRoll("_accessoriesAppearancePercent")){
                        clothing = initObjective(LevelObjectiveType.ClothingLeft, "_minimumAccessoryCount", "_maximumAccessoryCount");
                    } else {
                        clearObjective(LevelObjectiveType.ClothingLeft);
                    }
                    
                    if (diceRoll("_eggsAppearancePercent")){
                        eggs = initObjective(LevelObjectiveType.EggsLeft, "_minimumEggRowCount", "_maximumEggRowCount");
                    } else {
                        clearObjective(LevelObjectiveType.EggsLeft);
                    }
                    
                    if (diceRoll("_limitedMovesAppearancePercent")){
                        initObjective(LevelObjectiveType.MovesLeft, "_minimumLimitedMovesOnlyCount", "_maximumLimitedMovesOnlyCount");
                    } else {
                        clearObjective(LevelObjectiveType.MovesLeft);
                    }

                    for(int y = gridSize.y-1; y >= 0; y--){
                        for(int x = gridSize.x-1; x >= 0; x--){
                            Vector2i curr = new Vector2i(x, y);
                            GridCell currCell = getCell(curr);
    
                            if(!currCell.isBoosted()) {
                                if(currCell.getTileType() != TileType.HatOrPurse && clothing > 0){
                                    currCell.setTileType(TileType.HatOrPurse);
                                    setCell(curr, currCell);
                                    clothing--;
                                } else if(currCell.getHealth() == 0 && eggs > 0){
                                    currCell.setHealth(1);
                                    setCell(curr, currCell);
                                    eggs--;
                                }
                            }
                        }
                    }
                    
                }

                
                
            }

            public Boolean isNextLevel(){
                trackEggsAndClothing();

                Boolean goToNextLevel = true;
                goToNextLevel &= isAchieved(LevelObjectiveType.ClothingLeft);
                goToNextLevel &= isAchieved(LevelObjectiveType.EggsLeft);
                goToNextLevel &= isAchieved(LevelObjectiveType.ScoreRequirement);
                goToNextLevel &= !hasRunOutOfMoves();
                goToNextLevel &= isObjective(LevelObjectiveType.ScoreRequirement);

                if(goToNextLevel){
                    level++;
                    newLevelNewObjectives();
                    if(level > 25 && !spawnTiles.contains(TileType.AquaBird)){
                        spawnTiles.add(TileType.AquaBird);
                    } else if (level > 75 && !spawnTiles.contains(TileType.BlueBird)){
                        spawnTiles.add(TileType.BlueBird);
                    } else if (level > 100 && !spawnTiles.contains(TileType.PinkBird)){
                        spawnTiles.add(TileType.PinkBird);
                    }
                }

                return goToNextLevel;
            }

            public void addScore(Integer increase){
                currScore += increase;
                updateObjective(LevelObjectiveType.ScoreRequirement, currScore);
            }

            public void takeMove(){
                if(isObjective(LevelObjectiveType.MovesLeft)){
                    updateObjectiveByChange(LevelObjectiveType.MovesLeft, 1);
                }
            }

            public boolean hasRunOutOfMoves(){
                if(isObjective(LevelObjectiveType.MovesLeft)){
                    return objectivesTracker[LevelObjectiveType.MovesLeft.ordinal()][1] < 0;
                } else {
                    return false;
                }
            }

            private void trackEggsAndClothing(){
                if(isObjective(LevelObjectiveType.EggsLeft)){
                    Integer numberOfEggs = 0;
                    for(int x = 0; x < gridSize.x; x++){
                        for(int y = 0; y < gridSize.y; y++){
                            Vector2i curr = new Vector2i(x, y);

                            if(getCell(curr).getHealth() > 0) {
                                numberOfEggs++;
                            }
                        }
                    }
                    updateObjectiveRemaining(LevelObjectiveType.EggsLeft, numberOfEggs);
                }

                if(isObjective(LevelObjectiveType.ClothingLeft)){
                    Integer numberOfClothing = 0;
                    for(int x = 0; x < gridSize.x; x++){
                        for(int y = 0; y < gridSize.y; y++){
                            Vector2i curr = new Vector2i(x, y);

                            if(getCell(curr).getTileType() == TileType.HatOrPurse) {
                                numberOfClothing++;
                            }
                        }
                    }
                    updateObjectiveRemaining(LevelObjectiveType.ClothingLeft, numberOfClothing);
                }
            }

            // helper functions

            private Boolean diceRoll(String attribute){
                return randomizer.nextInt(100) < specialOrderData.get(attribute).getAsInt();
            }
            
            private Boolean isObjective(LevelObjectiveType objectiveType) {
                return objectivesTracker[objectiveType.ordinal()][0] != -1;
            }
            
            private Boolean isAchieved(LevelObjectiveType objectiveType){
                if(objectivesTracker[objectiveType.ordinal()][0] == -1){
                    return true;
                } else if (objectivesTracker[objectiveType.ordinal()][0] <=
                objectivesTracker[objectiveType.ordinal()][1]) {
                    return true;
                } else{
                    return false;
                }
            }
            
            private Integer initObjective(LevelObjectiveType objectiveType, String minimum, String maximum){
                
                Integer goal = randomizer.nextInt(
                    specialOrderCountRangeData.get(minimum).getAsInt(),
                    specialOrderCountRangeData.get(maximum).getAsInt()+1);
                if(goal > 0){
                    objectivesTracker[objectiveType.ordinal()][0] = goal;
                    objectivesTracker[objectiveType.ordinal()][1] = 0;
                } else {
                    objectivesTracker[objectiveType.ordinal()][0] = -1;
                    objectivesTracker[objectiveType.ordinal()][1] = -1;
                }
                
                return objectivesTracker[objectiveType.ordinal()][0];
            }

            private Integer scoreRequirement(){

                Integer r = 50;  // round to the nearest r
                Integer s = 500;  // starting level score
                Float g = 2.5f; // growth rate
                Integer o = 125; // offset

                return (int) (r * Math.round((s * Math.log1p(g * (level + o) - g + 1f) + s) / r));
            }         

            private void initObjective(LevelObjectiveType objectiveType, Integer requirement){
                objectivesTracker[objectiveType.ordinal()][0] = requirement;
                objectivesTracker[objectiveType.ordinal()][1] = 0;
            }

            private void clearObjective(LevelObjectiveType objectiveType){
                objectivesTracker[objectiveType.ordinal()][0] = -1;
                objectivesTracker[objectiveType.ordinal()][1] = -1;
            }
            
            private void updateObjectiveByChange(LevelObjectiveType objectiveType, Integer value){
                objectivesTracker[objectiveType.ordinal()][1] += value;
            }

            private void updateObjectiveRemaining(LevelObjectiveType objectiveType, Integer value){
                objectivesTracker[objectiveType.ordinal()][1] = objectivesTracker[objectiveType.ordinal()][0] - value;
            }

            private void updateObjective(LevelObjectiveType objectiveType, Integer value){
                objectivesTracker[objectiveType.ordinal()][1] = value;
            }

        } 

        enum BoosterType
        {
            None,
            BuzzyBirdHorizontal,
            BuzzyBirdVertical,
            BoomBird,
            PrismPeacock
        }

        enum TileType {
            AquaBird,
            BlueBird,
            GreenBird,
            PinkBird,
            PurpleBird,
            RedBird,
            SnowyBird,
            YellowBird,
            HatOrPurse,
            None
        }

        enum LevelObjectiveType
        {
            ScoreRequirement,
            MovesLeft,
            EggsLeft,
            ClothingLeft
        }

        enum PuzzleObjectiveType
        {
            HighScore,
            TotalScore,
            MakeFlyers,
            MakeFlyers_SingleGame,
            MakeBombBirds,
            MakeBombBirds_SingleGame,
            MakePeacocks,
            MakePeacocks_SingleGame,
            ClearRedBirds,
            ClearRedBirds_SingleGame,
            ClearBlueBirds,
            ClearBlueBirds_SingleGame,
            ClearGreenBirds,
            ClearGreenBirds_SingleGame,
            ClearYellowBirds,
            ClearYellowBirds_SingleGame,
            ClearPinkBirds,
            ClearPinkBirds_SingleGame,
            ClearWhiteBirds,
            ClearWhiteBirds_SingleGame,
            ClearPurpleBirds,
            ClearPurpleBirds_SingleGame,
            ClearAquaBirds,
            ClearAquaBirds_SingleGame,
            CompleteOrders,
            CompleteOrders_SingleGame,
            CompleteSpecialOrders,
            CompleteSpecialOrders_SingleGame,
            CompleteRushHourOrders,
            CompleteRushHourOrders_SingleGame,
            CompleteShortStaffedOrders,
            CompleteShortStaffedOrders_SingleGame,
            CompleteComboOrders,
            CompleteComboOrders_SingleGame,
            ClearedWithPeacockRed,
            ClearedWithPeacockRed_SingleGame,
            ClearedWithPeacockBlue,
            ClearedWithPeacockBlue_SingleGame,
            ClearedWithPeacockGreen,
            ClearedWithPeacockGreen_SingleGame,
            ClearedWithPeacockYellow,
            ClearedWithPeacockYellow_SingleGame,
            ClearedWithPeacockPink,
            ClearedWithPeacockPink_SingleGame,
            ClearedWithPeacockWhite,
            ClearedWithPecockWhite_SingleGame,
            ClearedWithPeacockPurple,
            ClearedWithPeacockPurple_SingleGame,
            ClearedWithPeacockAqua,
            ClearedWithPeacockAqua_SingleGame,
            HatchPowerups,
            HatchPowerups_SingleGame,
            ComboFlyerBomb,
            ComboFlyerBomb_SingleGame,
            ComboFlyerPeacock,
            ComboFlyerPeacock_SingleGame,
            ComboBombPeacock,
            ComboBombPeacock_SingleGame,
            ComboFlyerFlyer,
            ComboFlyerFlyer_SingleGame,
            ComboBombBomb,
            ComboBombBomb_SingleGame,
            ComboPeacockPeacock,
            ComboPeacockPeacock_SingleGame
        }

        // initializing the object

        private GridCell[][] grid;
        private boolean toVisit[][];
        private boolean visited[][];
        private Vector2i gridSize;
        private List<TileType> spawnTiles;
        private Random randomizer;
        private Integer matchComboScore;
        public LevelObjectives objectives;

        public GameState(){
            gridSize = new Vector2i(9, 9);
            spawnTiles = new ArrayList<TileType>(Arrays.asList(
            TileType.GreenBird,
            TileType.PurpleBird,
            TileType.RedBird,
            TileType.SnowyBird,
            TileType.YellowBird));
            randomizer = new Random(currentGameUUID.hashCode());
            initializeGameBoard();
            floodFillClearVisited();
            objectives = new LevelObjectives();
        }



        // functions related to accessing or modifying game tiles

        public GridCell getCell(Vector2i pos)
        {
            if (pos.x >= 0 && pos.x < gridSize.x && pos.y >= 0 && pos.y < gridSize.y)
            {
                return grid[pos.x][pos.y];
            }
            return null;
        }

        public void setCell(Vector2i pos, GridCell cell){
            if (pos.x >= 0 && pos.x < gridSize.x && pos.y >= 0 && pos.y < gridSize.y && cell != null)
            {
                grid[pos.x][pos.y] = cell;
            }
        }

        private void clearCell(Vector2i pos, Boolean isScore, Boolean forceClear){
            
            if (getCell(pos) == null) {
                return;
            }

            if(forceClear){
                setCell(pos, new GridCell(0, TileType.None, BoosterType.None));
                return;
            } else if(getCell(pos).getHealth() > 0) {
                breakEggTile(pos);
            } else if(getCell(pos).getTileType() != TileType.HatOrPurse && getCell(pos).getBooster() != BoosterType.PrismPeacock) {
                setCell(pos, new GridCell(0, TileType.None, BoosterType.None));
            }

            if(isScore) {
                score += 30;
                dizzyBirdMeter += 30;
            }

            switch (getCell(pos).getBooster()) {
                case BuzzyBirdHorizontal: 
                    buzzyBirdHorizontalBehaviour(pos, 1);
                    break;
                case BuzzyBirdVertical: 
                    buzzyBirdVerticalBehaviour(pos, 1);
                    break;
                case BoomBird: 
                    boomBirdBehaviour(pos, 3);
                    break;
                default:
                    break;
            }
        }

        private void breakEggTile(Vector2i pos){
            GridCell eggTile = getCell(pos);
            if(eggTile != null){
                eggTile.setHealth(0);
                setCell(pos, eggTile);
                objectives.trackEggsAndClothing();
            }
        }



        // functions related to generating a game board

        private void clearGrid(){
            grid = new GridCell[gridSize.x][gridSize.y];
        }
        
        public int calculateBoardChecksum() // level of emulation accuracy uncertain
        {
            byte[] inArray = new byte[gridSize.x * gridSize.y];
            
            for(int x = 0; x < gridSize.x; x++){
                for(int y = 0; y < gridSize.y; y++){
                    inArray[x + gridSize.x * y] = gridCellByteValueForChecksum(new Vector2i(x, y));
                }
            }

            //Board checksum algorithm!
            String string1 = Base64.getEncoder().encodeToString(inArray);
            String string2 = currentGameUUID.toString();
            String string3 = ((Integer)(moveCount)).toString();
            return (string1 + string2 + string3).hashCode();
        }

        private byte gridCellByteValueForChecksum(Vector2i pos) // level of emulation accuracy uncertain
        {
            int byteValue = 0;
            GridCell cell = getCell(pos);

            Integer tileTypeValue = cell.getTileType().ordinal();

            if (cell.isBoosted()) {
                byteValue += (cell.isBoosted() ? 1 : 0) * 20 + tileTypeValue;
            } else {
                byteValue += tileTypeValue * 2 + cell.tileHealth;
            }

            return (byte)byteValue;
        }

        private void initializeGameBoard() // level of emulation accuracy uncertain
        {

            clearGrid();
            List<TileType> shuffledSpawnTiles = new ArrayList<TileType>(spawnTiles);

            for (int y = 0; y < gridSize.y; y++)
            {
                for (int x = 0; x < gridSize.x; x++)
                {
                    Collections.shuffle(shuffledSpawnTiles, randomizer);

                    for (TileType spawnTile : shuffledSpawnTiles)
                    {
                        if ((x < 2 || !(getCell(new Vector2i(x - 1, y)).TileType == spawnTile) || 
                            !(getCell(new Vector2i(x - 2, y)).TileType == spawnTile)) && 
                            (y < 2 || !(getCell(new Vector2i(x, y - 1)).TileType == spawnTile) || 
                            !(getCell(new Vector2i(x, y - 2)).TileType == spawnTile)))
                        {
                            setCell(new Vector2i(x, y), new GridCell(0, spawnTile, BoosterType.None));
                            break;
                        }
                    }
                }
            }
        }



        // functions related to responding to a move from the player

        public void calculateMove(Vector2i pos1, Vector2i pos2){

            moveCount++;
            objectives.takeMove();

            if(pos2.y != -1){   // if given two tiles as input

                GridCell cell1 = getCell(pos1); // swap the tiles
                setCell(pos1, getCell(pos2));
                setCell(pos2, cell1);
    
                floodFillClearVisited();    // reset the visited array
    
                // Guarantee pos1 to have a more powerful or as powerful booster as pos2
                if(getCell(pos2).getBooster().ordinal() > getCell(pos1).getBooster().ordinal()){
                    Vector2i temp = pos1;
                    pos1 = pos2;
                    pos2 = temp;
                }
    
                if(getCell(pos1).isBoosted() && getCell(pos2).isBoosted()){ // booster combo behaviours
                    
                    if((getCell(pos2).getBooster() == BoosterType.BuzzyBirdHorizontal || 
                        getCell(pos2).getBooster() == BoosterType.BuzzyBirdVertical) &&
                        getCell(pos1).getBooster() == BoosterType.BoomBird){    // Buzzy bird and boom bird
    
                        clearCell(pos1, false, true);
                        clearCell(pos2, true, true);
        
                        buzzyBirdHorizontalBehaviour(pos1, 3);
                        buzzyBirdVerticalBehaviour(pos1, 3);
    
                    } else if (getCell(pos1).getBooster() == BoosterType.BoomBird &&
                            getCell(pos2).getBooster() == BoosterType.BoomBird){    // Boom bird and boom bird
    
                        clearCell(pos1, false, true);
                        clearCell(pos2, true, true);
            
                        boomBirdBehaviour(pos1, 5);
    
                    } else if ((getCell(pos1).getBooster() == BoosterType.BuzzyBirdHorizontal || 
                                getCell(pos1).getBooster() == BoosterType.BuzzyBirdVertical) &&
                                (getCell(pos2).getBooster() == BoosterType.BuzzyBirdHorizontal || 
                                getCell(pos2).getBooster() == BoosterType.BuzzyBirdVertical)){  // Buzzy bird and buzzy bird
    
                        clearCell(pos1, false, true);
                        clearCell(pos2, true, true);
                
                        buzzyBirdHorizontalBehaviour(pos1, 1);
                        buzzyBirdVerticalBehaviour(pos1, 1);
    
                    } else if (getCell(pos1).getBooster() == BoosterType.PrismPeacock &&
                                (getCell(pos2).getBooster() == BoosterType.BuzzyBirdHorizontal || 
                                getCell(pos2).getBooster() == BoosterType.BuzzyBirdVertical)){  // Prism peacock and buzzy bird
    
                        TileType refType = getCell(pos2).TileType;
    
                        clearCell(pos1, false, true);
                        clearCell(pos2, true, true);
    
                        List<Vector2i> newBuzzyBirds = new ArrayList<>();
    
                        for(int x = 0; x < gridSize.x; x++){
                            for(int y = 0; y < gridSize.y; y++){
                                Vector2i curr = new Vector2i(x, y);
    
                                if(getCell(curr).getTileType() == refType && !getCell(curr).isBoosted()) {
                                    newBuzzyBirds.add(curr);
                                }
                            }
                        }
    
                        for (Vector2i newBuzzyBird : newBuzzyBirds) {
                            Boolean coinflip = randomizer.nextInt(2) == 1;
                            
                            if(coinflip) {
                                buzzyBirdHorizontalBehaviour(newBuzzyBird, 1);
                            } else {
                                buzzyBirdVerticalBehaviour(newBuzzyBird, 1);
                            }
                        }
    
                    } else if (getCell(pos1).getBooster() == BoosterType.PrismPeacock &&
                            (getCell(pos2).getBooster() == BoosterType.BoomBird)){  // Prism peacock and boom bird
    
                        TileType refType = getCell(pos2).TileType;
    
                        clearCell(pos1, false, true);
                        clearCell(pos2, true, true);
    
                        List<Vector2i> newBoomBirds = new ArrayList<>();
    
                        for(int x = 0; x < gridSize.x; x++){
                            for(int y = 0; y < gridSize.y; y++){
                                Vector2i curr = new Vector2i(x, y);
                                GridCell currCell = getCell(curr);
    
                                if(currCell.getTileType() == refType && !currCell.isBoosted()) {
                                    currCell.setBooster(BoosterType.BoomBird);  // presumably no need to check for tile health
                                    setCell(curr, currCell);
                                    newBoomBirds.add(curr);
                                }
                            }
                        }
    
                        Integer d = Math.floorDiv(3, 2);
    
                        for (Vector2i pos : newBoomBirds) {     // first explosions
                
                            for(int x = pos.x-d; x <= pos.x+d; x++){
                                for(int y = pos.y-d; y <= pos.y+d; y++){
        
                                    Vector2i curr = new Vector2i(x, y);
        
                                    if(getCell(curr) != null && !newBoomBirds.contains(curr) && getCell(curr).getTileType() != TileType.None) {
                                        clearCell(curr, true, false);
                                    }
                                }
                            }
                        }
                        fillGaps();
    
                        newBoomBirds.clear();
    
                        for(int x = 0; x < gridSize.x; x++){
                            for(int y = 0; y < gridSize.y; y++){
                                Vector2i curr = new Vector2i(x, y);
                                GridCell currCell = getCell(curr);
    
                                if(currCell.getTileType() == refType && currCell.getBooster() == BoosterType.BoomBird) {
                                    newBoomBirds.add(curr);
                                }
                            }
                        }
    
                        for (Vector2i pos : newBoomBirds) {    // second explosions
                
                            for(int x = pos.x-d; x <= pos.x+d; x++){
                                for(int y = pos.y-d; y <= pos.y+d; y++){
        
                                    Vector2i curr = new Vector2i(x, y);
                                    clearCell(curr, true, true);
                                }
                            }
                        }

                        fillGaps();
    
    
                    } else if (getCell(pos1).getBooster() == BoosterType.PrismPeacock &&
                                getCell(pos1).getBooster() == BoosterType.PrismPeacock){    // Prism peacock and prism peacock
                                clearCell(pos1, false, true);
                                clearCell(pos2, false, true);
    
                        for(int x = 0; x < gridSize.x; x++){
                            for(int y = 0; y < gridSize.y; y++){
                                Vector2i curr = new Vector2i(x, y);
                                clearCell(curr, true, false);
                            }
                        }

                        fillGaps();
                    }
    
                } else if(getCell(pos1).isBoosted() && !getCell(pos2).isBoosted()){    // single booster behaviours
                    
                    switch (getCell(pos1).getBooster()) {
                        case BuzzyBirdHorizontal: 
                            buzzyBirdHorizontalBehaviour(pos1, 1);
                            break;
                        case BuzzyBirdVertical: 
                            buzzyBirdVerticalBehaviour(pos1, 1);
                            break;
                        case BoomBird: 
                            boomBirdBehaviour(pos1, 3);
                            break;
                        case PrismPeacock: 
                            prismPeacockBehaviour(pos1, getCell(pos2).getTileType());
                            break;
                        default:
                            break;
                    }
                }

            } else {    // only one tile as input

                switch (getCell(pos1).getBooster()) {
                    case BuzzyBirdHorizontal: 
                        buzzyBirdHorizontalBehaviour(pos1, 1);
                        break;
                    case BuzzyBirdVertical: 
                        buzzyBirdVerticalBehaviour(pos1, 1);
                        break;
                    case BoomBird: 
                        boomBirdBehaviour(pos1, 3);
                        break;
                    default:
                        break;
                }

            }


            matchComboScore = 0;

            while(findAndClearMatches(pos1, pos2)){ // for as long as there are still matches on the game board

                fillGaps();
                clearHats();

            }
                
        }

        // functions related to how boosters clear tiles

        private void buzzyBirdHorizontalBehaviour(Vector2i pos, Integer size) {
            
            clearCell(pos, false, true);

            Integer d = Math.floorDiv(size, 2);
            
            for(int y = pos.y-d; y <= pos.y+d; y++){
                for(int x = 0; x < gridSize.x; x++){
                    Vector2i curr = new Vector2i(x, y);
                    if(getCell(curr) != null){
                        if(getCell(curr).getBooster() != BoosterType.BuzzyBirdHorizontal){
                            clearCell(curr, true, false);
                        } else {
                            clearCell(curr, true, true);
                            buzzyBirdVerticalBehaviour(curr, 1);
                        }
                    }
                }
            }
            fillGaps();
        }
        
        private void buzzyBirdVerticalBehaviour(Vector2i pos, Integer size) {
            
            clearCell(pos, false, true);
            
            Integer d = Math.floorDiv(size, 2);
            
            for(int x = pos.x-d; x <= pos.x+d; x++){
                for(int y = 0; y < gridSize.y; y++){
                    Vector2i curr = new Vector2i(x, y);
                    if(getCell(curr) != null){
                        if(getCell(curr).getBooster() != BoosterType.BuzzyBirdVertical){
                            clearCell(curr, true, false);
                        } else {
                            clearCell(curr, true, true);
                            buzzyBirdHorizontalBehaviour(curr, 1);
                        }
                    }
                }
            }
            fillGaps();
        }
        
        private void boomBirdBehaviour(Vector2i pos, Integer size) {
            
            clearCell(pos, false, true);

            Integer d = Math.floorDiv(size, 2);

            for(int x = pos.x-d; x <= pos.x+d; x++){
                for(int y = pos.y-d; y <= pos.y+d; y++){

                    Vector2i curr = new Vector2i(x, y);

                    if(x != pos.x && y != pos.y) {
                        clearCell(curr, true, false);
                    }
                }
            }
            fillGaps();
            for(int x = pos.x-d; x <= pos.x+d; x++){
                for(int y = pos.y-d-1; y <= pos.y+d-1; y++){
                    Vector2i curr = new Vector2i(x, y);
                    clearCell(curr, true, false);
                }
            }
            fillGaps();
        }

        private void prismPeacockBehaviour(Vector2i pos, TileType refType) {

            clearCell(pos, false, true);

            for(int y = 0; y < gridSize.y; y++){
                for(int x = 0; x < gridSize.x; x++){
                    Vector2i curr = new Vector2i(x, y);
                    GridCell currCell = getCell(curr);

                    if(currCell.TileType == refType && !currCell.isBoosted()) {
                        clearCell(curr, true, false);
                    }
                }
            }
            fillGaps();
        }

        // functions related to the flood fill algorithm used to clear groups of tiles

        public Boolean floodFillIsNeighbourCell(Vector2i pos1, Vector2i pos2, Boolean checkMatch){
            GridCell cell1 = getCell(pos1);
            GridCell cell2 = getCell(pos2);

            if(cell1 == null || cell2 == null){
                return false;
            } else if (cell1.getTileType() == cell2.getTileType() && 
                        cell1.getTileType() != TileType.HatOrPurse){
                         //!cell1.isBoosted() && !cell2.isBoosted() &&
                         //cell1.getHealth() == 0 && cell1.getHealth() == 0
                
                return checkMatch ? floodFillGetToVisit(pos2) && floodFillGetToVisit(pos2) : true;
            }

            return false;
        }

        // false = don't visit this cell/cell has been visited
        // true = visit this cell/cell has not been visited
        public void floodFillSetToVisit(Vector2i pos){
            toVisit[pos.x][pos.y] = true;
            visited[pos.x][pos.y] = true;
        }

        public boolean floodFillGetToVisit(Vector2i pos){ // also returns true if uninitialized
            if (pos.x >= 0 && pos.x < gridSize.x && pos.y >= 0 && pos.y < gridSize.y)
            {
                return toVisit[pos.x][pos.y];
            }
            return false;
        }

        public void floodFillSetVisited(Vector2i pos){
            visited[pos.x][pos.y] = false;
        }

        public boolean floodFillGetVisited(Vector2i pos){
            if (pos.x >= 0 && pos.x < gridSize.x && pos.y >= 0 && pos.y < gridSize.y)
            {
                return visited[pos.x][pos.y];
            }
            return false;
        }

        public void floodFillClearVisited(){
            toVisit = new boolean[gridSize.x][gridSize.y];
            visited = new boolean[gridSize.x][gridSize.y];
        }

        public void floodFill(Vector2i pos, Integer matchType, Vector2i swap1, Vector2i swap2){
            
            TileType refType = getCell(pos).getTileType();
            BoosterType refBooster = BoosterType.None;
            
            List<Vector2i> connectedNodes = new LinkedList<>();
            Queue<Vector2i> floodFillQueue = new LinkedList<>();
            floodFillQueue.add(pos);

            Integer matchTileSize = matchType;

            while(!floodFillQueue.isEmpty()){
                Vector2i curr = floodFillQueue.poll();

                if(getCell(curr) == null) continue;
                if(!floodFillGetVisited(curr)) continue;
                if(getCell(pos).getTileType() != getCell(curr).getTileType()) continue;
                //if(getCell(pos).getBooster() != getCell(curr).getBooster()) continue;
                //if(getCell(pos).getHealth() > 0 && getCell(curr).getHealth() > 0) continue;

                floodFillSetVisited(curr);
                connectedNodes.add(curr);
                
                floodFillQueue.add(new Vector2i(curr.x-1, curr.y));
                floodFillQueue.add(new Vector2i(curr.x+1, curr.y));
                floodFillQueue.add(new Vector2i(curr.x, curr.y-1));
                floodFillQueue.add(new Vector2i(curr.x, curr.y+1));
                

                // detect tiles connected to other tiles in an L or T shape

                Integer horizontalConnections = 0;
                Integer verticalConnections = 0;

                if(floodFillIsNeighbourCell(curr, new Vector2i(curr.x-1, curr.y), true)) horizontalConnections++; 
                if(floodFillIsNeighbourCell(curr, new Vector2i(curr.x+1, curr.y), true)) horizontalConnections++; 
                if(floodFillIsNeighbourCell(curr, new Vector2i(curr.x, curr.y-1), true)) verticalConnections++; 
                if(floodFillIsNeighbourCell(curr, new Vector2i(curr.x, curr.y+1), true)) verticalConnections++; 

                if((horizontalConnections == 1 && verticalConnections == 1) || 
                    (horizontalConnections == 2 && verticalConnections == 1) || 
                    (horizontalConnections == 1 && verticalConnections == 2) &&
                    matchTileSize != 5) {   // do not change to boom bird if a prism peacock would form
                        matchTileSize = 6;
                    }
            }
            
            switch(matchTileSize) {
                case 3:
                    scoreCombo(30);
                    break;
                case 4:
                    refBooster = randomizer.nextInt(2) == 0 ? BoosterType.BuzzyBirdHorizontal : BoosterType.BuzzyBirdVertical;
                    scoreCombo(150);
                    break;
                case 5:
                    refBooster = BoosterType.PrismPeacock;
                    scoreCombo(150);
                    break;
                case 6:
                    refBooster = BoosterType.BoomBird;
                    scoreCombo(150);
                    break;
                default:
                    break;
            }

           
            for(Vector2i node : connectedNodes){
                clearCell(node, false, false);

                breakEggTile(new Vector2i(node.x-1, node.y));   // break the eggs surrounding a match
                breakEggTile(new Vector2i(node.x+1, node.y));
                breakEggTile(new Vector2i(node.x, node.y-1));
                breakEggTile(new Vector2i(node.x, node.y+1));
            }

            if(refBooster != BoosterType.None){

                if(refBooster == BoosterType.PrismPeacock){
                    refType = TileType.None;
                }

                if(connectedNodes.contains(swap1)){
                    setCell(swap1, new GridCell(0, refType, refBooster));
                } else if(connectedNodes.contains(swap2)){
                    setCell(swap2, new GridCell(0, refType, refBooster));
                } else {
                    setCell(pos, new GridCell(0, refType, refBooster));
                }
            }

        }

        // functions that clear or fill tiles by iterating through the entire board with nested for loops instead of flood fill

        private void clearHats() {
            for(int x = 0; x < gridSize.x; x++){
                for(int y = 0; y < gridSize.y; y++){ // for each column
                    Vector2i curr = new Vector2i(x, y);
                    if(getCell(curr).getTileType() == TileType.HatOrPurse){
                        clearCell(curr, false, true);
                    } else {
                        break;
                    }
                }
            }

            fillGaps();
            objectives.trackEggsAndClothing();
        }

        public void fillGaps(){
            for(int x = 0; x < gridSize.x; x++){
                Integer noGapsInColumn = 0;
                for(int y = 0; y < gridSize.y; y++){ // for each column
                    GridCell currCell = getCell(new Vector2i(x, y));
                    if(currCell.TileType == TileType.None && currCell.Booster == BoosterType.None){
                        noGapsInColumn++;
                    } else {
                        setCell(new Vector2i(x, y - noGapsInColumn), currCell); // move all the tiles down to eliminate gaps
                    }
                }
                for(int y = gridSize.y; y >= gridSize.y-noGapsInColumn; y--){
                    setCell(new Vector2i(x, y), new GridCell(0, spawnTiles.get(randomizer.nextInt(spawnTiles.size())), BoosterType.None)); // now fill the gaps
                }
            }
        }
              
        // identifies continuous lines of tiles with the same color, main function called by calculateMove

        public Boolean findAndClearMatches(Vector2i pos1, Vector2i pos2){ // Returns true if a match is found.

            floodFillClearVisited();
            
            Boolean isMatch = false;

            Map<Vector2i, Integer> matchType = new HashMap<>();

            // find all vertical matches
            for(int x = 0; x < gridSize.x; x++){
                Integer sameTiles = 1;
                for(int y = 0; y < gridSize.y; y++){ // for each column
                    if(floodFillIsNeighbourCell(new Vector2i(x, y), new Vector2i(x, y+1), false)){ // keep track of number of tiles with the same tile type next to each other
                        sameTiles++;
                    } else if (sameTiles > 2) {
                        isMatch = true;
                        
                        for(int backtrack = y-sameTiles+1; backtrack <= y; backtrack++){
                            Vector2i back = new Vector2i(x, backtrack);

                            floodFillSetToVisit(back);
                            matchType.put(back, sameTiles);
                        }
                        sameTiles = 1;
                    } else {
                        sameTiles = 1;
                    }
                }
            }
            
            // find all horizontal matches
            for(int y = 0; y < gridSize.y; y++){ // for each row
                Integer sameTiles = 1;
                for(int x = 0; x < gridSize.x; x++){
                if(floodFillIsNeighbourCell(new Vector2i(x+1, y), new Vector2i(x, y), false)){ // keep track of number of tiles with the same tile type next to each other
                        sameTiles++;
                    } else if (sameTiles > 2) {
                        isMatch = true;
                        
                        for(int backtrack = x-sameTiles+1; backtrack <= x; backtrack++){
                            Vector2i back = new Vector2i(backtrack, y);

                            floodFillSetToVisit(back);
                            matchType.put(back, sameTiles);
                        }
                        sameTiles = 1;
                    } else {
                        sameTiles = 1;
                    }
                }
            }

            // clear all matches
            for(int x = 0; x < gridSize.x; x++){
                for(int y = 0; y < gridSize.y; y++){
                    Vector2i curr = new Vector2i(x, y);

                    if(floodFillGetVisited(curr)) {
                        floodFill(curr, matchType.get(curr), pos1, pos2);
                    }
                }
            }

            return isMatch;
        }



        // functions related to scores and level progression

        private void scoreCombo(int scoreIncrease) {
            matchComboScore += scoreIncrease;

            dizzyBirdMeter += scoreIncrease / 30;
            score += matchComboScore;
            objectives.addScore(matchComboScore);
        }

        public Boolean updateLevel(){
            return objectives.isNextLevel();
        }

        public List<int[]> getObjectiveProgress(){
            objectives.trackEggsAndClothing();
            List<int[]> progress = new ArrayList<>();
            for(LevelObjectiveType objectiveType : LevelObjectiveType.values()){
                int[] objectiveData = objectives.objectivesTracker[objectiveType.ordinal()];
                if(objectiveData[0] != -1){
                    progress.add(objectiveData);
                }
            }
            return progress;
        }

        public Boolean runOutOfMoves(){
            return objectives.hasRunOutOfMoves();
        }

        


        // functions related to updating the board when a syncClient message is sent

        public String toBase64String(){
            byte[] inArray = new byte[gridSize.x * gridSize.y];
            
            for(int x = 0; x < gridSize.x; x++){
                for(int y = 0; y < gridSize.y; y++){
                    inArray[x + gridSize.x * y] = gridCellByteValueForBase64String(new Vector2i(x, y));
                }
            }

            return Base64.getEncoder().encodeToString(inArray);
        }

        public byte gridCellByteValueForBase64String(Vector2i pos)
        {
            int byteValue = 0;
            GridCell cell = getCell(pos);

            if(cell.TileType == TileType.HatOrPurse){
                byteValue = 18;
            } else if (cell.Booster == BoosterType.PrismPeacock){
                byteValue = 88;
            } else {
                byteValue += ((cell.Booster == BoosterType.None ? 2 : 1) * cell.TileType.ordinal() + (cell.tileHealth == 0 ? 0 : 1));
                byteValue += cell.Booster.ordinal() * 20;
            }
            
            
            return (byte)byteValue;
        }

        public void scrambleTiles() {
            List<GridCell> scrambledTiles = new ArrayList<>();

            for(int x = 0; x < gridSize.x; x++){
                for(int y = 0; y < gridSize.y; y++){  
                    Vector2i curr = new Vector2i(x, y);
                    scrambledTiles.add(getCell(curr));
                }
            }

            Collections.shuffle(scrambledTiles, randomizer);

            Integer tileNumber = 0;
            for(GridCell tile : scrambledTiles){
                setCell(new Vector2i(tileNumber % gridSize.y, tileNumber / gridSize.y), tile);
                tileNumber++;
            }

            while(true){ // copied over from the calculateMoves function

                Boolean isMatches = findAndClearMatches(new Vector2i(-1, -1), new Vector2i(-1, -1)); // blank input
                fillGaps();
                clearHats();
                fillGaps(); // replace tiles marked as having being cleared

                if(!isMatches){
                    break;
                }

            }
        }
    }

  
    // functions related to saving the saved game user variable to the player inventory.

    private void saveSavedGameUserVar(Player player) {
        /*
        `persistentAchievementDataUserVarDefId`: `13392`,
        `puzzleRedemptionStatusUserVarDefId`: `13404`,
        `puzzlePieceRedemptionStatusUserVarDefId`: `13405`,
        `savedGameUserVarDefId`: `30613`,
        */

        // write into the player inventory the number of moves made in the level, send inventory item packet
        player.account.getSaveSpecificInventory().getUserVarAccesor().setPlayerVarValue(30613, level, moveCount);
        // write into the player inventory the current score
        player.account.getSaveSpecificInventory().getUserVarAccesor().setPlayerVarValue(13392, 0, score);
        // write into the player inventory the highest score
        UserVarValue highestScore = player.account.getSaveSpecificInventory().getUserVarAccesor().getPlayerVarValue(13392, 1);
        player.account.getSaveSpecificInventory().getUserVarAccesor().setPlayerVarValue(13392, 1, Math.max(highestScore.value, score));
    }

    private void resetSavedGameUserVar(Player player) {

        // reset these values as a new game has been started
        level = 0;
        score = 0;
        moveCount = 0;
        dizzyBirdMeter = 0;
        
        // reset the saved game.
        player.account.getSaveSpecificInventory().getUserVarAccesor().deletePlayerVar(30613);
        // there is no level 0 and it is counted as having 1 move
        player.account.getSaveSpecificInventory().getUserVarAccesor().setPlayerVarValue(30613, 0, 1);
        // reset the recorded current score
        player.account.getSaveSpecificInventory().getUserVarAccesor().setPlayerVarValue(13392, 0, 0);
        // check if the highest score value is initialized, set it to zero if not
        UserVarValue highestScore = player.account.getSaveSpecificInventory().getUserVarAccesor().getPlayerVarValue(13392, 1);
        if(highestScore == null){
            player.account.getSaveSpecificInventory().getUserVarAccesor().setPlayerVarValue(13392, 1, 0);
        }
    }

    private void loadSavedGameUserVar(Player player) {

        // retrieve the data from the player inventory
        UserVarValue[] savedGame = player.account.getSaveSpecificInventory().getUserVarAccesor().getPlayerVarValue(30613);
        UserVarValue prevScore = player.account.getSaveSpecificInventory().getUserVarAccesor().getPlayerVarValue(13392, 0);

        // load in the values associated with a previous game
        if(savedGame != null){
            level = Math.max(1, savedGame.length-1);
            score = prevScore.value;
            moveCount = savedGame[savedGame.length-1].value;
            dizzyBirdMeter = 0;
        }
    }


    // functions that implement the game's protocols.

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
    public void startGame(Player player, XtReader rd){
        
        resetSavedGameUserVar(player);
        
        // The GUID is used as the seed for the random number generator.
        currentGameUUID = UUID.randomUUID().toString();
        gameState = new GameState();

        // the format of the minigame message response packet
        XtWriter mmData = new XtWriter();
        mmData.writeString(currentGameUUID);
        mmData.writeInt(gameState.calculateBoardChecksum());
        mmData.writeInt(level);
        mmData.writeInt(0); // not sure
        mmData.writeInt(score);
        mmData.writeInt(0); // not sure

        // send response packet
        MinigameMessagePacket mm = new MinigameMessagePacket();
		mm.command = "startGame";
		mm.data = mmData.encode().substring(4);
		player.client.sendPacket(mm);

        // load in server-generated game board
        syncClient(player, rd);

    }

    @MinigameMessage("move")
	public void move(Player player, XtReader rd) {

        int clientChecksum = rd.readInt();

        // process move sent by client
        gameState.calculateMove(new Vector2i(rd.readInt(), rd.readInt()), new Vector2i(rd.readInt(), rd.readInt()));
        // sync client if sever checksum differs from client checksum (always occurs at current)
        if (clientChecksum != gameState.calculateBoardChecksum()) {
            syncClient(player, rd);
        }

        // save score and number of moves made on this level
        saveGame(player, rd);
        
        // check if level should increase
        if (gameState.updateLevel()) {
            moveCount = 0;
            saveGame(player, rd);
            goToLevel(player);
        }

    }

	private void goToLevel(Player player) {
       
        // packet data
        XtWriter mmData = new XtWriter();
        mmData.writeInt(level);
        mmData.writeInt(0); // not sure
        mmData.writeInt(score);
        mmData.writeInt(0); // not sure

        // send packet
        MinigameMessagePacket mm = new MinigameMessagePacket();
		mm.command = "goToLevel";
		mm.data = mmData.encode().substring(4);
		player.client.sendPacket(mm);

    }

    @MinigameMessage("dizzyBird")
    public void dizzyBird(Player player, XtReader rd) {
        dizzyBirdMeter = 0; // reset the meter at the right-hand side of the screen
        gameState.scrambleTiles();
        syncClient(player, rd);
    }

    @MinigameMessage("continueGame")
	public void continueGame(Player player, XtReader rd) {
        
        // send start game client with previous values
        loadSavedGameUserVar(player);

        // The GUID is used as the seed for the random number generator.
        currentGameUUID = UUID.randomUUID().toString();
        gameState = new GameState();

        // the format of the minigame message response packet
        XtWriter mmData = new XtWriter();
        mmData.writeString(currentGameUUID);
        mmData.writeInt(gameState.calculateBoardChecksum());
        // it seems the game cannot start unless these specific values are used
        mmData.writeInt(1);
        mmData.writeInt(0);
        mmData.writeInt(500);
        mmData.writeInt(0);

        // send response packet
        MinigameMessagePacket mm = new MinigameMessagePacket();
		mm.command = "startGame";
		mm.data = mmData.encode().substring(4);
		player.client.sendPacket(mm);

        // load in server-generated game board
        syncClient(player, rd);
    }


    @MinigameMessage("saveGame")
	public void saveGame(Player player, XtReader rd) {

        saveSavedGameUserVar(player);

        if (player.client != null && player.client.isConnected()) {
            // Send to client
            InventoryItemPacket pk = new InventoryItemPacket();
            pk.item = player.account.getSaveSpecificInventory().getItem("303");
            player.client.sendPacket(pk);
        }
    }



    @MinigameMessage("redeemPiece")
	public void redeemPiece(Player player, XtReader rd) {

    }

    @MinigameMessage("syncClient")
	public void syncClient(Player player, XtReader rd) {
        XtWriter mmData = new XtWriter();
        mmData.writeInt(moveCount);
        mmData.writeInt(level);
        mmData.writeInt(score);
        mmData.writeInt(dizzyBirdMeter);
        mmData.writeString(gameState.toBase64String());

        
        List<int[]> objectiveProgress = gameState.getObjectiveProgress();
        
        if(!gameState.runOutOfMoves()){
            mmData.writeInt(objectiveProgress.size());
            for(int[] objectiveData : objectiveProgress){
                mmData.writeInt(objectiveData[2]);
                mmData.writeInt(objectiveData[0]);
                mmData.writeInt(objectiveData[1]);
            }
        } else {
            mmData.writeInt(2);

            mmData.writeInt(objectiveProgress.get(0)[2]);
            mmData.writeInt(objectiveProgress.get(0)[0]);
            mmData.writeInt(objectiveProgress.get(0)[1]);

            mmData.writeInt(1);
            mmData.writeInt(-1);
            mmData.writeInt(-1);
        }

/*  example level objective
        mmData.writeInt(4); // number of elements, only 2 goals at once

        mmData.writeInt(0); // increase score bar, required for multiple goals
        mmData.writeInt(500);   // level score requirement
        mmData.writeInt(250);   // current score achieved in level

        mmData.writeInt(1); // moves left
        mmData.writeInt(10);
        mmData.writeInt(0);

        mmData.writeInt(2); // eggs left
        mmData.writeInt(17); // where the number is first - second
        mmData.writeInt(0);

        mmData.writeInt(3); // items left
        mmData.writeInt(23);
        mmData.writeInt(0);
*/
        MinigameMessagePacket mm = new MinigameMessagePacket();
		mm.command = "syncClient";
		mm.data = mmData.encode().substring(4);
		player.client.sendPacket(mm);
    }
    
}
