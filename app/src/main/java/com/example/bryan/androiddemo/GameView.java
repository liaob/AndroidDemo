package com.example.bryan.androiddemo;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;

import java.util.ArrayList;

public class GameView extends View{

    Paint paint = new Paint();
    public Game game;
    public boolean hasSaveState = false;
    private final int numCellTypes = 12;
    public boolean continueButtonEnabled = false;

    private int cellSize = 0;
    private float textSize = 0;
    private float cellTextSize = 0;
    private int gridWidth = 0;
    private int TEXT_BLACK;
    private int TEXT_WHITE;
    private int TEXT_BROWN;
    public int startingX;
    public int startingY;
    public int endingX;
    public int endingY;
    private int textPaddingSize;
    private int iconPaddingSize;

    // Assets
    private Drawable backgroundRectangle;
    private Drawable[] cellRectangle = new Drawable[numCellTypes];
    private BitmapDrawable[] bitmapCell = new BitmapDrawable[numCellTypes];
    private Drawable newGameIcon;
    private Bitmap background = null;
    private BitmapDrawable loseGameOverlay;
    private BitmapDrawable winGameFinalOverlay;

    // Text variables
    private int sYAll;
    private int titleStartYAll;
    private int bodyStartYAll;
    private int eYAll;
    private int titleWidthHighScore;
    private int titleWidthScore;

    // Icon variables
    public int sYIcons;
    public int sXNewGame;
    public int iconSize;

    // Text values
    private String headerText;
    private String highScoreTitle;
    private String scoreTitle;
    private String instructionsText;
    private String winText;
    private String loseText;
    private String continueText;
    private String forNowText;

    long lastFPSTime = System.nanoTime();
    long currentTime = System.nanoTime();

    float titleTextSize;
    float bodyTextSize;
    float headerTextSize;
    float instructionsTextSize;
    float gameOverTextSize;

    boolean refreshLastTime = true;

    static final int BASE_ANIMATION_TIME = 100000000;

    static final float MERGING_ACCELERATION = (float) -0.5;
    static final float INITIAL_VELOCITY = (1 - MERGING_ACCELERATION) / 4;

    @Override
    public void onDraw(Canvas canvas) {
        // Reset the transparency of the screen

        canvas.drawBitmap(background, 0, 0, paint);

        drawScoreText(canvas);

        if (!game.isActive() && !game.aGrid.isAnimationActive()) {
            drawNewGameButton(canvas, true);
        }

        drawCells(canvas);

        if (!game.isActive()) {
            drawEndGameState(canvas);
        }

        // Refresh the screen if there is still an animation running
        if (game.aGrid.isAnimationActive()) {
            invalidate(startingX, startingY, endingX, endingY);
            tick();
            // Refresh one last time on game end.
        } else if (!game.isActive() && refreshLastTime) {
            invalidate();
            refreshLastTime = false;
        }
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldw, int oldh) {
        super.onSizeChanged(width, height, oldw, oldh);
        getLayout(width, height);
        createBackgroundBitmap(width, height);
        createBitmapCells();
        createOverlays();
    }

    private void drawDrawable(Canvas canvas, Drawable draw, int startingX,
                              int startingY, int endingX, int endingY) {
        draw.setBounds(startingX, startingY, endingX, endingY);
        draw.draw(canvas);
    }

