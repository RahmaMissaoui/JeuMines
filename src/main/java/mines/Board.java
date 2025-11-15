package mines;

import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.security.SecureRandom;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
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
    private static final int MARKED_MINE_CELL  = COVERED_MINE_CELL + getMarkForCell();

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
    private final SecureRandom random = new SecureRandom();

    /* ---- 2-player state -------------------------------------------------- */
    private int currentPlayer = 0;
    private int[] playerFlags = new int[2];
    private boolean gameWon = false;
    private boolean gameEndDetected = false;

    /* --------------------------------------------------------------------- */
    private static final String PLAYER_PREFIX = "Player ";
    private static final String FLAG_LINE_TEMPLATE = "%s flags: %d";
    private static final String WIN_DRAW_MESSAGE_TEMPLATE = "%s\n%s\n%s";
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
        repaint();
    }

    private void initializeBoard() {
        setInGame(true);
        setGameWon(false);
        gameEndDetected = false;
        mines = 40;
        rows = 16;
        cols = 16;
        allCells = rows * cols;
        setMinesLeft(mines);

        field = new int[allCells];
        Arrays.fill(field, COVER_FOR_CELL);

        markers = new int[allCells];
        Arrays.fill(markers, -1);

        playerFlags[0] = playerFlags[1] = 0;
        setCurrentPlayer(0);

        statusbar.setText(getStatusText());
    }

    private String getStatusText() {
        return PLAYER_PREFIX + (getCurrentPlayer() + 1) + "'s turn | Mines left: " + getMinesLeft() +
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
                    drawIndex = calculateDrawIndexGameOver(cell, marker);
                } else {
                    drawIndex = calculateDrawIndexInGame(cell, marker);
                    if (drawIndex == DRAW_COVER) {
                        numCovers++;
                    }
                }
                g.drawImage(img[drawIndex], j * CELL_SIZE, i * CELL_SIZE, this);
            }
        }
        // Win condition
        if (numCovers == 0 && inGame) {
            BoardState state = analyzeBoardState();
            if (state.uncoveredSafeCells == state.totalSafeCells &&
                state.correctlyFlaggedMines == mines &&
                !state.hasWrongFlags) {
                inGame = false;
                gameWon = true;
            }
        }
        // Update status
        if (!inGame) {
            if (gameWon) {
                updateWinStatus();
            } else {
                updateLossStatus();
            }
            checkGameEnd();
        } else {
            statusbar.setText(getStatusText());
        }
    }

    private int calculateDrawIndexInGame(int cell, int marker) {
        if (cell > COVERED_MINE_CELL) {
            return (marker == 0) ? DRAW_MARK_P1 : DRAW_MARK_P2;
        } else if (cell > MINE_CELL) {
            return DRAW_COVER;
        } else {
            return cell;
        }
    }

    private int calculateDrawIndexGameOver(int cell, int marker) {
        if (cell == COVERED_MINE_CELL) {
            return DRAW_MINE;
        } else if (cell == MARKED_MINE_CELL) {
            return (marker == 0) ? DRAW_MARK_P1 : DRAW_MARK_P2;
        } else if (cell > COVERED_MINE_CELL) {
            return (marker == 0) ? DRAW_WRONG_MARK_P1 : DRAW_WRONG_MARK_P2;
        } else if (cell > MINE_CELL) {
            return DRAW_COVER;
        } else {
            return cell;
        }
    }

    private BoardState analyzeBoardState() {
        BoardState state = new BoardState();
        state.totalSafeCells = allCells - mines;

        for (int i = 0; i < allCells; i++) {
            int cell = field[i];
            int marker = markers[i];

            if (cell >= 0 && cell <= 8) {
                state.uncoveredSafeCells++;
            }
            else if (cell == MARKED_MINE_CELL && marker != -1) {
                state.correctlyFlaggedMines++;
            }
            else if (cell >= 20 && cell <= 28 && marker != -1) {  // flagged safe cell
                state.hasWrongFlags = true;
            }
        }
        return state;
    }

    private void updateWinStatus() {
        int p1 = playerFlags[0], p2 = playerFlags[1];
        String result = p1 > p2 ? " Player 1 wins!" :
                       p2 > p1 ? " Player 2 wins!" : " It's a draw!";
        statusbar.setText("Game won! P1: " + p1 + " | P2: " + p2 + result);
    }

    private void updateLossStatus() {
        int loser = getCurrentPlayer();
        int winner = 1 - loser;
        statusbar.setText(PLAYER_PREFIX + (loser + 1) + " hit a mine! " + 
                         PLAYER_PREFIX + (winner + 1) + " wins!");
    }

    void checkGameEnd() {
        if (!inGame && !gameEndDetected) {
            gameEndDetected = true;
            String message;
            if (gameWon) {
                int p1 = playerFlags[0], p2 = playerFlags[1];
                String p1Flags = String.format(FLAG_LINE_TEMPLATE, PLAYER_PREFIX + 1, p1);
                String p2Flags = String.format(FLAG_LINE_TEMPLATE, PLAYER_PREFIX + 2, p2);
                String result = p1 > p2 ? PLAYER_PREFIX + 1 + " wins!" : 
                                p2 > p1 ? PLAYER_PREFIX + 2 + " wins!" : "It's a draw!";
                message = String.format(WIN_DRAW_MESSAGE_TEMPLATE, "Game won!", p1Flags, p2Flags) + "\n" + result;
            } else {
                int loser = currentPlayer;
                int winner = 1 - loser;
                message = PLAYER_PREFIX + (loser + 1) + " hit a mine! " + PLAYER_PREFIX + (winner + 1) + " wins!";
            }
            JOptionPane.showMessageDialog(this, message);
        }
    }

    // Helper class to hold board state analysis results
    private static class BoardState {
        int uncoveredSafeCells = 0;
        int correctlyFlaggedMines = 0;
        boolean hasWrongFlags = false;
        int totalSafeCells = 0;
    }

    /* --------------------------------------------------------------------- */
    public static int getCoverForCell()   { return COVER_FOR_CELL; }
    public static int getCoveredMineCell(){ return COVERED_MINE_CELL; }

    /* --------------------------------------------------------------------- */
    class MinesAdapter extends MouseAdapter {
        @Override
        public void mousePressed(MouseEvent e) {
            if (!isInGame()) {
                newGame();
                return;
            }

            int x = e.getX();
            int y = e.getY();
            int cCol = x / CELL_SIZE;
            int cRow = y / CELL_SIZE;

            if (cRow < 0 || cCol < 0 || cRow >= rows || cCol >= cols) return;

            int pos = cRow * cols + cCol;
            boolean repaintNeeded = false;
            boolean validMove = false;

            // Right-click: Flag / Unflag
            if (e.getButton() == MouseEvent.BUTTON3) {
                // Can't flag revealed cells or cells that are already flagged by other player
                if (field[pos] <= MINE_CELL) return;

                boolean isMarked = (field[pos] > COVERED_MINE_CELL);
                if (isMarked) {
                    // Only allow current player to remove their own flags
                    if (markers[pos] == getCurrentPlayer()) {
                        field[pos] -= getMarkForCell();
                        markers[pos] = -1;
                        playerFlags[getCurrentPlayer()]--;
                        setMinesLeft(getMinesLeft() + 1);
                        validMove = true;
                        repaintNeeded = true;
                    }
                } else {
                    // Can only flag if there are flags left and cell is not already revealed
                    if (getMinesLeft() > 0 && field[pos] >= COVER_FOR_CELL) {
                        field[pos] += getMarkForCell();
                        markers[pos] = getCurrentPlayer();
                        playerFlags[getCurrentPlayer()]++;
                        setMinesLeft(getMinesLeft() - 1);
                        validMove = true;
                        repaintNeeded = true;
                    }
                }
            }
            // Left-click: Reveal
            else if (e.getButton() == MouseEvent.BUTTON1) {
                // Can't reveal flagged cells or already revealed cells
                if (field[pos] > COVERED_MINE_CELL || field[pos] <= MINE_CELL) return;

                // Reveal the cell
                int originalValue = field[pos];
                field[pos] -= COVER_FOR_CELL;
                
                // Safety check: ensure value doesn't go negative
                if (field[pos] < 0) {
                    System.err.println("Cell value went negative! Original: " + originalValue + ", New: " + field[pos]);
                    field[pos] = Math.max(0, field[pos]);
                }
                
                repaintNeeded = true;
                validMove = true;

                // Check if player hit a mine
                if (field[pos] == MINE_CELL) {
                    setInGame(false);
                    // Don't switch players - the current player lost
                    validMove = false; // Prevent player switch
                } else if (field[pos] == EMPTY_CELL) {
                    findEmptyCells(pos);
                }
            }

            // Only switch player on valid move that doesn't end the game
            if (validMove && isInGame()) {
                setCurrentPlayer(1 - getCurrentPlayer());
            }

            if (repaintNeeded) {
                repaint();
            }
        }
    }

    // =====================================================================
    // Getters for testing
    // =====================================================================
    public int getCurrentPlayer() { return currentPlayer; }
    public boolean isInGame() { return inGame; }
    public int getMinesLeft() { return minesLeft; }
    public int[] getPlayerFlags() { return playerFlags.clone(); }
    public int[] getField() { return field.clone(); }
    public int[] getMarkers() { return markers.clone(); }
    public int getRows() { return rows; }
    public int getCols() { return cols; }
    public int getAllCells() { return allCells; }

    // For testing only
    public void setFieldForTesting(int[] testField) {
        this.field = testField.clone();
    }

    public static int getDrawCover() {
        return DRAW_COVER;
    }

    public static int getDrawMarkP1() {
        return DRAW_MARK_P1;
    }

    public static int getDrawMine() {
        return DRAW_MINE;
    }

    public void setInGame(boolean inGame) {
        this.inGame = inGame;
    }

    public static int getDrawWrongMarkP2() {
        return DRAW_WRONG_MARK_P2;
    }

    public boolean isGameWon() {
        return gameWon;
    }

    public void setGameWon(boolean gameWon) {
        this.gameWon = gameWon;
    }

    public void setCurrentPlayer(int currentPlayer) {
        this.currentPlayer = currentPlayer;
    }

    public void setMinesLeft(int minesLeft) {
        this.minesLeft = minesLeft;
    }

	public static int getMarkForCell() {
		return MARK_FOR_CELL;
	}
}