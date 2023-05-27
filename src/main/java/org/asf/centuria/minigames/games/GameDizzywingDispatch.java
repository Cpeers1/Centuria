package org.asf.centuria.minigames.games;

import java.util.Queue;
import java.util.Random;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
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

        public class GridCell {
            private int tileHealth;
            private TileType TileType;
            private BoosterType Booster;

            public GridCell(int health, TileType tileType, BoosterType booster){
                tileHealth = health;
                TileType = tileType;
                Booster = booster;
            }

            public void SetTileType(TileType tileType){
                TileType = tileType;
            }

            public TileType GetTileType(){
                return TileType;
            }

            public void SetBooster(BoosterType booster){
                Booster = booster;
            }
            
            public BoosterType GetBooster(){
                return Booster;
            }

            public void AddHealth(Integer change){
                tileHealth = Math.max(tileHealth - change, 0);
            }

            public void SetHealth(Integer health){
                tileHealth = health;
            }
            
            public Integer GetHealth(){
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

        private GridCell[][] grid;
        private int visited[][];
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
            InitializeGameBoard();
            floodFillClearVisited();
        }

        public GridCell GetCell(Vector2i pos)
        {
            if (pos.x >= 0 && pos.x < gridSize.x && pos.y >= 0 && pos.y < gridSize.y)
            {
                return grid[pos.x][pos.y];
            }
            return null;
        }

        private void SetCell(Vector2i pos, GridCell cell){
            if (pos.x >= 0 && pos.x < gridSize.x && pos.y >= 0 && pos.y < gridSize.y && cell != null)
            {
                grid[pos.x][pos.y] = cell;
            }
        }

        private void ClearCell(Vector2i pos, Boolean isScore, Boolean forceClear){
            
            if (GetCell(pos) == null || GetCell(pos).GetTileType() == TileType.None) {
                return;
            }

            if(forceClear){
                SetCell(pos, new GridCell(0, TileType.None, BoosterType.None));
                return;
            } else if(GetCell(pos).TileType != TileType.HatOrPurse) {
                SetCell(pos, new GridCell(0, TileType.None, BoosterType.None));
            }

            if(isScore) {
                score += 30;
                dizzyBirdMeter += 30;
            }

            switch (GetCell(pos).GetBooster()) {
                case BuzzyBirdHorizontal: 
                    BuzzyBirdHorizontalBehaviour(pos, 1);
                    break;
                case BuzzyBirdVertical: 
                    BuzzyBirdVerticalBehaviour(pos, 1);
                    break;
                case BoomBird: 
                    BoomBirdBehaviour(pos, 3);
                    break;
                default:
                    break;
            }
        }

        private void ClearGrid(){
            grid = new GridCell[gridSize.x][gridSize.y];
        }
        
        public int CalculateBoardChecksum()
        {
            byte[] inArray = new byte[gridSize.x * gridSize.y];
            
            for(int x = 0; x < gridSize.x; x++){
                for(int y = 0; y < gridSize.y; y++){
                    inArray[x + gridSize.x * y] = GridCellByteValueForChecksum(new Vector2i(x, y));
                }
            }

            //Board checksum algorithm!
            String string1 = Base64.getEncoder().encodeToString(inArray);
            String string2 = currentGameUUID.toString();
            String string3 = moveCount.toString();
            return (string1 + string2 + string3).hashCode();
        }

        private byte GridCellByteValueForChecksum(Vector2i pos)
        {
            int byteValue = 0;
            GridCell cell = GetCell(pos);

            Integer tileTypeValue = cell.GetTileType().ordinal();

            if (cell.isBoosted()) {
                byteValue += (cell.isBoosted() ? 1 : 0) * 20 + tileTypeValue;
            } else {
                byteValue += tileTypeValue * 2 + cell.tileHealth;
            }

            return (byte)byteValue;
        }

        private void InitializeGameBoard()
        {

            ClearGrid();
            List<TileType> shuffledSpawnTiles = new ArrayList<TileType>(spawnTiles);

            for (int y = 0; y < gridSize.y; y++)
            {
                for (int x = 0; x < gridSize.x; x++)
                {
                    Collections.shuffle(shuffledSpawnTiles, randomizer);

                    for (TileType spawnTile : shuffledSpawnTiles)
                    {
                        if ((x < 2 || !(GetCell(new Vector2i(x - 1, y)).TileType == spawnTile) || 
                            !(GetCell(new Vector2i(x - 2, y)).TileType == spawnTile)) && 
                            (y < 2 || !(GetCell(new Vector2i(x, y - 1)).TileType == spawnTile) || 
                            !(GetCell(new Vector2i(x, y - 2)).TileType == spawnTile)))
                        {
                            SetCell(new Vector2i(x, y), new GridCell(0, spawnTile, BoosterType.None));
                            break;
                        }
                    }
                }
            }
            moveCount = 0;
        }

        public void CalculateMove(Vector2i pos1, Vector2i pos2){

            moveCount++;

            if(pos2.y != -1){   // if given two tiles as input

                GridCell cell1 = GetCell(pos1);
                SetCell(pos1, GetCell(pos2));
                SetCell(pos2, cell1);
    
                floodFillClearVisited();
    
                // Guarantee pos1 to have a more powerful or as powerful booster as pos2
                if(GetCell(pos2).GetBooster().ordinal() > GetCell(pos1).GetBooster().ordinal()){
                    Vector2i temp = pos1;
                    pos1 = pos2;
                    pos2 = temp;
                }
    
                if(GetCell(pos1).isBoosted() && GetCell(pos2).isBoosted()){ // booster combo behaviours
                    
                    if((GetCell(pos2).GetBooster() == BoosterType.BuzzyBirdHorizontal || 
                        GetCell(pos2).GetBooster() == BoosterType.BuzzyBirdVertical) &&
                        GetCell(pos1).GetBooster() == BoosterType.BoomBird){    // Buzzy bird and boom bird
    
                        ClearCell(pos1, false, true);
                        ClearCell(pos2, true, true);
        
                        BuzzyBirdHorizontalBehaviour(pos1, 3);
                        BuzzyBirdVerticalBehaviour(pos1, 3);
    
                    } else if (GetCell(pos1).GetBooster() == BoosterType.BoomBird &&
                            GetCell(pos2).GetBooster() == BoosterType.BoomBird){    // Boom bird and boom bird
    
                        ClearCell(pos1, false, true);
                        ClearCell(pos2, true, true);
            
                        BoomBirdBehaviour(pos1, 5);
    
                    } else if ((GetCell(pos1).GetBooster() == BoosterType.BuzzyBirdHorizontal || 
                                GetCell(pos1).GetBooster() == BoosterType.BuzzyBirdVertical) &&
                                (GetCell(pos2).GetBooster() == BoosterType.BuzzyBirdHorizontal || 
                                GetCell(pos2).GetBooster() == BoosterType.BuzzyBirdVertical)){  // Buzzy bird and buzzy bird
    
                        ClearCell(pos1, false, true);
                        ClearCell(pos2, true, true);
                
                        BuzzyBirdHorizontalBehaviour(pos1, 1);
                        BuzzyBirdVerticalBehaviour(pos1, 1);
    
                    } else if (GetCell(pos1).GetBooster() == BoosterType.PrismPeacock &&
                                (GetCell(pos2).GetBooster() == BoosterType.BuzzyBirdHorizontal || 
                                GetCell(pos2).GetBooster() == BoosterType.BuzzyBirdVertical)){  // Prism peacock and buzzy bird
    
                        TileType refType = GetCell(pos2).TileType;
    
                        ClearCell(pos1, false, true);
                        ClearCell(pos2, true, true);
    
                        List<Vector2i> newBuzzyBirds = new ArrayList<>();
    
                        for(int x = 0; x < gridSize.x; x++){
                            for(int y = 0; y < gridSize.y; y++){
                                Vector2i curr = new Vector2i(x, y);
    
                                if(GetCell(curr).GetTileType() == refType && !GetCell(curr).isBoosted()) {
                                    newBuzzyBirds.add(curr);
                                }
                            }
                        }
    
                        for (Vector2i newBuzzyBird : newBuzzyBirds) {
                            Boolean coinflip = randomizer.nextInt(2) == 1;
                            
                            if(coinflip) {
                                BuzzyBirdHorizontalBehaviour(newBuzzyBird, 1);
                            } else {
                                BuzzyBirdVerticalBehaviour(newBuzzyBird, 1);
                            }
                        }
    
                    } else if (GetCell(pos1).GetBooster() == BoosterType.PrismPeacock &&
                            (GetCell(pos2).GetBooster() == BoosterType.BoomBird)){  // Prism peacock and boom bird
    
                        TileType refType = GetCell(pos2).TileType;
    
                        ClearCell(pos1, false, true);
                        ClearCell(pos2, true, true);
    
                        List<Vector2i> newBoomBirds = new ArrayList<>();
    
                        for(int x = 0; x < gridSize.x; x++){
                            for(int y = 0; y < gridSize.y; y++){
                                Vector2i curr = new Vector2i(x, y);
    
                                if(GetCell(curr).GetTileType() == refType && !GetCell(curr).isBoosted()) {
                                    SetCell(curr, new GridCell(0, refType, BoosterType.BoomBird));
                                    newBoomBirds.add(curr);
                                }
                            }
                        }
    
                        Integer d = Math.floorDiv(3, 2);
    
                        for (Vector2i pos : newBoomBirds) {
                
                            for(int x = pos.x-d; x <= pos.x+d; x++){
                                for(int y = pos.y-d; y <= pos.y+d; y++){
        
                                    Vector2i curr = new Vector2i(x, y);
        
                                    if(GetCell(curr).GetBooster() != BoosterType.BoomBird) {
                                        ClearCell(curr, true, false);
                                    }
                                }
                            }
                        }
                        fillGaps();
    
                        newBoomBirds.clear();
    
                        for(int x = 0; x < gridSize.x; x++){
                            for(int y = 0; y < gridSize.y; y++){
                                Vector2i curr = new Vector2i(x, y);
    
                                if(GetCell(curr).GetTileType() == refType && GetCell(curr).GetBooster() == BoosterType.BoomBird) {
                                    newBoomBirds.add(curr);
                                }
                            }
                        }
    
                        for (Vector2i pos : newBoomBirds) {
                
                            for(int x = pos.x-d; x <= pos.x+d; x++){
                                for(int y = pos.y-d; y <= pos.y+d; y++){
        
                                    Vector2i curr = new Vector2i(x, y);
                                    ClearCell(curr, true, false);
                                }
                            }
                        }
    
    
                    } else if (GetCell(pos1).GetBooster() == BoosterType.PrismPeacock &&
                                GetCell(pos1).GetBooster() == BoosterType.PrismPeacock){    // Prism peacock and prism peacock
                                ClearCell(pos1, false, true);
                                ClearCell(pos2, false, true);
    
                        for(int x = 0; x < gridSize.x; x++){
                            for(int y = 0; y < gridSize.y; y++){
                                Vector2i curr = new Vector2i(x, y);
                                ClearCell(curr, true, false);
                            }
                        }
                    }
    
                } else if(GetCell(pos1).isBoosted() && !GetCell(pos2).isBoosted()){    // single booster behaviours
                    
                    switch (GetCell(pos1).GetBooster()) {
                        case BuzzyBirdHorizontal: 
                            BuzzyBirdHorizontalBehaviour(pos1, 1);
                            break;
                        case BuzzyBirdVertical: 
                            BuzzyBirdVerticalBehaviour(pos1, 1);
                            break;
                        case BoomBird: 
                            BoomBirdBehaviour(pos1, 3);
                            break;
                        case PrismPeacock: 
                            PrismPeacockBehaviour(pos1, GetCell(pos2).GetTileType());
                            break;
                        default:
                            break;
                    }
                }

            } else {    // only one tile as input

                switch (GetCell(pos1).GetBooster()) {
                    case BuzzyBirdHorizontal: 
                        BuzzyBirdHorizontalBehaviour(pos1, 1);
                        break;
                    case BuzzyBirdVertical: 
                        BuzzyBirdVerticalBehaviour(pos1, 1);
                        break;
                    case BoomBird: 
                        BoomBirdBehaviour(pos1, 3);
                        break;
                    default:
                        break;
                }

            }


            matchComboScore = 0;

            while(true){ // for as long as there are still matches on the game board

                Boolean isMatches = findAndClearMatches();
                fillGaps(); // replace tiles marked as having being cleared

                if(!isMatches){
                    break;
                }

            }
                
        }

        private void BuzzyBirdHorizontalBehaviour(Vector2i pos, Integer size) {
            
            Integer d = Math.floorDiv(size, 2);
            
            for(int y = pos.y-d; y <= pos.y+d; y++){
                for(int x = 0; x < gridSize.x; x++){
                    Vector2i curr = new Vector2i(x, y);
                    ClearCell(curr, true, false);
                }
            }
            fillGaps();
        }

        private void BuzzyBirdVerticalBehaviour(Vector2i pos, Integer size) {
            
            Integer d = Math.floorDiv(size, 2);

            for(int x = pos.x-d; x <= pos.x+d; x++){
                for(int y = 0; y < gridSize.y; y++){
                    Vector2i curr = new Vector2i(x, y);
                    ClearCell(curr, true, false);
                }
            }
            fillGaps();
        }

        private void BoomBirdBehaviour(Vector2i pos, Integer size) {

            Integer d = Math.floorDiv(size, 2);

            for(int x = pos.x-d; x <= pos.x+d; x++){
                for(int y = pos.y-d; y <= pos.y+d; y++){

                    Vector2i curr = new Vector2i(x, y);

                    if(x != pos.x && y != pos.y) {
                        ClearCell(curr, true, false);
                    }
                }
            }
            fillGaps();
            for(int x = pos.x-d; x <= pos.x+d; x++){
                for(int y = pos.y-d-1; y <= pos.y+d-1; y++){
                    Vector2i curr = new Vector2i(x, y);
                    ClearCell(curr, true, false);
                }
            }
            fillGaps();
        }

        private void PrismPeacockBehaviour(Vector2i pos, TileType refType) {

            ClearCell(pos, false, true);

            for(int y = 0; y < gridSize.y; y++){
                for(int x = 0; x < gridSize.x; x++){
                    Vector2i curr = new Vector2i(x, y);

                    if(GetCell(new Vector2i(x, y)).TileType == refType && !GetCell(new Vector2i(x, y)).isBoosted()) {
                        ClearCell(curr, true, false);
                    }
                }
            }
            fillGaps();
        }

        public void fillGaps(){
            for(int x = 0; x < gridSize.x; x++){
                Integer noGapsInColumn = 0;
                for(int y = 0; y < gridSize.y; y++){ // for each column
                    GridCell currCell = GetCell(new Vector2i(x, y));
                    if(currCell.TileType == TileType.None && currCell.Booster == BoosterType.None){
                        noGapsInColumn++;
                    } else {
                        SetCell(new Vector2i(x, y - noGapsInColumn), currCell); // move all the tiles down to eliminate gaps
                    }
                }
                for(int y = gridSize.y; y >= gridSize.y-noGapsInColumn; y--){
                    SetCell(new Vector2i(x, y), new GridCell(0, spawnTiles.get(randomizer.nextInt(spawnTiles.size())), BoosterType.None)); // now fill the gaps
                }
            }
        }

        public Boolean floodFillIsNeighbourCell(Vector2i pos1, Vector2i pos2){
            GridCell cell1 = GetCell(pos1);
            GridCell cell2 = GetCell(pos2);

            if(cell1 == null || cell2 == null) return false;
            if(cell1.TileType == cell2.TileType && cell1.Booster == cell2.Booster) return true;

            return false;
        }

        // 0 = unvisited
        // 1 = visited
        // 3 = match 3 (unvisited)
        // 4 = match 4 (unvisited)
        // 5 = match 5 (unvisited)
        // 6 = intersection of match 3s (unvisited)
        public void floodFillSetMatch(Vector2i pos, int matchType){
            visited[pos.x][pos.y] = matchType;
        }

        public int floodFillGetMatch(Vector2i pos){
            if (pos.x >= 0 && pos.x < gridSize.x && pos.y >= 0 && pos.y < gridSize.y)
            {
                return visited[pos.x][pos.y];
            }
            return 0;
        }

        //public void floodFillSetVisited(Vector2i pos){
        //    visited[pos.x][pos.y] = 1;
        //}

        //public boolean floodFillGetVisited(Vector2i pos){
        //    return visited[pos.x][pos.y] == 1;
        //}

        public void floodFillClearVisited(){
            visited = new int[gridSize.x][gridSize.y];
        }

        public void floodFill(Vector2i pos){
            
            TileType refType = GetCell(pos).TileType;
            BoosterType refBooster = BoosterType.None;

            if(floodFillGetMatch(pos) == 3) {
                scoreCombo(30);
            }
            else if(floodFillGetMatch(pos) == 4) {
                refBooster = randomizer.nextInt(2) == 0 ? BoosterType.BuzzyBirdHorizontal : BoosterType.BuzzyBirdVertical;
                scoreCombo(150);
            }
            else if(floodFillGetMatch(pos) == 5) {
                refBooster = BoosterType.PrismPeacock;
                //refType = TileType.None;
                scoreCombo(150);
            }
            else if(floodFillGetMatch(pos) == 6) {
                refBooster = BoosterType.BoomBird;
                scoreCombo(150);
            }
            
            Queue<Vector2i> floodFillQueue = new LinkedList<>();
            floodFillQueue.add(pos);

            List<Vector2i> connectedNodes = new LinkedList<>();

            while(!floodFillQueue.isEmpty()){
                Vector2i curr = floodFillQueue.poll();

                if(GetCell(curr) == null) continue;
                if(floodFillGetMatch(curr) <= 1) continue;
                if(GetCell(pos).TileType != GetCell(curr).TileType) continue;
                if(GetCell(pos).Booster != GetCell(curr).Booster) continue;

                floodFillSetMatch(curr, 1);
                connectedNodes.add(curr);

                floodFillQueue.add(new Vector2i(curr.x-1, curr.y));
                floodFillQueue.add(new Vector2i(curr.x+1, curr.y));
                floodFillQueue.add(new Vector2i(curr.x, curr.y-1));
                floodFillQueue.add(new Vector2i(curr.x, curr.y+1));
            }

           
            for(Vector2i node : connectedNodes){
                ClearCell(node, false, false);
            }
            if(refBooster != BoosterType.None){

                if(refBooster == BoosterType.PrismPeacock){
                    refType = TileType.None;
                }

                SetCell(pos, new GridCell(0, refType, refBooster));
            }

        }

        private void scoreCombo(int scoreIncrease) {
            matchComboScore += scoreIncrease;
            score += matchComboScore;
            dizzyBirdMeter += matchComboScore / 100;
        }
        
        // patterns are hardcoded
        public Boolean findAndClearMatches(){

            Boolean isMatch = false;

            // find all vertical matches
            for(int x = 0; x < gridSize.x; x++){
                Integer sameTilesColumn = 1;
                for(int y = 0; y < gridSize.y; y++){ // for each column
                    if(floodFillIsNeighbourCell(new Vector2i(x, y), new Vector2i(x, y+1))){ // keep track of number of tiles with the same tile type next to each other
                        sameTilesColumn++;
                        //System.out.print(sameTilesColumn + " " + x + " " + y + "\n");
                    } else if (sameTilesColumn > 2) {
                        //System.out.print(sameTilesColumn + " " + x + " " + y + "\n");
                        isMatch = true;
                        
                        for(int backtrack = y-sameTilesColumn+1; backtrack <= y; backtrack++){
                            floodFillSetMatch(new Vector2i(x, backtrack), sameTilesColumn); // record this number
                        }
                        sameTilesColumn = 1;
                    } else {
                        sameTilesColumn = 1;
                    }
                }
            }
            
            // find all horizontal matches
            for(int y = 0; y < gridSize.y; y++){
                Integer sameTilesRow = 1;
                for(int x = 0; x < gridSize.x; x++){ // for each column
                    if(floodFillIsNeighbourCell(new Vector2i(x+1, y), new Vector2i(x, y))){ // keep track of number of tiles with the same tile type next to each other
                        sameTilesRow++;
                        //System.out.print(sameTilesRow + " " + x + " " + y + "\n");
                    } else if (sameTilesRow > 2) {
                        //System.out.print(sameTilesRow + " " + x + " " + y + "\n");
                        isMatch = true;

                        for(int backtrack = x-sameTilesRow+1; backtrack <= x; backtrack++){
                            Integer horizontalConnections = 0;
                            Integer verticalConnections = 0;
                            
                            if(floodFillGetMatch(new Vector2i(backtrack-1, y)) >= 3 && floodFillIsNeighbourCell(new Vector2i(backtrack, y), new Vector2i(backtrack-1, y))) horizontalConnections++;
                            if(floodFillGetMatch(new Vector2i(backtrack+1, y)) >= 3 && floodFillIsNeighbourCell(new Vector2i(backtrack, y), new Vector2i(backtrack+1, y))) horizontalConnections++;
                            if(floodFillGetMatch(new Vector2i(backtrack, y-1)) >= 3 && floodFillIsNeighbourCell(new Vector2i(backtrack, y), new Vector2i(backtrack, y-1))) verticalConnections++;
                            if(floodFillGetMatch(new Vector2i(backtrack, y+1)) >= 3 && floodFillIsNeighbourCell(new Vector2i(backtrack, y), new Vector2i(backtrack, y+1))) verticalConnections++;

                            if(horizontalConnections > 0 && verticalConnections > 0){
                                floodFillSetMatch(new Vector2i(backtrack, y), 6); // same as before, but check if this is the intersection of 2 lines
                            } else {
                                floodFillSetMatch(new Vector2i(backtrack, y), sameTilesRow);
                            }
                        }
                        sameTilesRow = 1;
                    } else {
                        sameTilesRow = 1;
                    }
                }
            }

            // clear all T shaped and L shaped matches
            for(int x = 0; x < gridSize.x; x++){
                for(int y = 0; y < gridSize.y; y++){
                    Vector2i curr = new Vector2i(x, y);

                    if(floodFillGetMatch(curr) == 6) {
                        floodFill(curr);
                    }
                }
            }

            // clear all line-shaped matches
            for(int x = 0; x < gridSize.x; x++){
                for(int y = 0; y < gridSize.y; y++){
                    Vector2i curr = new Vector2i(x, y);

                    if(floodFillGetMatch(curr) >= 3) {
                        floodFill(curr);
                    }
                }
            }

            return isMatch;
        }

        public String toBase64String(){
            byte[] inArray = new byte[gridSize.x * gridSize.y];
            
            for(int x = 0; x < gridSize.x; x++){
                for(int y = 0; y < gridSize.y; y++){
                    inArray[x + gridSize.x * y] = GridCellByteValueForBase64String(new Vector2i(x, y));
                }
            }

            return Base64.getEncoder().encodeToString(inArray);
        }

        public byte GridCellByteValueForBase64String(Vector2i pos)
        {
            int byteValue = 0;
            GridCell cell = GetCell(pos);

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

        public Boolean UpdateLevel(){ // Returns true if level is incremented.

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

        public void scrambleTiles() {
            List<GridCell> scrambledTiles = new ArrayList<>();

            for(int x = 0; x < gridSize.x; x++){
                for(int y = 0; y < gridSize.y; y++){  
                    Vector2i curr = new Vector2i(x, y);
                    scrambledTiles.add(GetCell(curr));
                }
            }

            Collections.shuffle(scrambledTiles, randomizer);

            Integer tileNumber = 0;
            for(GridCell tile : scrambledTiles){
                SetCell(new Vector2i(tileNumber % gridSize.y, tileNumber / gridSize.y), tile);
                tileNumber++;
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
        mmData.writeInt(gameState.CalculateBoardChecksum()); // this is a checksum, not a timestamp
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
        gameState.CalculateMove(new Vector2i(rd.readInt(), rd.readInt()), new Vector2i(rd.readInt(), rd.readInt()));
        if (clientChecksum != gameState.CalculateBoardChecksum()) {
            syncClient(player, rd);
        }

        // save inventory
        InventoryItemPacket pk = new InventoryItemPacket();
        pk.item = player.account.getSaveSpecificInventory().getItem("303");
        player.client.sendPacket(pk);

        // check if level should increase
        if (gameState.UpdateLevel()) {
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
