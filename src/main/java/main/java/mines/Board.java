package main.java.mines;

import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.Random;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class Board extends JPanel {

    private static final long serialVersionUID = 6195235521361212179L;

    private static final int NUM_IMAGES = 15;  
    private static final int CELL_SIZE = 15;

    /* ---- cell values ----------------------------------------------------- */
    private static final int COVER_FOR_CELL = 10;
    private static final int MARK_FOR_CELL   = 10;
    private static final int EMPTY_CELL      = 0;
    private static final int MINE_CELL       = 9;

    private static final int COVERED_MINE_CELL = MINE_CELL + COVER_FOR_CELL;
    private static final int MARKED_MINE_CELL  = COVERED_MINE_CELL + MARK_FOR_CELL;

    /* ---- drawing indices ------------------------------------------------- */
    private static final int DRAW_MINE           = 9;
    private static final int DRAW_COVER          = 10;
    private static final int DRAW_MARK_P1        = 11;  // Player 1 correct flag
    private static final int DRAW_WRONG_MARK_P1  = 12;  // Player 1 wrong flag
    private static final int DRAW_MARK_P2        = 13;  // Player 2 correct flag
    private static final int DRAW_WRONG_MARK_P2  = 14;  // Player 2 wrong flag

    /* ---- board state ----------------------------------------------------- */
    private int[] field;
    private int[] markers; // -1 = none, 0 = P1, 1 = P2
    private boolean inGame;
    private int minesLeft;
    private transient Image[] img;

    private int mines = 40;
    private int rows  = 16;
    private int cols  = 16;
    private int allCells;

    private final JLabel statusbar;
    private final Random random = new Random();

    /* ---- 2-player state -------------------------------------------------- */
    private int currentPlayer = 0;
    private int[] playerFlags = new int[2];
    private boolean gameWon = false;

    /* --------------------------------------------------------------------- */
    public Board(JLabel statusbar) {
        this.statusbar = statusbar;

        img = new Image[NUM_IMAGES];
        for (int i = 0; i < NUM_IMAGES; i++) {
            img[i] = new ImageIcon(
                    getClass().getClassLoader().getResource(i + ".gif")
            ).getImage();
        }

        setDoubleBuffered(true);
        addMouseListener(new MinesAdapter());
        newGame();
    }

    /* --------------------------------------------------------------------- */
    public void newGame() {
        initializeBoard();
        placeMinesRandomly();
        updateNeighborCounts();
    }

    private void initializeBoard() {
        inGame = true;
        minesLeft = mines;
        allCells = rows * cols;
        field = new int[allCells];

        Arrays.fill(field, COVER_FOR_CELL);
        markers = new int[allCells];
        Arrays.fill(markers, -1);
        playerFlags[0] = playerFlags[1] = 0;
        currentPlayer = 0;
        gameWon = false;
        statusbar.setText(getStatusText());
    }

    private String getStatusText() {
        return "Player " + (currentPlayer + 1) + "'s turn | Mines left: " + minesLeft +
               " | Flags: P1=" + playerFlags[0] + " P2=" + playerFlags[1];
    }

    private void placeMinesRandomly() {
        int placed = 0;
        while (placed < mines) {
            int pos = random.nextInt(allCells);
            if (field[pos] == COVER_FOR_CELL) {
                field[pos] = COVERED_MINE_CELL;
                placed++;
            }
        }
    }

    private void updateNeighborCounts() {
        for (int pos = 0; pos < allCells; pos++) {
            if (field[pos] == COVERED_MINE_CELL) {
                incrementNeighbors(pos);
            }
        }
    }

    public void incrementNeighbors(int minePos) {
        int row = minePos / cols;
        int col = minePos % cols;

        for (int dr = -1; dr <= 1; dr++) {
            for (int dc = -1; dc <= 1; dc++) {
                if (dr == 0 && dc == 0) continue;

                int nr = row + dr;
                int nc = col + dc;
                if (isValidCell(nr, nc)) {
                    int neighborPos = nr * cols + nc;
                    if (field[neighborPos] != COVERED_MINE_CELL) {
                        field[neighborPos]++;
                    }
                }
            }
        }
    }

    public boolean isValidCell(int row, int col) {
        return row >= 0 && row < rows && col >= 0 && col < cols;
    }

    /* --------------------------------------------------------------------- */
    public void findEmptyCells(int pos) {
        int row = pos / cols;
        int col = pos % cols;

        for (int dr = -1; dr <= 1; dr++) {
            for (int dc = -1; dc <= 1; dc++) {
                if (dr == 0 && dc == 0) continue;

                int nr = row + dr;
                int nc = col + dc;
                if (!isValidCell(nr, nc)) continue;

                int neighbour = nr * cols + nc;

                // Only reveal covered safe cells (10–18)
                if (field[neighbour] > MINE_CELL && field[neighbour] < COVERED_MINE_CELL) {
                    field[neighbour] -= COVER_FOR_CELL;

                    if (field[neighbour] == EMPTY_CELL) {
                        findEmptyCells(neighbour);
                    }
                }
            }
        }
    }

    /* --------------------------------------------------------------------- */
    @Override
    public void paint(Graphics g) {
        int numCovers = 0;

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                int idx = i * cols + j;
                int cell = field[idx];
                int marker = markers[idx];

                // Lose condition
                if (inGame && cell == MINE_CELL) {
                    inGame = false;
                }

                // Determine image to draw
                int drawIndex;

                if (!inGame) {
                    // Game over: show truth
                    if (cell == COVERED_MINE_CELL) {
                        drawIndex = DRAW_MINE;
                    } else if (cell == MARKED_MINE_CELL) {
                        // Correct flag: show player's flag
                        drawIndex = (marker == 0) ? DRAW_MARK_P1 : DRAW_MARK_P2;
                    } else if (cell > COVERED_MINE_CELL) {
                        // Wrong flag
                        drawIndex = (marker == 0) ? DRAW_WRONG_MARK_P1 : DRAW_WRONG_MARK_P2;
                    } else if (cell > MINE_CELL) {
                        drawIndex = DRAW_COVER;
                    } else {
                        drawIndex = cell; // revealed number or empty
                    }
                } else {
                    // In game
                    if (cell > COVERED_MINE_CELL) {
                        // Flagged cell
                        drawIndex = (marker == 0) ? DRAW_MARK_P1 : DRAW_MARK_P2;
                    } else if (cell > MINE_CELL) {
                        drawIndex = DRAW_COVER;
                        numCovers++;
                    } else {
                        drawIndex = cell; // revealed
                    }
                }

                g.drawImage(img[drawIndex], j * CELL_SIZE, i * CELL_SIZE, this);
            }
        }

        // Win condition
        if (numCovers == 0 && inGame) {
            inGame = false;
            gameWon = true;
        }

        // Update status
        if (!inGame) {
            if (gameWon) {
                int p1 = playerFlags[0], p2 = playerFlags[1];
                String result = p1 > p2 ? " Player 1 wins!" :
                               p2 > p1 ? " Player 2 wins!" : " It's a draw!";
                statusbar.setText("Game won! P1: " + p1 + " | P2: " + p2 + result);
            } else {
                int loser = currentPlayer;
                int winner = 1 - loser;
                statusbar.setText("Player " + (loser + 1) + " hit a mine! Player " + (winner + 1) + " wins!");
            }
        } else {
            statusbar.setText(getStatusText());
        }
    }

    /* --------------------------------------------------------------------- */
    public static int getCoverForCell()   { return COVER_FOR_CELL; }
    public static int getCoveredMineCell(){ return COVERED_MINE_CELL; }

    /* --------------------------------------------------------------------- */
    private class MinesAdapter extends MouseAdapter {
        @Override
        public void mousePressed(MouseEvent e) {
            if (!inGame) {
                newGame();
                repaint();
                return;
            }

            int x = e.getX();
            int y = e.getY();
            int cCol = x / CELL_SIZE;
            int cRow = y / CELL_SIZE;

            if (cRow < 0 || cCol < 0 || cRow >= rows || cCol >= cols) return;

            int pos = cRow * cols + cCol;
            boolean repaintNeeded = false;

            // Right-click: Flag / Unflag
            if (e.getButton() == MouseEvent.BUTTON3) {
                if (field[pos] <= MINE_CELL) return; // can't flag revealed

                boolean isMarked = (field[pos] > COVERED_MINE_CELL);
                if (isMarked) {
                    if (markers[pos] == currentPlayer) {
                        field[pos] -= MARK_FOR_CELL;
                        markers[pos] = -1;
                        playerFlags[currentPlayer]--;
                        minesLeft++;
                        repaintNeeded = true;
                        currentPlayer = 1 - currentPlayer;
                    }
                } else {
                    if (minesLeft > 0) {
                        field[pos] += MARK_FOR_CELL;
                        markers[pos] = currentPlayer;
                        playerFlags[currentPlayer]++;
                        minesLeft--;
                        repaintNeeded = true;
                        currentPlayer = 1 - currentPlayer;
                    }
                }
            }
            // Left-click: Reveal
            else {
                if (field[pos] > COVERED_MINE_CELL) return; // can't reveal flagged

                field[pos] -= COVER_FOR_CELL;
                repaintNeeded = true;

                if (field[pos] == MINE_CELL) {
                    inGame = false;
                } else if (field[pos] == EMPTY_CELL) {
                    findEmptyCells(pos);
                }

                if (inGame) {
                    currentPlayer = 1 - currentPlayer;
                }
            }

            if (repaintNeeded) {
                repaint();
            }
        }
    }
    
 // Add these getter methods to Board class for testing
    public int getCurrentPlayer() {
        return currentPlayer;
    }

    public boolean isInGame() {
        return inGame;
    }

    public int getMinesLeft() {
        return minesLeft;
    }

    public int[] getPlayerFlags() {
        return playerFlags.clone();
    }

    public int[] getField() {
        return field.clone();
    }

    public int[] getMarkers() {
        return markers.clone();
    }

    public int getRows() {
        return rows;
    }

    public int getCols() {
        return cols;
    }

    public int getAllCells() {
        return allCells;
    }

    // Package-private method for testing mine placement
    public void setFieldForTesting(int[] testField) {
        this.field = testField;
    }
}