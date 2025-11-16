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


class Board extends JPanel {

    private static final long serialVersionUID = 6195235521361212179L;
    private static final int NUM_IMAGES = 15;
    private static final int CELL_SIZE = 15;

    private static final int COVER_FOR_CELL = 10;
    private static final int MARK_FOR_CELL   = 10;
    private static final int EMPTY_CELL      = 0;
    private static final int MINE_CELL       = 9;
    private static final int COVERED_MINE_CELL = MINE_CELL + COVER_FOR_CELL; // 19
    private static final int MARKED_MINE_CELL  = COVERED_MINE_CELL + MARK_FOR_CELL; // 29

    private static final int DRAW_MINE           = 9;
    private static final int DRAW_COVER          = 10;
    private static final int DRAW_MARK_P1        = 11;
    private static final int DRAW_WRONG_MARK_P1  = 12;
    private static final int DRAW_MARK_P2        = 13;
    private static final int DRAW_WRONG_MARK_P2  = 14;

    private static final String PLAYER_PREFIX = "Player ";
    private static final String FLAG_LINE_TEMPLATE = "%s flags: %d";
    private static final String WIN_DRAW_MESSAGE_TEMPLATE = "%s\n%s\n%s";
    private static final String HIT_A_MINE = " hit a mine! ";
    private static final String WINS = " wins!";
    private static final String GAME_WON = "Game won!";
    private static final String GAME_OVER = "Game Over";

    private int[] field;
    private int[] markers;
    private boolean inGame;
    private int minesLeft;
    private transient Image[] img;

    private int mines = 40;
    private int rows  = 16;
    private int cols  = 16;
    private int allCells;

    private final JLabel statusbar;
    private final SecureRandom random = new SecureRandom();

    private int currentPlayer = 0;
    private int[] playerFlags = new int[2];
    private boolean gameWon = false;
    private boolean gameEndDetected = false;

    public Board(JLabel statusbar) {
        this.statusbar = statusbar;

        img = new Image[NUM_IMAGES];
        for (int i = 0; i < NUM_IMAGES; i++) {
            img[i] = new ImageIcon(getClass().getClassLoader().getResource(i + ".gif")).getImage();
        }

        setDoubleBuffered(true);
        addMouseListener(new MinesAdapter());
        newGame();
    }

