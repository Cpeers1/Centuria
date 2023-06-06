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
import org.asf.centuria.packets.xt.gameserver.minigame.MinigamePrizePacket;
import org.asf.centuria.interactions.modules.ResourceCollectionModule;
import org.asf.centuria.interactions.modules.resourcecollection.rewards.LootInfo;
import org.asf.centuria.levelevents.LevelEvent;
import org.asf.centuria.levelevents.LevelEventBus;
import org.joml.Vector2i;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class GameDizzywingDispatch extends AbstractMinigame{

    static private JsonArray specialOrders;
    static private JsonArray specialOrderCountRanges;
    static private JsonArray levelRewards;
    static private JsonArray puzzleRewards;
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
			specialOrders = helper.getAsJsonArray("specialOrders");
			specialOrderCountRanges = helper.getAsJsonArray("specialOrderCountRanges");
			levelRewards = helper.getAsJsonArray("levelRewards");
			puzzleRewards = helper.getAsJsonArray("puzzleRewards");
			achievementToUserVarIndexList = helper.getAsJsonArray("achievementToUserVarIndexList");
            
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

    // class fields

    // constants used in the game protocol
    public int level = 0;
    private int score = 0;
    private int moveCount = 0;
    private int dizzyBirdMeter = 0;

    // game board data
    public Grid grid;

    // source of all randomness in the game
    private String currentGameUUID;
    private Random randomizer;
    
    // used to access game logic related functions
    public LevelObjectives objectives;
    public Checksums checksums;
    public GenerateBoard generateBoard;
    public CalculateMoves calcMoves;
    public BoosterBehvaiours boosters;
    public Rewards rewards;

    public GameDizzywingDispatch(){
        currentGameUUID = UUID.randomUUID().toString();
        randomizer = new Random(currentGameUUID.hashCode());

        grid = new Grid();
        checksums = new Checksums();
        generateBoard = new GenerateBoard();
        calcMoves = new CalculateMoves();
        boosters = new BoosterBehvaiours();
        rewards = new Rewards();
        
        generateBoard.initializeGameBoard();
        objectives = new LevelObjectives();
    }



    // helper classes

    public class GridCell 
    {
        private int tileHealth;
        private TileColor color;
        private BoosterType booster;

        public GridCell(int health, TileColor tileColor, BoosterType boosterType){
            tileHealth = health;
            color = tileColor;
            booster = boosterType;
        }

        public void setColor(TileColor tileType){
            color = tileType;
        }

        public TileColor getColor(){
            return color;
        }

        public void setBooster(BoosterType boosterType){
            booster = boosterType;
        }
        
        public BoosterType getBooster(){
            return booster;
        }

        public void setHealth(Integer health){
            tileHealth = health;
        }
        
        public Integer getHealth(){
            return tileHealth;
        }

        public void setEmpty(){
            tileHealth = 0;
            color = TileColor.None;
            booster = BoosterType.None; 
        }

        public Boolean getEmpty(){
            return color == TileColor.None &&  booster == BoosterType.None;
        }
        
        // Convenience functions

        public Boolean isBoosted(){
            return booster != BoosterType.None;
        }

        public Boolean isBuzzyBird(){
            return booster == BoosterType.BuzzyBirdHorizontal || 
                booster == BoosterType.BuzzyBirdVertical;
        }

        public void setPos(Vector2i newPosition){
            grid.setCell(newPosition, this);
        }
        
        public Vector2i getPos(){
            return grid.getCellPos(this);
        }

        public Integer getX(){
            return grid.getCellX(this);
        }

        public Integer getY(){
            return grid.getCellY(this);
        }

        public Integer getIndex(){
            return grid.getCellIndex(this);
        }
  
    }

    public class Grid {
        // game board data
        private GridCell[][] grid;
        private Map<GridCell, Vector2i> cellToPos;
        private Vector2i gridSize;

        public Grid()
        {
            gridSize = new Vector2i(9, 9);
            grid = new GridCell[gridSize.x][gridSize.y];
            cellToPos = new HashMap<>();
        }

        public GridCell getCell(Vector2i pos)
        {
            if (pos.x >= 0 && pos.x < gridSize.x && pos.y >= 0 && pos.y < gridSize.y)
            {
                return grid[pos.x][pos.y];
            }
            return null;
        }

        public void setCell(Vector2i pos, GridCell cell)
        {
            if (pos.x >= 0 && pos.x < gridSize.x && pos.y >= 0 && pos.y < gridSize.y && cell != null)
            {
                grid[pos.x][pos.y] = cell;
                cellToPos.put(cell, pos);
            }
        }

        private void clearCell(GridCell cell, Boolean isScore, Boolean forceClear)
        {
            clearCell(cell, isScore, forceClear, false);
        }

        private void clearCell(GridCell cell, Boolean isScore, Boolean forceClear, Boolean isClearedByPeacock)
        {
            
            if(cell == null){
                return;
            }
            if(isScore){
                objectives.fixedUpdateScore(30);
            }
            if(forceClear){
                cell.setEmpty();
                return;
            }
            if(cell.getHealth() > 0) {
                breakEggTile(cell);
                return;
            }
            if(cell.getColor() == TileColor.HatOrPurse || cell.getBooster() == BoosterType.PrismPeacock){
                return;
            }
            
            cell.setEmpty();

            switch (cell.getBooster()) {
                case BuzzyBirdHorizontal: 
                    boosters.buzzyBirdHorizontalBehaviour(cell, 1);
                    break;
                case BuzzyBirdVertical: 
                    boosters.buzzyBirdVerticalBehaviour(cell, 1);
                    break;
                case BoomBird: 
                    boosters.boomBirdBehaviour(cell, 3);
                    break;
                default:
                    rewards.clearCellUpdateObjective(cell);
                    if(isClearedByPeacock){
                        rewards.clearedWithPeacockUpdateObjective(cell);
                    }
                    break;
            }
        }

        private void breakEggTile(GridCell eggTile){
            if(eggTile != null){
                eggTile.setHealth(0);
                objectives.trackEggsAndClothing();
            }
        }

        private void clearGrid(){
            grid = new GridCell[gridSize.x][gridSize.y];
            cellToPos = new HashMap<>();
        }

        // convenience functions

        public List<GridCell> verticalIterate()
        {
            List<GridCell> list = new ArrayList<>();
            for(int x = 0; x < gridSize.x; x++){
                for(int y = 0; y < gridSize.y; y++){
                    list.add(getCell(new Vector2i(x, y)));
                }
            }
            return list;
        }

        public List<List<GridCell>> verticalNestedIterate()
        {
            List<List<GridCell>> nestedList = new ArrayList<>();
            for(int x = 0; x < gridSize.x; x++){
                List<GridCell> List = new ArrayList<>();
                for(int y = 0; y < gridSize.y; y++){
                    List.add(getCell(new Vector2i(x, y)));
                }
                nestedList.add(List);
            }
            return nestedList;
        }

        public List<GridCell> horizontalTopDownIterate()
        {
            List<GridCell> list = new ArrayList<>();
            for(int y = gridSize.y-1; y >= 0; y--){
                for(int x = 0; x < gridSize.x; x++){
                    list.add(getCell(new Vector2i(x, y)));
                }
            }
            return list;
        }

        public List<List<GridCell>> horizontalNestedIterate()
        {
            List<List<GridCell>> nestedList = new ArrayList<>();
            for(int y = 0; y < gridSize.y; y++){
                List<GridCell> List = new ArrayList<>();
                for(int x = 0; x < gridSize.x; x++){
                    List.add(getCell(new Vector2i(x, y)));
                }
                nestedList.add(List);
            }
            return nestedList;
        }

        public Integer getX(){
            return gridSize.x;
        }

        public Integer getY(){
            return gridSize.y;
        }

        public Vector2i getCellPos(GridCell cell){
            return cellToPos.get(cell);
        }

        public void swap(GridCell cell1, GridCell cell2){
            setCell(cell1.getPos(), cell2);
            setCell(cell2.getPos(), cell1);
        }

        public Integer getCellX(GridCell cell){
            return cellToPos.get(cell).x;
        }

        public Integer getCellY(GridCell cell){
            return cellToPos.get(cell).y;
        }

        public Integer getCellIndex(GridCell cell){
            return getCellX(cell) + getX() * getCellY(cell);
        }

        public Integer size(){
            return gridSize.x * gridSize.y;
        }

        public TileColor getTileType(Vector2i pos){
            if(getCell(pos) == null){
                return null;
            } else {
                return getCell(pos).getColor();
            }
        }

    }

    private class Checksums {
        public Checksums() {}

        public int calculateBoardChecksum() // level of emulation accuracy uncertain
        {
            byte[] inArray = new byte[grid.size()];
            
            for(GridCell cell : grid.verticalIterate()){
                inArray[cell.getIndex()] = gridCellByteValueForChecksum(cell);
            }
    
            //Board checksum algorithm!
            String string1 = Base64.getEncoder().encodeToString(inArray);
            String string2 = currentGameUUID.toString();
            String string3 = String.valueOf(moveCount);
            return (string1 + string2 + string3).hashCode();
        }
    
        private byte gridCellByteValueForChecksum(GridCell cell) // level of emulation accuracy uncertain
        {
            int byteValue = 0;
            Integer tileTypeValue = cell.getColor().ordinal();
    
            if (cell.isBoosted()) {
                byteValue += (cell.isBoosted() ? 1 : 0) * 20 + tileTypeValue;
            } else {
                byteValue += tileTypeValue * 2 + cell.tileHealth;
            }
    
            return (byte)byteValue;
        }
        
        public String toBase64String()
        {
            byte[] inArray = new byte[grid.size()];

            for(GridCell cell : grid.verticalIterate()){
                inArray[cell.getIndex()] = gridCellByteValueForBase64String(cell);
            }

            return Base64.getEncoder().encodeToString(inArray);
        }

        public byte gridCellByteValueForBase64String(GridCell cell)
        {

            if(cell.getEmpty()){
                throw new RuntimeException("There was a blank cell that was not filled.");
            }

            int byteValue = 0;

            if(cell.color == TileColor.HatOrPurse){
                byteValue = 18;
            } else if (cell.booster == BoosterType.PrismPeacock){
                byteValue = 88;
            } else {
                byteValue += ((cell.booster == BoosterType.None ? 2 : 1) * cell.color.ordinal() + (cell.tileHealth == 0 ? 0 : 1));
                byteValue += cell.booster.ordinal() * 20;
            }
            
            return (byte)byteValue;
        }
    }

    private class GenerateBoard {
        
        private List<TileColor> spawnTiles;
        
        public GenerateBoard()
        {
            spawnTiles = new ArrayList<TileColor>(Arrays.asList(
            TileColor.GreenBird,
            TileColor.PurpleBird,
            TileColor.RedBird,
            TileColor.SnowyBird,
            TileColor.YellowBird));
        }

        public void addSpawnTile(TileColor tileType){
            if(!spawnTiles.contains(tileType)){
                spawnTiles.add(tileType);
            }
        }

        private void initializeGameBoard() // level of emulation accuracy uncertain
        {

            grid.clearGrid();
            List<TileColor> shuffledSpawnTiles = new ArrayList<TileColor>(spawnTiles);

            for (int y = 0; y < grid.getY(); y++)
            {
                for (int x = 0; x < grid.getX(); x++)
                {
                    Collections.shuffle(shuffledSpawnTiles, randomizer);

                    for (TileColor spawnTile : shuffledSpawnTiles)
                    {
                        Boolean validSpawn = true;

                        if ((x >= 2 && grid.getTileType(new Vector2i(x-1, y)) == spawnTile &&
                            grid.getTileType(new Vector2i(x-2, y)) == spawnTile) ||
                            (y >= 2 && grid.getTileType(new Vector2i(x, y-1)) == spawnTile &&
                            grid.getTileType(new Vector2i(x, y-2)) == spawnTile))
                        {
                            validSpawn = false;
                        }

                        if(validSpawn)
                        {
                            Vector2i curr = new Vector2i(x, y);
                            grid.setCell(curr, new GridCell(0, spawnTile, BoosterType.None));
                            break;
                        }
                    }
                }
            }
        } 
        
        public void scrambleTiles() {
            List<GridCell> scrambledTiles = grid.verticalIterate();

            Collections.shuffle(scrambledTiles, randomizer);

            Integer tileNumber = 0;
            for(GridCell tile : scrambledTiles){

                Vector2i newPos = new Vector2i(tileNumber % tile.getX(), tileNumber / tile.getY());
                tile.setPos(newPos);
                tileNumber++;
            }

            calcMoves.clearMatches(new Vector2i(-1, -1), new Vector2i(-1, -1));
        }

        private void clearHats() {
            for(GridCell cell : grid.verticalIterate()){
                if(cell.getColor() == TileColor.HatOrPurse){
                    grid.clearCell(cell, false, true);
                } else {
                    break;
                }
            }

            fillGaps();
            objectives.trackEggsAndClothing();
        }

        public void fillGaps()
        {
            for(List<GridCell> column : grid.verticalNestedIterate()){
                Integer noGapsInColumn = 0;
                for(GridCell cell : column){
                    if(cell.getEmpty()){
                        noGapsInColumn++;
                    } else {
                        Vector2i newPos = new Vector2i(cell.getX(), cell.getY()-noGapsInColumn);
                        cell.setPos(newPos);
                    }
                }
                for(int y = grid.getY()-1; y >= grid.getY()-noGapsInColumn; y--){
                    GridCell cell = column.get(y);
                    cell.setHealth(0);
                    cell.setColor(spawnTiles.get(randomizer.nextInt(spawnTiles.size())));
                    cell.setBooster(BoosterType.None);
                }
            }
        }
        
    }

    public class CalculateMoves{
        
        // used by the flood fill algorithm
        private boolean toVisit[][];
        private boolean visited[][];

        public CalculateMoves(){
            floodFillClearVisited();
        }

        public void calculateMove(Vector2i pos1, Vector2i pos2){

            moveCount++;
            objectives.trackMoves();
            objectives.clearScoreCombo();

            if(pos2.y != -1){   // if given two tiles as input

                grid.swap(grid.getCell(pos1), grid.getCell(pos2));

                clearMatches(pos1, pos2);   // clear matches formed by the swap

                // Sort pos1 and pos2 by booster type
                if(grid.getCell(pos2).getBooster().ordinal() > grid.getCell(pos1).getBooster().ordinal()){
                    Vector2i temp = pos1;
                    pos1 = pos2;
                    pos2 = temp;
                }

                GridCell first = grid.getCell(pos1);
                GridCell second = grid.getCell(pos2);

                if(first.isBoosted() && second.isBoosted()){ // booster combo behaviours
                    
                    if(first.getBooster() == BoosterType.BoomBird && second.isBuzzyBird()){
                        boosters.buzzyBoomComboBehaviour(first, second);
                    } else if (first.getBooster() == BoosterType.BoomBird && second.getBooster() == BoosterType.BoomBird){ 
                        boosters.boomBoomComboBehaviour(first, second);
                    } else if (first.isBuzzyBird() && second.isBuzzyBird()){
                        boosters.buzzyBuzzyComboBehaviour(first, second);
                    } else if (first.getBooster() == BoosterType.PrismPeacock && second.isBuzzyBird()){
                        boosters.prismBuzzyComboBehaviours(first, second);
                    } else if (first.getBooster() == BoosterType.PrismPeacock && second.getBooster() == BoosterType.BoomBird){
                        boosters.prismBoomComboBehaviours(first, second);
                    } else if (first.getBooster() == BoosterType.PrismPeacock && second.getBooster() == BoosterType.PrismPeacock){
                        boosters.prismPrismComboBehaviours(first, second);
                    }

                } else if(first.isBoosted() && !second.isBoosted()){    // single booster behaviours
                    
                    switch (first.getBooster()) {
                        case BuzzyBirdHorizontal: 
                            boosters.buzzyBirdHorizontalBehaviour(first, 1);
                            break;
                        case BuzzyBirdVertical: 
                            boosters.buzzyBirdVerticalBehaviour(first, 1);
                            break;
                        case BoomBird: 
                            boosters.boomBirdBehaviour(first, 3);
                            break;
                        case PrismPeacock: 
                            boosters.prismPeacockBehaviour(first, second.getColor());
                            break;
                        default:
                            break;
                    }
                }

            } else {    // only one tile as input

                GridCell first = grid.getCell(pos1);

                switch (first.getBooster()) {
                    case BuzzyBirdHorizontal: 
                        boosters.buzzyBirdHorizontalBehaviour(first, 1);
                        break;
                    case BuzzyBirdVertical: 
                        boosters.buzzyBirdVerticalBehaviour(first, 1);
                        break;
                    case BoomBird: 
                        boosters.boomBirdBehaviour(first, 3);
                        break;
                    default:
                        break;
                }

            }

            clearMatches(pos1, pos2);
                
        }

        private void clearMatches(Vector2i pos1, Vector2i pos2) {
            while(findAndClearMatches(pos1, pos2)){ // for as long as there are still matches on the game board
                generateBoard.fillGaps();
                generateBoard.clearHats();
            }
        }

        public Boolean findAndClearMatches(Vector2i swap1, Vector2i swap2){ // Returns true if a match is found.

            floodFillClearVisited();
            Boolean isMatch = false;
            Map<Vector2i, Integer> matchType = new HashMap<>();

            // find all vertical matches
            for(List<GridCell> column : grid.verticalNestedIterate()){

                Integer sameTiles = 1;
                List<GridCell> prevNeighbours = new ArrayList<>();

                for(GridCell cell : column){
                    if(prevNeighbours.contains(cell)){ // keep track of number of tiles with the same tile type next to each other
                        sameTiles++;
                    } else if (sameTiles > 2) {
                        isMatch = true;
                        
                        for(int backtrack = cell.getY()-sameTiles; backtrack < cell.getY(); backtrack++){
                            Vector2i back = new Vector2i(cell.getX(), backtrack);

                            floodFillSetToVisit(back);
                            matchType.put(back, sameTiles);
                        }
                        sameTiles = 1;
                    } else {
                        sameTiles = 1;
                    }
                    prevNeighbours = getSameTypeNeighbours(cell, false);
                }
            }

            // find all horizontal matches
            for(List<GridCell> row : grid.horizontalNestedIterate()){

                Integer sameTiles = 1;
                List<GridCell> prevNeighbours = new ArrayList<>();

                for(GridCell cell : row){
                    if(prevNeighbours.contains(cell)){ // keep track of number of tiles with the same tile type next to each other
                        sameTiles++;
                    } else if (sameTiles > 2) {
                        isMatch = true;
                        
                        for(int backtrack = cell.getX()-sameTiles; backtrack < cell.getX(); backtrack++){
                            Vector2i back = new Vector2i(backtrack, cell.getY());

                            floodFillSetToVisit(back);
                            matchType.put(back, sameTiles);
                        }
                        sameTiles = 1;
                    } else {
                        sameTiles = 1;
                    }
                    prevNeighbours = getSameTypeNeighbours(cell, false);
                }
            }

            // clear all matches
            for(GridCell cell : grid.verticalIterate()){
                Vector2i cellPos = cell.getPos();
                if(floodFillGetVisited(cellPos)) {
                    floodFill(cell, matchType.get(cellPos), swap1, swap2);
                }
            }

            return isMatch;

            
        }

        public List<GridCell> getNeighbours(GridCell cell)
        {
            List<GridCell> neighbours = new ArrayList<>();
            List<Vector2i> neighboursPos = new ArrayList<>();
            neighboursPos.add(new Vector2i(cell.getX()-1, cell.getY()));
            neighboursPos.add(new Vector2i(cell.getX()+1, cell.getY()));
            neighboursPos.add(new Vector2i(cell.getX(), cell.getY()+1));
            neighboursPos.add(new Vector2i(cell.getX(), cell.getY()-1));

            for(Vector2i curr : neighboursPos){

                GridCell nextCell = grid.getCell(curr);

                if(nextCell != null){
                    neighbours.add(nextCell);
                }
            }
            return neighbours;         
        }

        public List<GridCell> getSameTypeNeighbours(GridCell cell, Boolean checkMatch)
        {
            List<GridCell> neighbours = new ArrayList<>();

            for(GridCell nextCell : getNeighbours(cell)){

                if(cell.getColor() == nextCell.getColor() && 
                    cell.getColor() != TileColor.HatOrPurse){

                    if(!checkMatch || 
                    floodFillGetToVisit(cell.getPos()) && floodFillGetToVisit(nextCell.getPos())){
                        neighbours.add(nextCell);
                    }
                }
            }
            return neighbours;         
        }

        

        public Boolean floodFillIsBoomBird(GridCell cell){
            List<GridCell> horizontalNeighbours = new ArrayList<>();
            List<GridCell> verticalNeighbours = new ArrayList<>();
            List<Vector2i> horizontalPos = new ArrayList<>();
            List<Vector2i> verticalPos = new ArrayList<>();
            horizontalPos.add(new Vector2i(cell.getX()-1, cell.getY()));
            horizontalPos.add(new Vector2i(cell.getX()+1, cell.getY()));
            verticalPos.add(new Vector2i(cell.getX(), cell.getY()+1));
            verticalPos.add(new Vector2i(cell.getX(), cell.getY()-1));

            for(Vector2i curr : horizontalPos){

                GridCell nextCell = grid.getCell(curr);

                if(nextCell != null &&
                    cell.getColor() == nextCell.getColor() && 
                    cell.getColor() != TileColor.HatOrPurse){

                    if(floodFillGetToVisit(cell.getPos()) && floodFillGetToVisit(nextCell.getPos())){
                        horizontalNeighbours.add(nextCell);
                    }
                    
                }
            }

            for(Vector2i curr : verticalPos){

                GridCell nextCell = grid.getCell(curr);

                if(nextCell != null &&
                    cell.getColor() == nextCell.getColor() && 
                    cell.getColor() != TileColor.HatOrPurse){

                    if(floodFillGetToVisit(cell.getPos()) && floodFillGetToVisit(nextCell.getPos())){
                        horizontalNeighbours.add(nextCell);
                    }
                    
                }
            }

            return (horizontalNeighbours.size() == 1 && verticalNeighbours.size() == 1) || 
                (horizontalNeighbours.size() == 2 && verticalNeighbours.size() == 1) || 
                (horizontalNeighbours.size() == 1 && verticalNeighbours.size() == 2);  
        }

        // false = don't visit this cell/cell has been visited
        // true = visit this cell/cell has not been visited
        public void floodFillSetToVisit(Vector2i pos){
            toVisit[pos.x][pos.y] = true;
            visited[pos.x][pos.y] = true;
        }

        public boolean floodFillGetToVisit(Vector2i pos){ // also returns true if uninitialized
            if (pos.x >= 0 && pos.x < grid.getX() && pos.y >= 0 && pos.y < grid.getY())
            {
                return toVisit[pos.x][pos.y];
            }
            return false;
        }

        public void floodFillSetVisited(Vector2i pos){
            visited[pos.x][pos.y] = false;
        }

        public boolean floodFillGetVisited(Vector2i pos){
            if (pos.x >= 0 && pos.x < grid.getX() && pos.y >= 0 && pos.y < grid.getY())
            {
                return visited[pos.x][pos.y];
            }
            return false;
        }

        public void floodFillClearVisited(){
            toVisit = new boolean[grid.getX()][grid.getY()];
            visited = new boolean[grid.getX()][grid.getY()];
        }

        public void floodFill(GridCell cell, Integer matchType, Vector2i swap1, Vector2i swap2){
            
            TileColor refType = cell.getColor();
            BoosterType refBooster = BoosterType.None;
            
            List<GridCell> connectedNodes = new ArrayList<>();
            Queue<GridCell> floodFillQueue = new LinkedList<>();
            floodFillQueue.add(cell);

            Integer matchTileSize = matchType;
            Boolean containsEgg = false;

            while(!floodFillQueue.isEmpty()){
                GridCell nextCell = floodFillQueue.poll();

                if(nextCell == null) continue;
                if(!floodFillGetVisited(nextCell.getPos())) continue;
                if(nextCell.getColor() != nextCell.getColor()) continue;

                floodFillSetVisited(nextCell.getPos());
                connectedNodes.add(nextCell);
                floodFillQueue.addAll(getSameTypeNeighbours(nextCell, true));

                if(nextCell.getHealth() > 0){
                    containsEgg = true;
                }
                
                // detect tiles connected to other tiles in an L or T shape
                if(floodFillIsBoomBird(nextCell) && matchTileSize != 5) {   // do not change to boom bird if a prism peacock would form
                    matchTileSize = 6;
                }
            }
            
            // keep track of puzzle objectives
            switch(matchTileSize) {
                case 3:
                    objectives.updateScore(30);
                    break;
                case 4:
                    refBooster = randomizer.nextInt(2) == 0 ? BoosterType.BuzzyBirdHorizontal : BoosterType.BuzzyBirdVertical;
                    objectives.updateScore(150);
                    rewards.incrementPuzzle(PuzzleObjectiveType.MakeFlyers);
                    break;
                case 5:
                    refBooster = BoosterType.PrismPeacock;
                    objectives.updateScore(150);
                    rewards.incrementPuzzle(PuzzleObjectiveType.MakePeacocks);
                    break;
                case 6:
                    refBooster = BoosterType.BoomBird;
                    objectives.updateScore(150);
                    rewards.incrementPuzzle(PuzzleObjectiveType.MakeBombBirds);
                    break;
                default:
                    break;
            }

            if(containsEgg && matchTileSize > 3){
                rewards.incrementPuzzle(PuzzleObjectiveType.HatchPowerups);
            }

            
            for(GridCell nextCell : connectedNodes){
                grid.clearCell(nextCell, false, false);

                for(GridCell eggTile : getNeighbours(nextCell)){
                    grid.breakEggTile(eggTile);
                }
            }

            if(refBooster != BoosterType.None){

                if(refBooster == BoosterType.PrismPeacock){
                    refType = TileColor.None;
                }

                if(connectedNodes.contains(grid.getCell(swap1))){
                    grid.setCell(swap1, new GridCell(0, refType, refBooster));
                } else if(connectedNodes.contains(grid.getCell(swap2))){
                    grid.setCell(swap2, new GridCell(0, refType, refBooster));
                } else {
                    grid.setCell(cell.getPos(), new GridCell(0, refType, refBooster));
                }
            }
        }

    }

    private class BoosterBehvaiours {
        public BoosterBehvaiours(){}

        public void buzzyBoomComboBehaviour(GridCell first, GridCell second){
            grid.clearCell(first, false, true);
            grid.clearCell(second, true, true);

            buzzyBirdHorizontalBehaviour(first, 3);
            buzzyBirdVerticalBehaviour(first, 3);

            rewards.incrementPuzzle(PuzzleObjectiveType.ComboFlyerBomb);
        }

        public void boomBoomComboBehaviour(GridCell first, GridCell second){
            grid.clearCell(first, false, true);
            grid.clearCell(second, true, true);
            
            boomBirdBehaviour(first, 5);

            rewards.incrementPuzzle(PuzzleObjectiveType.ComboBombBomb);
        }

        public void buzzyBuzzyComboBehaviour(GridCell first, GridCell second){
            grid.clearCell(first, false, true);
            grid.clearCell(second, true, true);
            
            buzzyBirdHorizontalBehaviour(first, 1);
            buzzyBirdVerticalBehaviour(first, 1);

            rewards.incrementPuzzle(PuzzleObjectiveType.ComboFlyerFlyer);
        }

        public void prismBuzzyComboBehaviours(GridCell first, GridCell second){
            TileColor refType = second.color;

            grid.clearCell(first, false, true);
            grid.clearCell(second, true, true);

            rewards.incrementPuzzle(PuzzleObjectiveType.ComboFlyerPeacock);

            List<GridCell> newBuzzyBirds = new ArrayList<>();

            for(GridCell cell : grid.verticalIterate()){
                if(cell.getColor() == refType && !cell.isBoosted()) {
                    newBuzzyBirds.add(cell);
                }
            }

            for (GridCell newBuzzyBird : newBuzzyBirds) {
                Boolean coinflip = randomizer.nextInt(2) == 1;
                
                if(coinflip) {
                    buzzyBirdHorizontalBehaviour(newBuzzyBird, 1, true);
                } else {
                    buzzyBirdVerticalBehaviour(newBuzzyBird, 1, true);
                }
            }
        }

        public void prismBoomComboBehaviours(GridCell first, GridCell second){
            TileColor refType = second.color;

            grid.clearCell(first, false, true);
            grid.clearCell(second, true, true);

            rewards.incrementPuzzle(PuzzleObjectiveType.ComboBombPeacock);

            List<Vector2i> newBoomBirds = new ArrayList<>();

            for(GridCell cell : grid.verticalIterate()){
                if(cell.getColor() == refType && !cell.isBoosted()) {
                    if(cell.getColor() == refType && !cell.isBoosted()) {
                        cell.setBooster(BoosterType.BoomBird);
                        grid.setCell(cell.getPos(), cell);
                        newBoomBirds.add(cell.getPos());
                    }
                }
            }

            Integer d = Math.floorDiv(3, 2);

            for (Vector2i pos : newBoomBirds) {     // first explosions
    
                for(int x = pos.x-d; x <= pos.x+d; x++){
                    for(int y = pos.y-d; y <= pos.y+d; y++){

                        GridCell cell = grid.getCell(new Vector2i(x, y));

                        if(cell != null && !newBoomBirds.contains(cell.getPos())) {
                            grid.clearCell(cell, true, false, true);
                        }
                    }
                }
            }
            generateBoard.fillGaps();
            newBoomBirds.clear();

            for(GridCell cell : grid.verticalIterate()){
                if(cell.getColor() == refType && cell.getBooster() == BoosterType.BoomBird) {
                    newBoomBirds.add(cell.getPos());
                }
            }

            for (Vector2i pos : newBoomBirds) {    // second explosions
    
                for(int x = pos.x-d; x <= pos.x+d; x++){
                    for(int y = pos.y-d; y <= pos.y+d; y++){

                        GridCell cell = grid.getCell(new Vector2i(x, y));
                        grid.clearCell(cell, true, true, true);
                    }
                }
            }

            generateBoard.fillGaps();
        }

        public void prismPrismComboBehaviours(GridCell first, GridCell second){
            grid.clearCell(first, false, true);
            grid.clearCell(second, false, true);

            rewards.incrementPuzzle(PuzzleObjectiveType.ComboBombPeacock);

            for(GridCell cell : grid.verticalIterate()){
                grid.clearCell(cell, true, false, true);
            }

            generateBoard.fillGaps();
        }

        private void buzzyBirdHorizontalBehaviour(GridCell cell, Integer size){
            buzzyBirdHorizontalBehaviour(cell, size, false);
        }

        private void buzzyBirdHorizontalBehaviour(GridCell cell, Integer size, boolean isClearedByPeacock) {
            
            grid.clearCell(cell, false, true);

            Integer d = Math.floorDiv(size, 2);
            
            for(int y = cell.getY()-d; y <= cell.getY()+d; y++){
                for(int x = 0; x < grid.getX(); x++){
                    GridCell curr = grid.getCell(new Vector2i(x, y));
                    if(curr != null){
                        if(curr.getBooster() != BoosterType.BuzzyBirdHorizontal){
                            grid.clearCell(curr, true, false, isClearedByPeacock);
                        } else {
                            grid.clearCell(curr, true, true, isClearedByPeacock);
                            buzzyBirdVerticalBehaviour(curr, 1);
                        }
                    }
                }
            }
            generateBoard.fillGaps();
        }

        private void buzzyBirdVerticalBehaviour(GridCell cell, Integer size){
            buzzyBirdVerticalBehaviour(cell, size, false);
        }

        private void buzzyBirdVerticalBehaviour(GridCell cell, Integer size, boolean isClearedByPeacock) {
            
            grid.clearCell(cell, false, true);
            
            Integer d = Math.floorDiv(size, 2);
            
            for(int x = cell.getX()-d; x <= cell.getX()+d; x++){
                for(int y = 0; y < grid.getY(); y++){
                    GridCell curr = grid.getCell(new Vector2i(x, y));
                    if(curr != null){
                        if(curr.getBooster() != BoosterType.BuzzyBirdVertical){
                            grid.clearCell(curr, true, false, isClearedByPeacock);
                        } else {
                            grid.clearCell(curr, true, true, isClearedByPeacock);
                            buzzyBirdHorizontalBehaviour(curr, 1);
                        }
                    }
                }
            }
            generateBoard.fillGaps();
        }
        
        private void boomBirdBehaviour(GridCell cell, Integer size) {
            
            grid.clearCell(cell, false, true);

            Integer d = Math.floorDiv(size, 2);

            for(int x = cell.getX()-d; x <= cell.getX()+d; x++){
                for(int y = cell.getY()-d; y <= cell.getY()+d; y++){

                    GridCell curr = grid.getCell(new Vector2i(x, y));
                    if(x != cell.getX() && y != cell.getY()) {
                        grid.clearCell(curr, true, false);
                    }
                }
            }
            generateBoard.fillGaps();
            for(int x = cell.getX()-d; x <= cell.getX()+d; x++){
                for(int y = cell.getY()-d-1; y <= cell.getY()+d-1; y++){
                    GridCell curr = grid.getCell(new Vector2i(x, y));
                    grid.clearCell(curr, true, false);
                }
            }
            generateBoard.fillGaps();
        }

        private void prismPeacockBehaviour(GridCell cell, TileColor refType) {

            grid.clearCell(cell, false, true);

            for(GridCell curr : grid.verticalIterate()){
                if(curr.color == refType && !curr.isBoosted()) {
                    grid.clearCell(curr, true, false, true);
                }
            }
            generateBoard.fillGaps();
        }

    }

    public class LevelObjectives 
    {
        private int objectivesTracker[][] = { {-1, -1, 0},
                                            {-1, -1, 1},
                                            {-1, -1, 2},
                                            {-1, -1, 3}};

        JsonObject specialOrderData;
        JsonObject specialOrderCountRangeData;
        private Integer matchComboScore = 0;
        Integer objectiveScore = 0;

        public LevelObjectives(){
            newLevelNewObjectives();
        }

        public void newLevelNewObjectives(){

            objectiveScore = 0;
            if(level != 0){

                //get level data for the level range the current level falls into
                
                for(JsonElement ele : specialOrders){
                    JsonObject data = ele.getAsJsonObject();

                    if(rewards.inRange(data)){
                        specialOrderData = data;
                        break;
                    }
                }

                for(JsonElement ele : specialOrderCountRanges){
                    JsonObject data = ele.getAsJsonObject();

                    if(rewards.inRange(data)){
                        specialOrderCountRangeData = data;
                        break;
                    }
                }
            } else {
                specialOrderData = specialOrders.get(0).getAsJsonObject();
                specialOrderCountRangeData = specialOrderCountRanges.get(0).getAsJsonObject();
            }

            // clear the score objective
            initObjective(LevelObjectiveType.ScoreRequirement, scoreRequirement());

            // randomly pick objectives to be given to the player
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

                // Add clothing and eggs to the board if the respective objectives are given
                for(GridCell cell : grid.horizontalTopDownIterate()){

                    if(!cell.isBoosted()) {
                        if(cell.getColor() != TileColor.HatOrPurse && 
                            !cell.isBoosted() && clothing > 0){

                            cell.setColor(TileColor.HatOrPurse);
                            clothing--;
                        } else if(cell.getHealth() == 0 && !cell.isBoosted() && eggs > 0){
                            cell.setHealth(1);
                            eggs--;
                        }
                    }
                }
            }
        }

        public Boolean isNextLevel(){   // returns true if level is incermented
            trackEggsAndClothing();

            Boolean goToNextLevel = true;
            goToNextLevel &= isAchieved(LevelObjectiveType.ClothingLeft);
            goToNextLevel &= isAchieved(LevelObjectiveType.EggsLeft);
            goToNextLevel &= isAchieved(LevelObjectiveType.ScoreRequirement);
            goToNextLevel &= !hasRunOutOfMoves();
            goToNextLevel &= isObjective(LevelObjectiveType.ScoreRequirement);

            if(goToNextLevel){
                level++;

                // update puzzle objectives

                Integer numberOfOrders = 0;

                if(isAchieved(LevelObjectiveType.ClothingLeft) && 
                    isObjective(LevelObjectiveType.ClothingLeft)){
                    rewards.incrementPuzzle(PuzzleObjectiveType.CompleteSpecialOrders);
                    numberOfOrders++;
                }
                if(isAchieved(LevelObjectiveType.MovesLeft) && 
                    isObjective(LevelObjectiveType.MovesLeft)){
                    rewards.incrementPuzzle(PuzzleObjectiveType.CompleteRushHourOrders);
                    numberOfOrders++;
                }
                if(isAchieved(LevelObjectiveType.EggsLeft) && 
                    isObjective(LevelObjectiveType.EggsLeft)){
                    rewards.incrementPuzzle(PuzzleObjectiveType.CompleteShortStaffedOrders);
                    numberOfOrders++;
                }
                if(numberOfOrders > 1){
                    rewards.incrementPuzzle(PuzzleObjectiveType.CompleteComboOrders);
                }
                rewards.incrementPuzzle(PuzzleObjectiveType.CompleteOrders);

                // add new bird types to be placed on to the board

                if(level > 25){
                    generateBoard.addSpawnTile(TileColor.AquaBird);
                }
                if (level > 75){
                    generateBoard.addSpawnTile(TileColor.BlueBird);
                }
                if (level > 10){
                    generateBoard.addSpawnTile(TileColor.PinkBird);
                }
                
                // get the objectives for the new level
                newLevelNewObjectives();
            }

            return goToNextLevel;
        }

        public void trackMoves(){   // to be called whenever a moves is made
            if(isObjective(LevelObjectiveType.MovesLeft)){
                addValueToObjective(LevelObjectiveType.MovesLeft, 1);
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
                for(GridCell cell : grid.horizontalTopDownIterate()){
                    if(cell.getHealth() > 0) {
                        numberOfEggs++;
                    }
                }
                setObjectiveMaxSubValue(LevelObjectiveType.EggsLeft, numberOfEggs);
            }

            if(isObjective(LevelObjectiveType.ClothingLeft)){
                Integer numberOfClothing = 0;
                for(GridCell cell : grid.horizontalTopDownIterate()){
                    if(cell.getColor() == TileColor.HatOrPurse) {
                        numberOfClothing++;
                    }
                }
                setObjectiveMaxSubValue(LevelObjectiveType.ClothingLeft, numberOfClothing);
            }
        }

        // helper functions

        

        private Boolean diceRoll(String attribute){ // probability of objectives is expressed as percentages
            return randomizer.nextInt(100) < specialOrderData.get(attribute).getAsInt();
        }
        
        private Boolean isObjective(LevelObjectiveType objectiveType) { // check if objective has been assigned to the player
            return objectivesTracker[objectiveType.ordinal()][0] != -1;
        }
        
        private Boolean isAchieved(LevelObjectiveType objectiveType){ // returns true if objective is fulfilled or not assigned
            if(objectivesTracker[objectiveType.ordinal()][0] == -1){
                return true;
            } else if (objectivesTracker[objectiveType.ordinal()][0] <=
            objectivesTracker[objectiveType.ordinal()][1]) {
                return true;
            } else{
                return false;
            }
        }
        
        private Integer initObjective(LevelObjectiveType objectiveType, String minimum, String maximum){    // generates objective given names of Json elements that contain level data
            
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

        private Integer scoreRequirement(){ // formula for score required to pass a level

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
        
        private void addValueToObjective(LevelObjectiveType objectiveType, Integer value){
            objectivesTracker[objectiveType.ordinal()][1] += value;
        }

        private void setObjectiveMaxSubValue(LevelObjectiveType objectiveType, Integer value){
            objectivesTracker[objectiveType.ordinal()][1] = objectivesTracker[objectiveType.ordinal()][0] - value;
        }

        private void updateObjective(LevelObjectiveType objectiveType, Integer value){
            objectivesTracker[objectiveType.ordinal()][1] = value;
        }

        private void updateScore(int scoreIncrease) {
            matchComboScore += scoreIncrease;

            dizzyBirdMeter += scoreIncrease / 30;
            score += matchComboScore;
            objectiveScore += scoreIncrease;
            updateObjective(LevelObjectiveType.ScoreRequirement, objectiveScore);
        }

        private void fixedUpdateScore(int scoreIncrease) {
            dizzyBirdMeter += scoreIncrease / 30;
            score += scoreIncrease;
            objectiveScore += scoreIncrease;
            updateObjective(LevelObjectiveType.ScoreRequirement, objectiveScore);
        }

        private void clearScoreCombo(){
            matchComboScore = 0;
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
        
    } 

    private class Rewards{
        private int puzzleCurrentProgress[];

        public Rewards(){
            puzzleCurrentProgress = new int[100];
        }

        private void clearCellUpdateObjective(GridCell cell) {
            switch(cell.getColor()){
                case RedBird:
                    incrementPuzzle(PuzzleObjectiveType.ClearRedBirds);
                    break;
                case BlueBird:
                    incrementPuzzle(PuzzleObjectiveType.ClearBlueBirds);
                    break;
                case GreenBird:
                    incrementPuzzle(PuzzleObjectiveType.ClearGreenBirds);
                    break;
                case YellowBird:
                    incrementPuzzle(PuzzleObjectiveType.ClearYellowBirds);
                    break;
                case PinkBird:
                    incrementPuzzle(PuzzleObjectiveType.ClearPinkBirds);
                    break;
                case SnowyBird:
                    incrementPuzzle(PuzzleObjectiveType.ClearWhiteBirds);
                    break;
                case PurpleBird:
                    incrementPuzzle(PuzzleObjectiveType.ClearPurpleBirds);
                case AquaBird:
                    incrementPuzzle(PuzzleObjectiveType.ClearAquaBirds);
                    break;
                default:
                    break;
            }
        }
        
        private void clearedWithPeacockUpdateObjective(GridCell cell) {
            switch(cell.getColor()){
                case RedBird:
                    incrementPuzzle(PuzzleObjectiveType.ClearedWithPeacockRed);
                    break;
                case BlueBird:
                    incrementPuzzle(PuzzleObjectiveType.ClearedWithPeacockBlue);
                    break;
                case GreenBird:
                    incrementPuzzle(PuzzleObjectiveType.ClearedWithPeacockGreen);
                    break;
                case YellowBird:
                    incrementPuzzle(PuzzleObjectiveType.ClearedWithPeacockYellow);
                    break;
                case PinkBird:
                    incrementPuzzle(PuzzleObjectiveType.ClearedWithPeacockPink);
                    break;
                case SnowyBird:
                    incrementPuzzle(PuzzleObjectiveType.ClearedWithPeacockWhite);
                    break;
                case PurpleBird:
                    incrementPuzzle(PuzzleObjectiveType.ClearedWithPeacockPurple);
                case AquaBird:
                    incrementPuzzle(PuzzleObjectiveType.ClearedWithPeacockAqua);
                    break;
                default:
                    break;
            }
        }

        public void incrementPuzzle(PuzzleObjectiveType puzzle){
            puzzleCurrentProgress[puzzle.getVal()]++;

            PuzzleObjectiveType[] enums = PuzzleObjectiveType.values();
            PuzzleObjectiveType nextPuzzle = enums[(puzzle.ordinal()+1)%enums.length];

            if(nextPuzzle.toString().contains("SingleGame")){
                puzzleCurrentProgress[nextPuzzle.getVal()]++;
            }

        }

        public void clearPuzzleTemp(PuzzleObjectiveType puzzle){
            puzzleCurrentProgress[puzzle.getVal()] = 0;
        }

        public Integer getPuzzleTemp(PuzzleObjectiveType puzzle){
            return puzzleCurrentProgress[puzzle.getVal()];
        }

        // functions related to saving the saved game user variable to the player inventory.

        private Integer puzzleTypeToUserVarIndex(PuzzleObjectiveType puzzle){
            for(JsonElement ele : achievementToUserVarIndexList){
                JsonObject dataPair = (JsonObject) ele;
                if(dataPair.get("achievementType").getAsInt() == puzzle.getVal()){
                    return dataPair.get("userVarIndex").getAsInt();
                }
            }
            return -1;
        }

        private void setPuzzleObjectiveUserVar(Player player, PuzzleObjectiveType puzzle, Integer value){ 
            player.account.getSaveSpecificInventory().getUserVarAccesor().setPlayerVarValue(UserVarIDs.persistentAchievementDataUserVarDefId.getVal(), puzzleTypeToUserVarIndex(puzzle), value);
        }

        private Integer getPuzzleObjectiveUserVar(Player player, PuzzleObjectiveType puzzle){

            UserVarValue value = player.account.getSaveSpecificInventory().getUserVarAccesor().getPlayerVarValue(UserVarIDs.persistentAchievementDataUserVarDefId.getVal(), puzzle.getVal());
            if(value == null){
                setPuzzleObjectiveUserVar(player, puzzle, 0);
                return 0;
            } else {
                return player.account.getSaveSpecificInventory().getUserVarAccesor().getPlayerVarValue(UserVarIDs.persistentAchievementDataUserVarDefId.getVal(), puzzle.getVal()).value;
            }
        }

        // these are called when the game starts.

        private void saveSavedGameUserVar(Player player) {

            // write into the player inventory the number of moves made in the level, send inventory item packet
            player.account.getSaveSpecificInventory().getUserVarAccesor().setPlayerVarValue(UserVarIDs.savedGameUserVarDefId.getVal(), level, moveCount);

            // write into the player inventory the puzzle objectives
            for(PuzzleObjectiveType puzzleType : PuzzleObjectiveType.values()){
                if(puzzleType.toString().contains("SingleGame") || 
                    puzzleType == PuzzleObjectiveType.HighScore){

                    setPuzzleObjectiveUserVar(player, puzzleType, 
                        Math.max(getPuzzleTemp(puzzleType), 
                        getPuzzleObjectiveUserVar(player, puzzleType)));
                } else {
                    setPuzzleObjectiveUserVar(player, puzzleType, 
                        getPuzzleTemp(puzzleType) +
                        getPuzzleObjectiveUserVar(player, puzzleType));
                    clearPuzzleTemp(puzzleType);
                }
            }
        }

        private void resetSavedGameUserVar(Player player) {

            // reset these values as a new game has been started
            level = 0;
            score = 0;
            moveCount = 0;
            dizzyBirdMeter = 0;

            currentGameUUID = UUID.randomUUID().toString();
            generateBoard.initializeGameBoard();
            
            // initialise user vars
            player.account.getSaveSpecificInventory().getUserVarAccesor().deletePlayerVar(UserVarIDs.savedGameUserVarDefId.getVal());   // only this can be safely reset
            player.account.getSaveSpecificInventory().getUserVarAccesor().setPlayerVarValue(UserVarIDs.savedGameUserVarDefId.getVal(), 0, 1);
            player.account.getSaveSpecificInventory().getUserVarAccesor().setPlayerVarValue(UserVarIDs.tutorial.getVal(), 0, 1);
        }

        private void loadSavedGameUserVar(Player player) {  // the game only keeps track of the highest score ever and I'm not sure what the continue game button should do

            // retrieve the data from the player inventory
            UserVarValue[] savedGame = player.account.getSaveSpecificInventory().getUserVarAccesor().getPlayerVarValue(UserVarIDs.savedGameUserVarDefId.getVal());

            // load in the values associated with a previous game
            if(savedGame != null){
                level = Math.max(1, savedGame.length-1);
                score = 0;
                moveCount = savedGame[savedGame.length-1].value;
                dizzyBirdMeter = 0;
            }
        }

        private void giveLevelReward(Player player){

            for(JsonElement ele : levelRewards){
                JsonArray rewardData = (JsonArray)ele.deepCopy();
                JsonObject levelData = (JsonObject)rewardData.get(0);
                rewardData.remove(0);

                Integer randNo = randomizer.nextInt(100);
                Integer sumWeight = 0;
                
                if(inRange(levelData)){
                    
                    for(JsonElement eleReward : rewardData){
                        JsonObject rewardDataEle = (JsonObject)eleReward;

                        if(sumWeight < randNo && randNo <= rewardDataEle.get("weight").getAsInt()){
                            LootInfo chosenReward = ResourceCollectionModule.getLootReward(rewardDataEle.get("lootTableDefID").getAsString());
                            
                            // Give reward
                            String itemId = chosenReward.reward.itemId;
                            Integer count = chosenReward.count;

                            player.account.getSaveSpecificInventory().getItemAccessor(player)
                            .add(Integer.parseInt(itemId), count);

                            // XP
                            LevelEventBus.dispatch(new LevelEvent("levelevents.minigames.dizzywingdispatch",
                                    new String[] { "level:" + level }, player));

                            // Send packet
                            MinigamePrizePacket p1 = new MinigamePrizePacket();
                            p1.given = true;
                            p1.itemDefId = itemId;
                            p1.itemCount = count;
                            p1.prizeIndex1 = 1;
                            p1.prizeIndex2 = 0;
                            player.client.sendPacket(p1);
                        } else {
                            sumWeight += rewardDataEle.get("weight").getAsInt();
                        }

                    }

                }
            }

        }

        private Boolean inRange(JsonObject data){
            return (level >= data.get("levelIndex").getAsInt() &&
                level <= data.get("endLevelIndex").getAsInt()) || 
                data.get("isEndLevelInfinite").getAsBoolean() ;
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

    enum TileColor 
    {
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
        HighScore(0),
        TotalScore(1),
        MakeFlyers(2),
        MakeFlyers_SingleGame(3),
        MakeBombBirds(4),
        MakeBombBirds_SingleGame(5),
        MakePeacocks(6),
        MakePeacocks_SingleGame(7),
        ClearRedBirds(8),
        ClearRedBirds_SingleGame(9),
        ClearBlueBirds(10),
        ClearBlueBirds_SingleGame(11),
        ClearGreenBirds(12),
        ClearGreenBirds_SingleGame(13),
        ClearYellowBirds(14),
        ClearYellowBirds_SingleGame(15),
        ClearPinkBirds(17),
        ClearPinkBirds_SingleGame(18),
        ClearWhiteBirds(19),
        ClearWhiteBirds_SingleGame(20),
        ClearPurpleBirds(21),
        ClearPurpleBirds_SingleGame(22),
        ClearAquaBirds(23),
        ClearAquaBirds_SingleGame(24),
        CompleteOrders(25),
        CompleteOrders_SingleGame(26),
        CompleteSpecialOrders(27),
        CompleteSpecialOrders_SingleGame(28),
        CompleteRushHourOrders(29),
        CompleteRushHourOrders_SingleGame(31),
        CompleteShortStaffedOrders(32),
        CompleteShortStaffedOrders_SingleGame(33),
        CompleteComboOrders(34),
        CompleteComboOrders_SingleGame(35),
        ClearedWithPeacockRed(36),
        ClearedWithPeacockRed_SingleGame(37),
        ClearedWithPeacockBlue(38),
        ClearedWithPeacockBlue_SingleGame(39),
        ClearedWithPeacockGreen(40),
        ClearedWithPeacockGreen_SingleGame(41),
        ClearedWithPeacockYellow(42),
        ClearedWithPeacockYellow_SingleGame(43),
        ClearedWithPeacockPink(44),
        ClearedWithPeacockPink_SingleGame(45),
        ClearedWithPeacockWhite(46),
        ClearedWithPeacockWhite_SingleGame(47),
        ClearedWithPeacockPurple(48),
        ClearedWithPeacockPurple_SingleGame(49),
        ClearedWithPeacockAqua(50),
        ClearedWithPeacockAqua_SingleGame(51),
        HatchPowerups(52),
        HatchPowerups_SingleGame(53),
        ComboFlyerBomb(54),
        ComboFlyerBomb_SingleGame(55),
        ComboFlyerPeacock(56),
        ComboFlyerPeacock_SingleGame(57),
        ComboBombPeacock(58),
        ComboBombPeacock_SingleGame(59),
        ComboFlyerFlyer(60),
        ComboFlyerFlyer_SingleGame(61),
        ComboBombBomb(62),
        ComboBombBomb_SingleGame(63),
        ComboPeacockPeacock(64),
        ComboPeacockPeacock_SingleGame(65);
        
        private final int value;
        private PuzzleObjectiveType(int value) {
            this.value = value;
        }
        public int getVal() {
            return value;
        }
    
    }

    enum UserVarIDs
    {
        persistentAchievementDataUserVarDefId(13392),
        puzzleRedemptionStatusUserVarDefId(13404),
        puzzlePieceRedemptionStatusUserVarDefId(13405),
        savedGameUserVarDefId(30613),
        tutorial(13624),
        userVarInventory(303);
        
        private final int value;
        private UserVarIDs(int value) {
            this.value = value;
        }
        public int getVal() {
            return value;
        }
    
    }
    
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

        // save score and number of moves made on this level
        saveGame(player, rd);
        
        rewards.resetSavedGameUserVar(player);

        // the format of the minigame message response packet
        XtWriter mmData = new XtWriter();
        mmData.writeString(currentGameUUID);
        mmData.writeInt(checksums.calculateBoardChecksum());
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
        calcMoves.calculateMove(new Vector2i(rd.readInt(), rd.readInt()), new Vector2i(rd.readInt(), rd.readInt()));
        // sync client if sever checksum differs from client checksum (always occurs at current)
        if (clientChecksum != checksums.calculateBoardChecksum()) {
            syncClient(player, rd);
        }
        
        // check if level should increase
        if (objectives.isNextLevel()) {
            moveCount = 0;
            rewards.giveLevelReward(player);
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
        generateBoard.scrambleTiles();
        syncClient(player, rd);
    }

    @MinigameMessage("continueGame")
	public void continueGame(Player player, XtReader rd) {
        
        // send start game client with previous values
        rewards.loadSavedGameUserVar(player);

        // The GUID is used as the seed for the random number generator.
        currentGameUUID = UUID.randomUUID().toString();

        // the format of the minigame message response packet
        XtWriter mmData = new XtWriter();
        mmData.writeString(currentGameUUID);
        mmData.writeInt(checksums.calculateBoardChecksum());
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

        rewards.saveSavedGameUserVar(player);
        
        if (player.client != null && player.client.isConnected()) {
            // Send to client
            InventoryItemPacket pk = new InventoryItemPacket();
            pk.item = player.account.getSaveSpecificInventory().getItem(Integer.toString(UserVarIDs.userVarInventory.getVal()));
            player.client.sendPacket(pk);
        }
    }



    @MinigameMessage("redeemPiece")
	public void redeemPiece(Player player, XtReader rd) {
        int paintingIndex = rd.readInt(); // 0 - 6
        int pieceIndex = rd.readInt(); // 0 - 16

        player.account.getSaveSpecificInventory().getUserVarAccesor().setPlayerVarValue(
            UserVarIDs.puzzlePieceRedemptionStatusUserVarDefId.getVal(), paintingIndex*16+pieceIndex, 1);
  
        for(int painting = 0; painting < 6; painting++){
            Boolean fullPainting = true;
            for(int piece = 0; piece < 16; piece++){
                fullPainting &= player.account.getSaveSpecificInventory().getUserVarAccesor().getPlayerVarValue(
                    UserVarIDs.puzzlePieceRedemptionStatusUserVarDefId.getVal(), paintingIndex*16+pieceIndex) != null;
            }
            if(fullPainting && player.account.getSaveSpecificInventory().getUserVarAccesor().getPlayerVarValue(
                UserVarIDs.puzzleRedemptionStatusUserVarDefId.getVal(), paintingIndex) == null){
                
                Integer rewardID = puzzleRewards.get(painting).getAsInt();

                // give player the item
                player.account.getSaveSpecificInventory().getItemAccessor(player)
                .add(rewardID, 1);

                // XP
                LevelEventBus.dispatch(new LevelEvent("levelevents.minigames.dizzywingdispatch",
                new String[] { "level:" + level }, player));
                
                // send player a notification
                MinigamePrizePacket p1 = new MinigamePrizePacket();
                p1.given = true;
                p1.itemDefId = Integer.toString(rewardID);
                p1.itemCount = 1;
                p1.prizeIndex1 = 1;
                p1.prizeIndex2 = 0;
                player.client.sendPacket(p1);

                player.account.getSaveSpecificInventory().getUserVarAccesor().setPlayerVarValue(
                    UserVarIDs.puzzleRedemptionStatusUserVarDefId.getVal(), paintingIndex, 1);
            }      
        }
        if (player.client != null && player.client.isConnected()) {
            // Send to client
            InventoryItemPacket pk = new InventoryItemPacket();
            pk.item = player.account.getSaveSpecificInventory().getItem(Integer.toString(
                UserVarIDs.userVarInventory.getVal()));
            player.client.sendPacket(pk);
        }
    }

    @MinigameMessage("syncClient")
	public void syncClient(Player player, XtReader rd) {
        XtWriter mmData = new XtWriter();
        mmData.writeInt(moveCount);
        mmData.writeInt(level);
        mmData.writeInt(score);
        mmData.writeInt(dizzyBirdMeter);
        mmData.writeString(checksums.toBase64String());

        
        List<int[]> objectiveProgress = objectives.getObjectiveProgress();
        
        if(!objectives.hasRunOutOfMoves()){
            mmData.writeInt(objectiveProgress.size());
            for(int[] objectiveData : objectiveProgress){
                mmData.writeInt(objectiveData[2]);   // objective type
                mmData.writeInt(objectiveData[0]);   // objective requirement
                mmData.writeInt(objectiveData[1]);   // objective current progress
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

        MinigameMessagePacket mm = new MinigameMessagePacket();
		mm.command = "syncClient";
		mm.data = mmData.encode().substring(4);
		player.client.sendPacket(mm);
    }
    
}
