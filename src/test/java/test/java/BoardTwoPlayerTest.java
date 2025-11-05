package test.java;

import main.java.mines.Board;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.JLabel;
import java.awt.event.MouseEvent;
import java.lang.reflect.Field;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class BoardTwoPlayerTest {

    private Board board;
    private JLabel statusLabel;

    @BeforeEach
    void setUp() throws Exception {
        statusLabel = new JLabel();
        board = new Board(statusLabel);
        configureBoard(); // configures rows, cols, mines, random
    }

    // -------------------------------------------------------------------------
    // Helper: configure board (3x3, 1 mine, deterministic)
    private void configureBoard() throws Exception {
        setField("rows", 3);
        setField("cols", 3);
        setField("mines", 1);
        setField("allCells", 9);
        setField("inGame", true);
        Random r = new Random(12345L);
        setField("random", r);
    }

    // Helper: fully initialize the board (must be called in each test)
    private void initBoard() throws Exception {
        board.newGame();
        setField("minesLeft", 1);
    }

    // -------------------------------------------------------------------------
    @Test
    void testPlayerTurnSwitchesOnLeftClickSafeCell() throws Exception {
        initBoard();
        setField("currentPlayer", 0);
        int pos = firstCoveredPos();
        simulateLeftClick(pos);
        assertEquals(1, getField("currentPlayer"));
    }

    @Test
    void testPlayerTurnSwitchesOnRightClickFlag() throws Exception {
        initBoard();
        setField("currentPlayer", 0);
        int pos = firstCoveredPos();
        simulateRightClick(pos);
        assertEquals(1, getField("currentPlayer"));
    }

    @Test
    void testPlayerTurnDoesNotSwitchOnInvalidMove() throws Exception {
        initBoard();
        setField("currentPlayer", 0);
        int pos = firstCoveredPos();
        simulateLeftClick(pos);
        int before = (int) getField("currentPlayer");
        simulateRightClick(pos);
        assertEquals(before, getField("currentPlayer"));
    }

    @Test
    void testPlayer1PlacesFlagIncreasesP1CountAndDecreasesMinesLeft() throws Exception {
        initBoard();
        setField("currentPlayer", 0);
        int pos = firstCoveredPos();
        simulateRightClick(pos);
        int[] flags = (int[]) getField("playerFlags");
        assertEquals(1, flags[0]);
        assertEquals(0, flags[1]);
        assertEquals(0, getField("minesLeft"));
    }

    @Test
    void testPlayer2PlacesFlagIncreasesP2Count() throws Exception {
        initBoard();
        setField("currentPlayer", 1);
        int pos = firstCoveredPos();
        simulateRightClick(pos);
        int[] flags = (int[]) getField("playerFlags");
        assertEquals(0, flags[0]);
        assertEquals(1, flags[1]);
    }

    @Test
    void testUnflaggingReturnsFlagToPlayerAndIncreasesMinesLeft() throws Exception {
        initBoard();
        setField("currentPlayer", 0);
        int pos = firstCoveredPos();
        simulateRightClick(pos);
        simulateRightClick(pos);
        int[] flags = (int[]) getField("playerFlags");
        assertEquals(0, flags[0]);
        assertEquals(1, getField("minesLeft"));
    }

    @Test
    void testCannotRemoveOtherPlayersFlag() throws Exception {
        initBoard();
        setField("currentPlayer", 0);
        int pos = firstCoveredPos();
        simulateRightClick(pos);
        setField("currentPlayer", 1);
        simulateRightClick(pos);
        int[] markers = (int[]) getField("markers");
        int[] flags = (int[]) getField("playerFlags");
        assertEquals(0, markers[pos]);
        assertEquals(1, flags[0]);
        assertEquals(0, flags[1]);
    }

    @Test
    void testPlayerWhoHitsMineLosesAndOtherWins() throws Exception {
        initBoard();
        setField("currentPlayer", 0);
        int mine = minePos();
        simulateLeftClick(mine);
        assertFalse((boolean) getField("inGame"));
        assertFalse((boolean) getField("gameWon"));
        assertTrue(statusLabel.getText().contains("Player 1 hit a mine! Player 2 wins!"));
    }

    @Test
    void testWinByClearingAllSafeCellsAwardsHigherFlagPlayer() throws Exception {
        initBoard();
        setField("currentPlayer", 0);
        int flagPos = firstCoveredPos();
        simulateRightClick(flagPos);

        int[] field = (int[]) getField("field");
        for (int i = 0; i < field.length; i++) {
            if (field[i] == Board.getCoverForCell() && i != flagPos) {
                simulateLeftClick(i);
            }
        }

        String txt = statusLabel.getText();
        assertTrue(txt.contains("Player 1 wins!") || txt.contains("draw"),
                "Expected win or draw for Player 1");
    }

    @Test
    void testGameResetsOnClickAfterGameOver() throws Exception {
        initBoard();
        setField("currentPlayer", 0);
        simulateLeftClick(minePos());
        simulateLeftClick(0);
        assertTrue((boolean) getField("inGame"));
        assertEquals(0, getField("currentPlayer"));
        assertArrayEquals(new int[]{0, 0}, (int[]) getField("playerFlags"));
    }

    // -------------------------------------------------------------------------
    // Helper methods

    private int firstCoveredPos() throws Exception {
        int[] f = (int[]) getField("field");
        for (int i = 0; i < f.length; i++) {
            if (f[i] == Board.getCoverForCell()) return i;
        }
        throw new IllegalStateException("No covered cell found");
    }

    private int minePos() throws Exception {
        int[] f = (int[]) getField("field");
        for (int i = 0; i < f.length; i++) {
            if (f[i] == Board.getCoveredMineCell()) return i;
        }
        throw new IllegalStateException("No mine placed");
    }

    private void simulateLeftClick(int pos) throws Exception {
        invokeMousePressed(pos, MouseEvent.BUTTON1);
    }

    private void simulateRightClick(int pos) throws Exception {
        invokeMousePressed(pos, MouseEvent.BUTTON3);
    }

    private void invokeMousePressed(int pos, int button) throws Exception {
        int row = pos / 3;
        int col = pos % 3;
        int x = col * 15 + 7;
        int y = row * 15 + 7;

        MouseEvent ev = new MouseEvent(board, MouseEvent.MOUSE_PRESSED,
                System.currentTimeMillis(), 0, x, y, 0, false, button);

        board.getMouseListeners()[0].mousePressed(ev);
    }

    private void setField(String name, Object value) throws Exception {
        Field f = Board.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(board, value);
    }

    private Object getField(String name) throws Exception {
        Field f = Board.class.getDeclaredField(name);
        f.setAccessible(true);
        return f.get(board);
    }
}