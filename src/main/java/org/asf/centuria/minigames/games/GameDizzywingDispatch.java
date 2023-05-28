package org.asf.centuria.minigames.games;

import java.util.Queue;
import java.util.Random;
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
import org.asf.centuria.minigames.AbstractMinigame;
import org.asf.centuria.minigames.MinigameMessage;
import org.asf.centuria.packets.xt.gameserver.inventory.InventoryItemPacket;
import org.asf.centuria.packets.xt.gameserver.minigame.MinigameMessagePacket;
import org.joml.Vector2i;

public class GameDizzywingDispatch extends AbstractMinigame{

    private String currentGameUUID;
    public GameState gameState;
    private int level = 1;
    private int score = 0;
    private int moveCount = 0;
    private int dizzyBirdMeter = 0;

    static {
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



        // initializing the object

        private GridCell[][] grid;
        private boolean toVisit[][];
        private boolean visited[][];
        private Vector2i gridSize;
        private Integer moveCount;
        private List<TileType> spawnTiles;
        private Random randomizer;
        private Integer matchComboScore;

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

            setCell(new Vector2i(0, 0), new GridCell(0, TileType.PinkBird, BoosterType.None));
            setCell(new Vector2i(1, 1), new GridCell(0, TileType.PinkBird, BoosterType.None));
            setCell(new Vector2i(1, 2), new GridCell(0, TileType.PinkBird, BoosterType.None));
            setCell(new Vector2i(2, 0), new GridCell(0, TileType.PinkBird, BoosterType.None));
            setCell(new Vector2i(3, 0), new GridCell(0, TileType.PinkBird, BoosterType.None));
            
            setCell(new Vector2i(6, 0), new GridCell(0, TileType.PinkBird, BoosterType.None));
            setCell(new Vector2i(6, 1), new GridCell(0, TileType.PinkBird, BoosterType.None));
            setCell(new Vector2i(6, 3), new GridCell(0, TileType.PinkBird, BoosterType.None));
            setCell(new Vector2i(5, 2), new GridCell(0, TileType.PinkBird, BoosterType.None));
            setCell(new Vector2i(7, 2), new GridCell(0, TileType.PinkBird, BoosterType.None));

            setCell(new Vector2i(0, 8), new GridCell(0, TileType.PinkBird, BoosterType.None));
            setCell(new Vector2i(1, 8), new GridCell(0, TileType.PinkBird, BoosterType.None));
            setCell(new Vector2i(2, 7), new GridCell(0, TileType.PinkBird, BoosterType.None));
            setCell(new Vector2i(3, 8), new GridCell(0, TileType.PinkBird, BoosterType.None));
            setCell(new Vector2i(4, 8), new GridCell(0, TileType.PinkBird, BoosterType.None));

            setCell(new Vector2i(0, 5), new GridCell(0, TileType.PinkBird, BoosterType.None));
            setCell(new Vector2i(1, 5), new GridCell(0, TileType.PinkBird, BoosterType.None));
            setCell(new Vector2i(2, 4), new GridCell(0, TileType.PinkBird, BoosterType.None));
            setCell(new Vector2i(3, 5), new GridCell(0, TileType.PinkBird, BoosterType.None));

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

        private void setCell(Vector2i pos, GridCell cell){
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
                GridCell brokenEggTile = getCell(pos);
                brokenEggTile.setHealth(0);
                setCell(pos, brokenEggTile);
            } else if(getCell(pos).getTileType() != TileType.HatOrPurse) {
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

        private void nextToMatchEffects(Vector2i pos){  // breaks egg tiles, activates booster tiles
            GridCell cell = getCell(pos);
            if(cell != null){
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
                    case None:
                        cell.setHealth(0);
                        setCell(pos, cell);
                        break;
                    default:
                        break;
                }
                
            }
        }



        // functions related to generating a game board

        private void clearGrid(){
            grid = new GridCell[gridSize.x][gridSize.y];
        }
        
        public int calculateBoardChecksum()
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
            String string3 = moveCount.toString();
            return (string1 + string2 + string3).hashCode();
        }

        private byte gridCellByteValueForChecksum(Vector2i pos)
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