    private void drawScoreText(Canvas canvas) {
        // Drawing the score text: Ver 2
        paint.setTextSize(bodyTextSize);
        paint.setTextAlign(Paint.Align.CENTER);

        int bodyWidthHighScore = (int) (paint.measureText("" + game.highScore));
        int bodyWidthScore = (int) (paint.measureText("" + game.score));

        int textWidthHighScore = Math.max(titleWidthHighScore,
                bodyWidthHighScore) + textPaddingSize * 2;
        int textWidthScore = Math.max(titleWidthScore, bodyWidthScore)
                + textPaddingSize * 2;

        int textMiddleHighScore = textWidthHighScore / 2;
        int textMiddleScore = textWidthScore / 2;

        int eXHighScore = endingX;
        int sXHighScore = eXHighScore - textWidthHighScore;

        int eXScore = sXHighScore - textPaddingSize;
        int sXScore = eXScore - textWidthScore;

        // Outputting high-scores box
        backgroundRectangle.setBounds(sXHighScore, sYAll, eXHighScore, eYAll);
        backgroundRectangle.draw(canvas);
        paint.setTextSize(titleTextSize);
        paint.setColor(TEXT_BROWN);
        canvas.drawText(highScoreTitle, sXHighScore + textMiddleHighScore,
                titleStartYAll, paint);
        paint.setTextSize(bodyTextSize);
        paint.setColor(TEXT_WHITE);
        canvas.drawText(String.valueOf(game.highScore), sXHighScore
                + textMiddleHighScore, bodyStartYAll, paint);

        // Outputting scores box
        backgroundRectangle.setBounds(sXScore, sYAll, eXScore, eYAll);
        backgroundRectangle.draw(canvas);
        paint.setTextSize(titleTextSize);
        paint.setColor(TEXT_BROWN);
        canvas.drawText(scoreTitle, sXScore + textMiddleScore, titleStartYAll,
                paint);
        paint.setTextSize(bodyTextSize);
        paint.setColor(TEXT_WHITE);
        canvas.drawText(String.valueOf(game.score), sXScore + textMiddleScore,
                bodyStartYAll, paint);
    }

    private void drawNewGameButton(Canvas canvas, boolean lightUp) {
        drawDrawable(canvas, backgroundRectangle, sXNewGame, sYIcons,
                    sXNewGame + iconSize, sYIcons + iconSize);
        drawDrawable(canvas, newGameIcon, sXNewGame + iconPaddingSize, sYIcons
                        + iconPaddingSize, sXNewGame + iconSize - iconPaddingSize,
                sYIcons + iconSize - iconPaddingSize);
    }

    private void drawHeader(Canvas canvas) {
        // Drawing the header
        paint.setTextSize(headerTextSize);
        paint.setColor(TEXT_BLACK);
        paint.setTextAlign(Paint.Align.LEFT);
        int textShiftY = centerText() * 2;
        int headerStartY = sYAll - textShiftY;
        canvas.drawText(headerText, startingX, headerStartY, paint);
    }

    public void drawInstructions(Canvas canvas) {
        // Drawing the instructions
        paint.setTextSize(instructionsTextSize);
        paint.setTextAlign(Paint.Align.LEFT);
        int textShiftY = centerText() * 5;
        canvas.drawText(instructionsText, startingX, endingY - textShiftY
                + textPaddingSize, paint);
    }

    private void drawBackground(Canvas canvas) {
        drawDrawable(canvas, backgroundRectangle, startingX, startingY,
                endingX, endingY);
    }

    private void drawBackgroundGrid(Canvas canvas) {
        // Outputting the game grid
        for (int xx = 0; xx < game.numSquaresX; xx++) {
            for (int yy = 0; yy < game.numSquaresY; yy++) {
                int sX = startingX + gridWidth + (cellSize + gridWidth) * xx;
                int eX = sX + cellSize;
                int sY = startingY + gridWidth + (cellSize + gridWidth) * yy;
                int eY = sY + cellSize;

                drawDrawable(canvas, cellRectangle[0], sX, sY, eX, eY);
            }
        }
    }

