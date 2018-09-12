package io.github.liaob.game2;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.SoundPool;
import android.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Game {
        public static final int SPAWN_ANIMATION = -1;
        public static final int MOVE_ANIMATION = 0;
        public static final int MERGE_ANIMATION = 1;

        public static final int FADE_GLOBAL_ANIMATION = 0;

        public static final long MOVE_ANIMATION_TIME = GameView.BASE_ANIMATION_TIME;
        public static final long SPAWN_ANIMATION_TIME = GameView.BASE_ANIMATION_TIME;
        public static final long NOTIFICATION_ANIMATION_TIME = GameView.BASE_ANIMATION_TIME * 5;
        public static final long NOTIFICATION_DELAY_TIME = MOVE_ANIMATION_TIME
                + SPAWN_ANIMATION_TIME;
        private static final String PERSONAL_BEST = "Personal Best";

    public static final int startingMaxValue = 2048;

        public static final int GAME_WIN = 1;
        public static final int GAME_LOST = -1;
        public static final int GAME_NORMAL = 0;
        public static final int GAME_NORMAL_WON = 1;
        public Grid grid = null;
        public AnimationGrid aGrid;
        final int numSquaresX = 4;
        final int numSquaresY = 4;
        final int startTiles = 2;

        public int gameState = 0;

        public long turns = 0;
        public String moves = "0";
        public String personalBest = "N/A";

        private SoundPool soundPool;

        private Context mContext;

        private GameView mView;

        public Game(Context context, GameView view) {
            mContext = context;
            mView = view;
            initSoundPool();
        }

        public void newGame() {
            if (grid == null) {
                grid = new Grid(numSquaresX, numSquaresY);
            } else {
                grid.clearGrid();
            }
            aGrid = new AnimationGrid(numSquaresX, numSquaresY);
            personalBest = getPersonalBest();
            turns = 0;
            moves = "0";
            gameState = GAME_NORMAL;
            addStartTiles();
            mView.refreshLastTime = true;
            mView.resyncTime();
            mView.invalidate();
        }

        private void addStartTiles() {
            for (int xx = 0; xx < startTiles; xx++) {
                this.addRandomTile();
            }
        }

        private void addRandomTile() {
            if (grid.isCellsAvailable()) {
                Tile tile = new Tile(grid.randomAvailableCell(), 2);
                spawnTile(tile);
            }
        }

        private void spawnTile(Tile tile) {
            grid.insertTile(tile);
            aGrid.startAnimation(tile.getX(), tile.getY(), SPAWN_ANIMATION,
                    SPAWN_ANIMATION_TIME, MOVE_ANIMATION_TIME, null);
        }

        private void recordHighScore() {
            SharedPreferences settings = PreferenceManager
                    .getDefaultSharedPreferences(mContext);
            SharedPreferences.Editor editor = settings.edit();
            editor.putString(PERSONAL_BEST, personalBest);
            editor.apply();
        }

        private String getPersonalBest() {
            SharedPreferences settings = PreferenceManager
                    .getDefaultSharedPreferences(mContext);
            return settings.getString(PERSONAL_BEST, "N/A");
        }

        private void prepareTiles() {
            for (Tile[] array : grid.field) {
                for (Tile tile : array) {
                    if (grid.isCellOccupied(tile)) {
                        tile.setMergedFrom(null);
                    }
                }
            }
        }

        private void moveTile(Tile tile, Cell cell) {
            grid.field[tile.getX()][tile.getY()] = null;
            grid.field[cell.getX()][cell.getY()] = tile;
            tile.updatePosition(cell);
        }

        public boolean gameWon() {
            return (gameState > 0 && gameState % 2 != 0);
        }

        public boolean gameLost() {
            return (gameState == GAME_LOST);
        }

        public boolean isActive() {
            return !(gameWon() || gameLost());
        }

        public void move(int direction) {
            aGrid.cancelAnimations();
            // 0: up, 1: right, 2: down, 3: left
            if (!isActive()) {
                return;
            }
            Cell vector = getVector(direction);
            List<Integer> traversalsX = buildTraversalsX(vector);
            List<Integer> traversalsY = buildTraversalsY(vector);
            boolean moved = false;

            prepareTiles();
            playClick();

            for (int xx : traversalsX) {
                for (int yy : traversalsY) {
                    Cell cell = new Cell(xx, yy);
                    Tile tile = grid.getCellContent(cell);

                    if (tile != null) {
                        Cell[] positions = findFarthestPosition(cell, vector);
                        Tile next = grid.getCellContent(positions[1]);

                        if (next != null && next.getValue() == tile.getValue()
                                && next.getMergedFrom() == null) {

                            Tile merged = new Tile(positions[1],
                                    tile.getValue() * 2);
                            Tile[] temp = {tile, next};
                            merged.setMergedFrom(temp);

                            grid.insertTile(merged);
                            grid.removeTile(tile);

                            // Converge the two tiles' positions
                            tile.updatePosition(positions[1]);

                            int[] extras = {xx, yy};
                            aGrid.startAnimation(merged.getX(), merged.getY(),
                                    MOVE_ANIMATION, MOVE_ANIMATION_TIME, 0, extras); // Direction:

                            aGrid.startAnimation(merged.getX(), merged.getY(),
                                    MERGE_ANIMATION, SPAWN_ANIMATION_TIME,
                                    MOVE_ANIMATION_TIME, null);

                            // The mighty 2048 tile
                            if (merged.getValue() >= winValue() && !gameWon()) {
                                gameState = gameState + GAME_WIN; // Set win state
                                endGame();
                            }
                        } else {
                            moveTile(tile, positions[0]);
                            int[] extras = {xx, yy, 0};
                            aGrid.startAnimation(positions[0].getX(),
                                    positions[0].getY(), MOVE_ANIMATION,
                                    MOVE_ANIMATION_TIME, 0, extras); // Direction: 1

                        }

                        if (!positionsEqual(cell, tile)) {
                            moved = true;
                        }
                    }
                }
            }

            if (moved) {
                turns++;
                moves = String.valueOf(turns);
                addRandomTile();
                checkLose();
            }
            mView.resyncTime();
            mView.invalidate();
        }

        private void checkLose() {
            if (!movesAvailable() && !gameWon()) {
                gameState = GAME_LOST;
                loseGame();
            }
            else if(gameWon()){
                endGame();
            }
            else{
            }
        }

    private void loseGame() {
        aGrid.startAnimation(-1, -1, FADE_GLOBAL_ANIMATION,
                NOTIFICATION_ANIMATION_TIME, NOTIFICATION_DELAY_TIME, null);
    }

        private void endGame() {
            aGrid.startAnimation(-1, -1, FADE_GLOBAL_ANIMATION,
                    NOTIFICATION_ANIMATION_TIME, NOTIFICATION_DELAY_TIME, null);
            if(personalBest.equals("N/A")){
                personalBest = moves;
                recordHighScore();
            }
            else if (turns <= Integer.parseInt(personalBest)) {
                personalBest = moves;
                recordHighScore();
            }
        }

        private Cell getVector(int direction) {
            Cell[] map = {new Cell(0, -1), // up
                    new Cell(1, 0), // right
                    new Cell(0, 1), // down
                    new Cell(-1, 0) // left
            };
            return map[direction];
        }

        private List<Integer> buildTraversalsX(Cell vector) {
            List<Integer> traversals = new ArrayList<Integer>();

            for (int xx = 0; xx < numSquaresX; xx++) {
                traversals.add(xx);
            }
            if (vector.getX() == 1) {
                Collections.reverse(traversals);
            }

            return traversals;
        }

        private List<Integer> buildTraversalsY(Cell vector) {
            List<Integer> traversals = new ArrayList<Integer>();

            for (int xx = 0; xx < numSquaresY; xx++) {
                traversals.add(xx);
            }
            if (vector.getY() == 1) {
                Collections.reverse(traversals);
            }

            return traversals;
        }

        private Cell[] findFarthestPosition(Cell cell, Cell vector) {
            Cell previous;
            Cell nextCell = new Cell(cell.getX(), cell.getY());
            do {
                previous = nextCell;
                nextCell = new Cell(previous.getX() + vector.getX(),
                        previous.getY() + vector.getY());
            } while (grid.isCellWithinBounds(nextCell)
                    && grid.isCellAvailable(nextCell));

            Cell[] answer = {previous, nextCell};
            return answer;
        }

        private boolean movesAvailable() {
            return grid.isCellsAvailable() || tileMatchesAvailable();
        }

        private boolean tileMatchesAvailable() {
            Tile tile;

            for (int xx = 0; xx < numSquaresX; xx++) {
                for (int yy = 0; yy < numSquaresY; yy++) {
                    tile = grid.getCellContent(new Cell(xx, yy));

                    if (tile != null) {
                        for (int direction = 0; direction < 4; direction++) {
                            Cell vector = getVector(direction);
                            Cell cell = new Cell(xx + vector.getX(), yy
                                    + vector.getY());

                            Tile other = grid.getCellContent(cell);

                            if (other != null
                                    && other.getValue() == tile.getValue()) {
                                return true;
                            }
                        }
                    }
                }
            }

            return false;
        }

        private boolean positionsEqual(Cell first, Cell second) {
            return first.getX() == second.getX() && first.getY() == second.getY();
        }

        private int winValue() {
                return startingMaxValue;
            }

    private void initSoundPool() {
        soundPool = new SoundPool(1, AudioManager.STREAM_MUSIC, 0);
    }

    private void playClick(){
        AudioManager am = (AudioManager) mView.getContext().getSystemService(
                Context.AUDIO_SERVICE);
        float audioMaxVolumn = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        float audioCurrentVolumn = am
                .getStreamVolume(AudioManager.STREAM_MUSIC);
        float volumnRatio = audioCurrentVolumn / audioMaxVolumn;
        soundPool.play( soundPool.load(mView.getContext(), R.raw.click, 1), volumnRatio, volumnRatio, 1, 0, 1);
    }
}