        private void initializeGameBoard()
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
            moveCount = 0;
        }



        // functions related to responding to a move from the player

        public void calculateMove(Vector2i pos1, Vector2i pos2){

            moveCount++;

            if(pos2.y != -1){   // if given two tiles as input

                GridCell cell1 = getCell(pos1);
                setCell(pos1, getCell(pos2));
                setCell(pos2, cell1);
    
                floodFillClearVisited();
    
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
    
                                if(getCell(curr).getTileType() == refType && !getCell(curr).isBoosted()) {
                                    setCell(curr, new GridCell(0, refType, BoosterType.BoomBird));
                                    newBoomBirds.add(curr);
                                }
                            }
                        }
    
                        Integer d = Math.floorDiv(3, 2);
    
                        for (Vector2i pos : newBoomBirds) {
                
                            for(int x = pos.x-d; x <= pos.x+d; x++){
                                for(int y = pos.y-d; y <= pos.y+d; y++){
        
                                    Vector2i curr = new Vector2i(x, y);
        
                                    if(!newBoomBirds.contains(curr)) {
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
    
                                if(getCell(curr).getTileType() == refType && getCell(curr).getBooster() == BoosterType.BoomBird) {
                                    newBoomBirds.add(curr);
                                }
                            }
                        }
    
                        for (Vector2i pos : newBoomBirds) {
                
                            for(int x = pos.x-d; x <= pos.x+d; x++){
                                for(int y = pos.y-d; y <= pos.y+d; y++){
        
                                    Vector2i curr = new Vector2i(x, y);
                                    clearCell(curr, true, false);
                                }
                            }
                        }
    
    
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
            
            Integer d = Math.floorDiv(size, 2);
            
            for(int y = pos.y-d; y <= pos.y+d; y++){
                for(int x = 0; x < gridSize.x; x++){
                    Vector2i curr = new Vector2i(x, y);
                    clearCell(curr, true, false);
                }
            }
            fillGaps();
        }

        private void buzzyBirdVerticalBehaviour(Vector2i pos, Integer size) {
            
            Integer d = Math.floorDiv(size, 2);

            for(int x = pos.x-d; x <= pos.x+d; x++){
                for(int y = 0; y < gridSize.y; y++){
                    Vector2i curr = new Vector2i(x, y);
                    clearCell(curr, true, false);
                }
            }
            fillGaps();
        }

        private void boomBirdBehaviour(Vector2i pos, Integer size) {

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

                    if(getCell(new Vector2i(x, y)).TileType == refType && !getCell(new Vector2i(x, y)).isBoosted()) {
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
                         !cell1.isBoosted() && !cell2.isBoosted() &&
                         cell1.getHealth() == 0 && cell1.getHealth() == 0){
                
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
                if(getCell(pos).getBooster() != getCell(curr).getBooster()) continue;
                if(getCell(pos).getHealth() != getCell(curr).getHealth()) continue;

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
                    (horizontalConnections == 1 && verticalConnections == 2)) {
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

                nextToMatchEffects(new Vector2i(node.x-1, node.y));   // break the eggs surrounding a match
                nextToMatchEffects(new Vector2i(node.x+1, node.y));
                nextToMatchEffects(new Vector2i(node.x, node.y-1));
                nextToMatchEffects(new Vector2i(node.x, node.y+1));
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
            score += matchComboScore;
            dizzyBirdMeter += scoreIncrease / 30;
        }

        public Boolean updateLevel(){ // Returns true if level is incremented.

            moveCount = 0;

            Integer roundAccuracy = 100;
            Integer firstLvlScore = 1000;
            Float growthRate = 5f;

            Integer scoreRequirement = (int) (roundAccuracy * Math.round((firstLvlScore * Math.log1p(growthRate * level - growthRate + 1f) + firstLvlScore) / roundAccuracy));
            if(score > scoreRequirement){
                level++;
                return true;
            } else {
                return false;
            }
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
        
        
        // send timestamp to start a game
        currentGameUUID = UUID.randomUUID().toString();
        level = 1;
        gameState = new GameState();

        XtWriter mmData = new XtWriter();
        mmData.writeString(currentGameUUID);
        mmData.writeInt(gameState.calculateBoardChecksum()); // this is a checksum, not a timestamp
        mmData.writeInt(level);
        mmData.writeInt(0);
        mmData.writeInt(score);
        mmData.writeInt(0);

        MinigameMessagePacket mm = new MinigameMessagePacket();
		mm.command = "startGame";
		mm.data = mmData.encode().substring(4);
		player.client.sendPacket(mm);

        syncClient(player, rd);

    }

    @MinigameMessage("move")
	public void move(Player player, XtReader rd) {

        int clientChecksum = rd.readInt();

        // process move sent by client
        gameState.calculateMove(new Vector2i(rd.readInt(), rd.readInt()), new Vector2i(rd.readInt(), rd.readInt()));
        if (clientChecksum != gameState.calculateBoardChecksum()) {
            syncClient(player, rd);
        }

        // save inventory
        InventoryItemPacket pk = new InventoryItemPacket();
        pk.item = player.account.getSaveSpecificInventory().getItem("303");
        player.client.sendPacket(pk);

        // check if level should increase
        if (gameState.updateLevel()) {
            goToLevel(player);
        }

    }

	private void goToLevel(Player player) {
        // change to next level
        XtWriter mmData = new XtWriter();
        mmData.writeInt(level);
        mmData.writeInt(0);
        mmData.writeInt(score);
        mmData.writeInt(0);

        MinigameMessagePacket mm = new MinigameMessagePacket();
		mm.command = "goToLevel";
		mm.data = mmData.encode().substring(4);
		player.client.sendPacket(mm);

    }

    @MinigameMessage("dizzyBird")
    public void dizzyBird(Player player, XtReader rd) {
        dizzyBirdMeter = 0;
        gameState.scrambleTiles();
        syncClient(player, rd);
    }

    @MinigameMessage("continueGame")
	public void continueGame(Player player, XtReader rd) {
        // send start game client with previous values
    }

    @MinigameMessage("saveGame")
	public void saveGame(Player player, XtReader rd) {

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
        //mmData.writeString("8/T19vf4+fr7/P3+/w==");
        mmData.writeInt(0); // no level objective

        MinigameMessagePacket mm = new MinigameMessagePacket();
		mm.command = "syncClient";
		mm.data = mmData.encode().substring(4);
		player.client.sendPacket(mm);
    }
    
}