    private void drawCells(Canvas canvas) {
        paint.setTextSize(textSize);
        paint.setTextAlign(Paint.Align.CENTER);
        // Outputting the individual cells
        for (int xx = 0; xx < game.numSquaresX; xx++) {
            for (int yy = 0; yy < game.numSquaresY; yy++) {
                int sX = startingX + gridWidth + (cellSize + gridWidth) * xx;
                int eX = sX + cellSize;
                int sY = startingY + gridWidth + (cellSize + gridWidth) * yy;
                int eY = sY + cellSize;

                Tile currentTile = game.grid.getCellContent(xx, yy);
                if (currentTile != null) {
                    // Get and represent the value of the tile
                    int value = currentTile.getValue();
                    int index = log2(value);

                    // Check for any active animations
                    ArrayList<AnimationCell> aArray = game.aGrid
                            .getAnimationCell(xx, yy);
                    boolean animated = false;
                    for (int i = aArray.size() - 1; i >= 0; i--) {
                        AnimationCell aCell = aArray.get(i);
                        // If this animation is not active, skip it
                        if (aCell.getAnimationType() == Game.SPAWN_ANIMATION) {
                            animated = true;
                        }
                        if (!aCell.isActive()) {
                            continue;
                        }

                        if (aCell.getAnimationType() == Game.SPAWN_ANIMATION) { // Spawning
                            // animation
                            double percentDone = aCell.getPercentageDone();
                            float textScaleSize = (float) (percentDone);
                            paint.setTextSize(textSize * textScaleSize);

                            float cellScaleSize = cellSize / 2
                                    * (1 - textScaleSize);
                            bitmapCell[index].setBounds(
                                    (int) (sX + cellScaleSize),
                                    (int) (sY + cellScaleSize),
                                    (int) (eX - cellScaleSize),
                                    (int) (eY - cellScaleSize));
                            bitmapCell[index].draw(canvas);
                        } else if (aCell.getAnimationType() == Game.MERGE_ANIMATION) { // Merging
                            // Animation
                            double percentDone = aCell.getPercentageDone();
                            float textScaleSize = (float) (1 + INITIAL_VELOCITY
                                    * percentDone + MERGING_ACCELERATION
                                    * percentDone * percentDone / 2);
                            paint.setTextSize(textSize * textScaleSize);

                            float cellScaleSize = cellSize / 2
                                    * (1 - textScaleSize);
                            bitmapCell[index].setBounds(
                                    (int) (sX + cellScaleSize),
                                    (int) (sY + cellScaleSize),
                                    (int) (eX - cellScaleSize),
                                    (int) (eY - cellScaleSize));
                            bitmapCell[index].draw(canvas);
                        } else if (aCell.getAnimationType() == Game.MOVE_ANIMATION) { // Moving
                            // animation
                            double percentDone = aCell.getPercentageDone();
                            int tempIndex = index;
                            if (aArray.size() >= 2) {
                                tempIndex = tempIndex - 1;
                            }
                            int previousX = aCell.extras[0];
                            int previousY = aCell.extras[1];
                            int currentX = currentTile.getX();
                            int currentY = currentTile.getY();
                            int dX = (int) ((currentX - previousX)
                                    * (cellSize + gridWidth)
                                    * (percentDone - 1) * 1.0);
                            int dY = (int) ((currentY - previousY)
                                    * (cellSize + gridWidth)
                                    * (percentDone - 1) * 1.0);
                            bitmapCell[tempIndex].setBounds(sX + dX, sY + dY,
                                    eX + dX, eY + dY);
                            bitmapCell[tempIndex].draw(canvas);
                        }
                        animated = true;
                    }

                    // No active animations? Just draw the cell
                    if (!animated) {
                        bitmapCell[index].setBounds(sX, sY, eX, eY);
                        bitmapCell[index].draw(canvas);
                    }
                }
            }
        }
    }

    private void drawEndGameState(Canvas canvas) {
        double alphaChange = 1;
        continueButtonEnabled = false;
        for (AnimationCell animation : game.aGrid.globalAnimation) {
            if (animation.getAnimationType() == Game.FADE_GLOBAL_ANIMATION) {
                alphaChange = animation.getPercentageDone();
            }
        }
        BitmapDrawable displayOverlay = null;
        if (game.gameWon()) {
                displayOverlay = winGameFinalOverlay;
            }
        else {
            displayOverlay = loseGameOverlay;
        }
        if (displayOverlay != null) {
            displayOverlay.setBounds(startingX, startingY, endingX, endingY);
            displayOverlay.setAlpha((int) (255 * alphaChange));
            displayOverlay.draw(canvas);
        }
    }