    void newGame() {
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
        return PLAYER_PREFIX + (currentPlayer + 1) + "'s turn | Mines left: " + minesLeft +
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
                    int np = nr * cols + nc;
                    if (field[np] != COVERED_MINE_CELL) {
                        field[np]++;
                    }
                }
            }
        }
    }

    public boolean isValidCell(int row, int col) {
        return row >= 0 && row < rows && col >= 0 && col < cols;
    }

    public void findEmptyCells(int pos) {
        int row = pos / cols;
        int col = pos % cols;

        for (int dr = -1; dr <= 1; dr++) {
            for (int dc = -1; dc <= 1; dc++) {
                // Only ONE continue allowed → this is the only one
                if (dr == 0 && dc == 0) continue;

                int nr = row + dr;
                int nc = col + dc;

                // Bounds check + validity in one guard
                if (!isValidCell(nr, nc)) continue;

                int np = nr * cols + nc;
                int value = field[np];

                // Skip if already revealed or flagged
                if (value < COVER_FOR_CELL || value >= MARKED_MINE_CELL) {
                    continue;
                }

                // Reveal the cell
                field[np] -= COVER_FOR_CELL;

                // If it's empty (now 0), recurse into neighbors
                if (field[np] == 0) {
                    findEmptyCells(np);
                }
                // Numbered cells (1–8) are revealed but not recursed into → correct behavior
            }
        }
    }
    @Override
    public void paint(Graphics g) {
        int numCovers = 0;
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                int idx = i * cols + j;
                int cell = field[idx];
                int marker = markers[idx];

                if (inGame && cell == MINE_CELL) {
                    inGame = false;
                    updateLossStatus();  // Trigger loss message
                }

                int drawIndex = !inGame ? calculateDrawIndexGameOver(cell, marker)
                                        : calculateDrawIndexInGame(cell, marker);
                if (inGame && drawIndex == DRAW_COVER) numCovers++;

                g.drawImage(img[drawIndex], j * CELL_SIZE, i * CELL_SIZE, this);
            }
        }

        if (numCovers == 0 && inGame) {
            BoardState state = analyzeBoardState();
            if (state.uncoveredSafeCells == state.totalSafeCells &&
                state.correctlyFlaggedMines == mines && !state.hasWrongFlags) {
                inGame = false;
                gameWon = true;
            }
        }

        if (!inGame) {
            if (gameWon) updateWinStatus();
            checkGameEnd();
        } else {
            statusbar.setText(getStatusText());
        }
    }

    private int calculateDrawIndexInGame(int cell, int marker) {
        if (cell >= 20) {
            return marker == 0 ? DRAW_MARK_P1 : DRAW_MARK_P2;
        }
        if (cell > MINE_CELL) {
            return DRAW_COVER;
        }
        return cell;
    }

    private int calculateDrawIndexGameOver(int cell, int marker) {
        if (cell == COVERED_MINE_CELL) return DRAW_MINE;
        if (cell == MARKED_MINE_CELL) return marker == 0 ? DRAW_MARK_P1 : DRAW_MARK_P2;
        if (cell >= 20 && cell <= 28) return marker == 0 ? DRAW_WRONG_MARK_P1 : DRAW_WRONG_MARK_P2;
        if (cell > MINE_CELL) return DRAW_COVER;
        return cell;
    }

    private BoardState analyzeBoardState() {
        BoardState s = new BoardState();
        s.totalSafeCells = allCells - mines;
        for (int i = 0; i < allCells; i++) {
            int cell = field[i];
            int marker = markers[i];
            if (cell >= 0 && cell <= 8) s.uncoveredSafeCells++;
            else if (cell == MARKED_MINE_CELL) s.correctlyFlaggedMines++;
            else if (cell >= 20 && cell <= 28 && marker != -1) s.hasWrongFlags = true;
        }
        return s;
    }

    private void updateWinStatus() {
        int p1 = playerFlags[0];
        int p2 = playerFlags[1];
        
        String result;
        if (p1 > p2) {
            result = PLAYER_PREFIX + 1 + WINS;
        } else if (p2 > p1) {
            result = PLAYER_PREFIX + 2 + WINS;
        } else {
            result = "It's a draw!";
        }
        
        statusbar.setText(GAME_WON + " P1: " + p1 + " | P2: " + p2 + result);
    }

    private void updateLossStatus() {
        int loser = currentPlayer;
        int winner = 1 - loser;
        statusbar.setText(PLAYER_PREFIX + (loser + 1) + HIT_A_MINE +
                         PLAYER_PREFIX + (winner + 1) + WINS);
    }

    void checkGameEnd() {
        if (!inGame && !gameEndDetected) {
            gameEndDetected = true;
            String message;

            if (gameWon) {
                String p1f = String.format(FLAG_LINE_TEMPLATE, PLAYER_PREFIX + 1, playerFlags[0]);
                String p2f = String.format(FLAG_LINE_TEMPLATE, PLAYER_PREFIX + 2, playerFlags[1]);

                String result;
                if (playerFlags[0] > playerFlags[1]) {
                    result = PLAYER_PREFIX + 1 + WINS;
                } else if (playerFlags[1] > playerFlags[0]) {
                    result = PLAYER_PREFIX + 2 + WINS;
                } else {
                    result = "It's a draw!";
                }
                message = String.format(WIN_DRAW_MESSAGE_TEMPLATE, GAME_WON, p1f, p2f) + "\n" + result;
            } else {
                int loser = currentPlayer;
                int winner = 1 - loser;
                message = PLAYER_PREFIX + (loser + 1) + HIT_A_MINE + "\n" +
                          PLAYER_PREFIX + (winner + 1) + WINS;
            }

            JOptionPane.showMessageDialog(this, message, GAME_OVER, JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private static class BoardState {
        int uncoveredSafeCells = 0;
        int correctlyFlaggedMines = 0;
        boolean hasWrongFlags = false;
        int totalSafeCells = 0;
    }

    // GETTERS & SETTERS
    public static int getCoverForCell() { return COVER_FOR_CELL; }
    public static int getCoveredMineCell() { return COVERED_MINE_CELL; }
    public static int getMarkForCell() { return MARK_FOR_CELL; }
    public static int getDrawCover() { return DRAW_COVER; }
    public static int getDrawMarkP1() { return DRAW_MARK_P1; }
    public static int getDrawMine() { return DRAW_MINE; }
    public static int getDrawWrongMarkP2() { return DRAW_WRONG_MARK_P2; }

    public int getCurrentPlayer() { return currentPlayer; }
    public boolean isInGame() { return inGame; }
    public int getMinesLeft() { return minesLeft; }
    public int[] getPlayerFlags() { return playerFlags.clone(); }
    public int[] getField() { return field.clone(); }
    public int[] getMarkers() { return markers.clone(); }
    public int getRows() { return rows; }
    public int getCols() { return cols; }
    public int getAllCells() { return allCells; }
    public int getTotalMines() { return mines; }

    public void setFieldForTesting(int[] testField) { this.field = testField.clone(); }
    public void setInGame(boolean inGame) { this.inGame = inGame; }
    public boolean isGameWon() { return gameWon; }
    public void setGameWon(boolean gameWon) { this.gameWon = gameWon; }
    public void setCurrentPlayer(int p) { this.currentPlayer = p; }
    public void setMinesLeft(int minesLeft) { this.minesLeft = minesLeft; }

    public void simulateMousePress(MouseEvent e) {
        for (java.awt.event.MouseListener l : getMouseListeners()) {
            l.mousePressed(e);
        }
    }

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

            if (e.getButton() == MouseEvent.BUTTON3) {
                if (field[pos] <= MINE_CELL) return;

                boolean isFlagged = (field[pos] >= 20);

                if (isFlagged) {
                    if (markers[pos] == currentPlayer) {
                        field[pos] -= MARK_FOR_CELL;
                        markers[pos] = -1;
                        playerFlags[currentPlayer]--;
                        setMinesLeft(getMinesLeft() + 1);
                        repaintNeeded = true;
                    }
                } else {
                    if (field[pos] < 20) {
                        field[pos] += MARK_FOR_CELL;
                        markers[pos] = currentPlayer;
                        playerFlags[currentPlayer]++;
                        setMinesLeft(getMinesLeft() - 1);
                        repaintNeeded = true;
                    }
                }
            }
            else if (e.getButton() == MouseEvent.BUTTON1) {
                if (field[pos] < COVER_FOR_CELL || field[pos] >= 20) return;

                field[pos] -= COVER_FOR_CELL;
                repaintNeeded = true;
                validMove = true;

                if (field[pos] == MINE_CELL) {
                    setInGame(false);
                    validMove = false;
                } else if (field[pos] == EMPTY_CELL) {
                    findEmptyCells(pos);
                }

                if (validMove && isInGame()) {
                    setCurrentPlayer(1 - currentPlayer);
                }
            }

            if (repaintNeeded) repaint();
        }
    }
}