    private void createEndGameStates(Canvas canvas, boolean win,
                                     boolean showButton) {
        int width = endingX - startingX;
        int length = endingY - startingY;
        int middleX = width / 2;
        int middleY = length / 2;
        if (win) {
            paint.setColor(TEXT_WHITE);
            paint.setAlpha(255);
            paint.setTextSize(gameOverTextSize);
            paint.setTextAlign(Paint.Align.CENTER);
            int textBottom = middleY - centerText();
            canvas.drawText(winText, middleX, textBottom, paint);
            paint.setTextSize(bodyTextSize);
            String text = showButton ? continueText : forNowText;
            canvas.drawText(text, middleX, textBottom + textPaddingSize * 2
                    - centerText() * 2, paint);
        } else {
            paint.setColor(TEXT_BLACK);
            paint.setAlpha(255);
            paint.setTextSize(gameOverTextSize);
            paint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText(loseText, middleX, middleY - centerText(), paint);
        }
    }

    private void createBackgroundBitmap(int width, int height) {
        background = Bitmap
                .createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(background);
        drawHeader(canvas);
        drawNewGameButton(canvas, false);
        drawBackground(canvas);
        drawBackgroundGrid(canvas);
        drawInstructions(canvas);

    }

    private void createBitmapCells() {
        paint.setTextSize(cellTextSize);
        paint.setTextAlign(Paint.Align.CENTER);
        Resources resources = getResources();
        for (int xx = 0; xx < bitmapCell.length; xx++) {
            Bitmap bitmap = Bitmap.createBitmap(cellSize, cellSize,
                    Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            drawDrawable(canvas, cellRectangle[xx], 0, 0, cellSize, cellSize);
            bitmapCell[xx] = new BitmapDrawable(resources, bitmap);
        }
    }

    private void createOverlays() {
        Resources resources = getResources();
        // Initalize overlays
        Bitmap bitmap = Bitmap.createBitmap(endingX - startingX, endingY
                - startingY, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        createEndGameStates(canvas, true, true);
        bitmap = Bitmap.createBitmap(endingX - startingX, endingY - startingY,
                Bitmap.Config.ARGB_8888);
        canvas = new Canvas(bitmap);
        createEndGameStates(canvas, true, false);
        winGameFinalOverlay = new BitmapDrawable(resources, bitmap);
        bitmap = Bitmap.createBitmap(endingX - startingX, endingY - startingY,
                Bitmap.Config.ARGB_8888);
        canvas = new Canvas(bitmap);
        createEndGameStates(canvas, false, false);
        loseGameOverlay = new BitmapDrawable(resources, bitmap);
    }

    private void tick() {
        currentTime = System.nanoTime();
        game.aGrid.tickAll(currentTime - lastFPSTime);
        lastFPSTime = currentTime;
    }

    public void resyncTime() {
        lastFPSTime = System.nanoTime();
    }

    private static int log2(int n) {
        if (n <= 0)
            throw new IllegalArgumentException();
        return 31 - Integer.numberOfLeadingZeros(n);
    }

    private void getLayout(int width, int height) {
        cellSize = Math.min(width / (game.numSquaresX + 1), height
                / (game.numSquaresY + 3));
        gridWidth = cellSize / 7;
        int screenMiddleX = width / 2;
        int screenMiddleY = height / 2;
        int boardMiddleX = screenMiddleX;
        int boardMiddleY = screenMiddleY + cellSize / 2;
        iconSize = cellSize / 2;

        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(cellSize);
        textSize = cellSize * cellSize
                / Math.max(cellSize, paint.measureText("0000"));
        cellTextSize = textSize * 0.9f;
        titleTextSize = textSize / 3;
        bodyTextSize = (int) (textSize / 1.5);
        instructionsTextSize = (int) (textSize / 1.8);
        headerTextSize = textSize * 2;
        gameOverTextSize = textSize * 2;
        textPaddingSize = (int) (textSize / 3);
        iconPaddingSize = (int) (textSize / 5);

        // Grid Dimensions
        double halfNumSquaresX = game.numSquaresX / 2d;
        double halfNumSquaresY = game.numSquaresY / 2d;

        startingX = (int) (boardMiddleX - (cellSize + gridWidth)
                * halfNumSquaresX - gridWidth / 2);
        endingX = (int) (boardMiddleX + (cellSize + gridWidth)
                * halfNumSquaresX + gridWidth / 2);
        startingY = (int) (boardMiddleY - (cellSize + gridWidth)
                * halfNumSquaresY - gridWidth / 2);
        endingY = (int) (boardMiddleY + (cellSize + gridWidth)
                * halfNumSquaresY + gridWidth / 2);

        paint.setTextSize(titleTextSize);

        int textShiftYAll = centerText();
        // static variables
        sYAll = (int) (startingY - cellSize * 1.5);
        titleStartYAll = (int) (sYAll + textPaddingSize + titleTextSize / 2 - textShiftYAll);
        bodyStartYAll = (int) (titleStartYAll + textPaddingSize + titleTextSize
                / 2 + bodyTextSize / 2);

        titleWidthHighScore = (int) (paint.measureText(highScoreTitle));
        titleWidthScore = (int) (paint.measureText(scoreTitle));
        paint.setTextSize(bodyTextSize);
        textShiftYAll = centerText();
        eYAll = (int) (bodyStartYAll + textShiftYAll + bodyTextSize / 2 + textPaddingSize);

        sYIcons = (startingY + eYAll) / 2 - iconSize / 2;
        sXNewGame = (endingX - iconSize);
        resyncTime();
    }

    private int centerText() {
        return (int) ((paint.descent() + paint.ascent()) / 2);
    }

    public GameView(Context context) {
        super(context);
        Resources resources = context.getResources();
        // Loading resources
        game = new Game(context, this);
        try {
            // Getting text values
            headerText = resources.getString(R.string.header);
            highScoreTitle = resources.getString(R.string.high_score);
            scoreTitle = resources.getString(R.string.score);
            instructionsText = resources.getString(R.string.instructions);
            winText = resources.getString(R.string.you_win);
            loseText = resources.getString(R.string.game_over);
            continueText = resources.getString(R.string.go_on);
            forNowText = resources.getString(R.string.for_now);
            // Getting assets
            backgroundRectangle = resources.getDrawable(R.drawable.background_rectangle);

            cellRectangle[0] = resources.getDrawable(R.drawable.cell_rectangle);
            cellRectangle[1] = resources.getDrawable(R.drawable.ic_2048_red);
            cellRectangle[2] = resources.getDrawable(R.drawable.ic_2048_orange);
            cellRectangle[3] = resources.getDrawable(R.drawable.ic_2048_yellow);
            cellRectangle[4] = resources.getDrawable(R.drawable.ic_2048_green);
            cellRectangle[5] = resources.getDrawable(R.drawable.ic_2048_teal);
            cellRectangle[6] = resources.getDrawable(R.drawable.ic_2048_blue);
            cellRectangle[7] = resources.getDrawable(R.drawable.ic_2048_purple);
            cellRectangle[8] = resources.getDrawable(R.drawable.ic_2048_pink);
            cellRectangle[9] = resources.getDrawable(R.drawable.ic_2048_brown);
            cellRectangle[10] = resources.getDrawable(R.drawable.ic_2048_gray);
            cellRectangle[11] = resources.getDrawable(R.drawable.ic_2048_black);

            newGameIcon = resources.getDrawable(R.drawable.ic_launcher_background);

            TEXT_WHITE = resources.getColor(R.color.text_white);
            TEXT_BLACK = resources.getColor(R.color.text_black);
            TEXT_BROWN = resources.getColor(R.color.text_brown);
            this.setBackgroundColor(resources.getColor(R.color.background));
        } catch (Exception e) {
            System.out.println("Error getting assets?");
        }
        setOnTouchListener(new InputListener(this));
        game.newGame();
    }

}
