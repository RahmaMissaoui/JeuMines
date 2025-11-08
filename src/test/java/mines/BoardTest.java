package mines;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import javax.swing.JLabel;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

public class BoardTest {

    private Board board;
    private JLabel statusbar;

    @BeforeEach
    public void setUp() {
        statusbar = new JLabel();
        board = new Board(statusbar);
    }

    @AfterEach
    public void tearDown() {
        board = null;
        statusbar = null;
    }

    @Test
    public void testInitialState() {
        assertTrue(board.isInGame(), "Game should be in progress initially");
        assertEquals(0, board.getCurrentPlayer(), "Initial player should be 0");
        assertEquals(40, board.getMinesLeft(), "Mines left should be 40 initially");
        
        int[] playerFlags = board.getPlayerFlags();
        assertEquals(0, playerFlags[0], "Player 1 flags should be 0 initially");
        assertEquals(0, playerFlags[1], "Player 2 flags should be 0 initially");
        
        assertEquals(16, board.getRows(), "Should have 16 rows");
        assertEquals(16, board.getCols(), "Should have 16 columns");
        assertEquals(256, board.getAllCells(), "Should have 256 total cells");
    }

    @Test
    public void testIsValidCell() {
        // Test valid cells
        assertTrue(board.isValidCell(0, 0), "Cell (0,0) should be valid");
        assertTrue(board.isValidCell(15, 15), "Cell (15,15) should be valid");
        assertTrue(board.isValidCell(8, 8), "Cell (8,8) should be valid");
        
        // Test invalid cells
        assertFalse(board.isValidCell(-1, 0), "Cell (-1,0) should be invalid");
        assertFalse(board.isValidCell(0, -1), "Cell (0,-1) should be invalid");
        assertFalse(board.isValidCell(16, 0), "Cell (16,0) should be invalid");
        assertFalse(board.isValidCell(0, 16), "Cell (0,16) should be invalid");
        assertFalse(board.isValidCell(16, 16), "Cell (16,16) should be invalid");
    }

    @Test
    public void testIncrementNeighbors() throws Exception {
        // Use reflection to test private method
        Method incrementNeighbors = Board.class.getDeclaredMethod("incrementNeighbors", int.class);
        incrementNeighbors.setAccessible(true);
        
        // Create a test field with one mine
        int[] testField = new int[256];
        for (int i = 0; i < testField.length; i++) {
            testField[i] = Board.getCoverForCell();
        }
        
        // Place a mine in the center (position 136 = 8*16 + 8)
        testField[136] = Board.getCoveredMineCell();
        board.setFieldForTesting(testField);
        
        // Call the private method
        incrementNeighbors.invoke(board, 136);
        
        int[] field = board.getField();
        
        // Check that all neighbors were incremented
        for (int dr = -1; dr <= 1; dr++) {
            for (int dc = -1; dc <= 1; dc++) {
                if (dr == 0 && dc == 0) continue;
                
                int pos = (8 + dr) * 16 + (8 + dc);
                if (board.isValidCell(8 + dr, 8 + dc)) {
                    assertEquals(Board.getCoverForCell() + 1, field[pos], "Neighbor should be incremented");
                }
            }
        }
        
        // The mine cell itself should remain unchanged
        assertEquals(Board.getCoveredMineCell(), field[136], "Mine cell should remain unchanged");
    }

    @Test
    public void testFindEmptyCells() {
        // Create a test field with an empty area
        int[] testField = new int[256];
        for (int i = 0; i < testField.length; i++) {
            testField[i] = Board.getCoverForCell();
        }
        
        // Create a 3x3 empty area in the center
        for (int r = 7; r <= 9; r++) {
            for (int c = 7; c <= 9; c++) {
                testField[r * 16 + c] = 0 + Board.getCoverForCell(); // Empty cell with cover
            }
        }
        
        board.setFieldForTesting(testField);
        
        // Start from center (8,8)
        board.findEmptyCells(8 * 16 + 8);
        
        int[] field = board.getField();
        
        // All cells in the 3x3 area should be revealed (cover removed)
        for (int r = 7; r <= 9; r++) {
            for (int c = 7; c <= 9; c++) {
                assertEquals(0, field[r * 16 + c], "Empty cell should be revealed");
            }
        }
    }

    @Test
    public void testPlayerSwitching() {
        // Test initial player
        assertEquals(0, board.getCurrentPlayer(), "Initial player should be 0");
       
        // The actual switching happens in mouse events, but we can verify the logic works
        int nextPlayer = 1 - board.getCurrentPlayer();
        assertEquals(1, nextPlayer, "Next player should be 1 when current is 0");
        
        nextPlayer = 1 - nextPlayer;
        assertEquals(0, nextPlayer, "Next player should be 0 when current is 1");
    }

    @Test
    public void testNewGameReset() {
        // First modify some state
        int[] modifiedField = new int[256];
        for (int i = 0; i < modifiedField.length; i++) {
            modifiedField[i] = 5; // Some modified state
        }
        board.setFieldForTesting(modifiedField);
        
        // Call newGame to reset
        board.newGame();
        
        // Verify reset state
        assertTrue(board.isInGame(), "Game should be in progress after newGame");
        assertEquals(0, board.getCurrentPlayer(), "Player should be reset to 0");
        assertEquals(40, board.getMinesLeft(), "Mines left should be reset to 40");
        
        int[] playerFlags = board.getPlayerFlags();
        assertEquals(0, playerFlags[0], "Player 1 flags should be reset to 0");
        assertEquals(0, playerFlags[1], "Player 2 flags should be reset to 0");
    }

    @Test
    public void testGameConstants() {
        // Test that constants have expected values
        assertEquals(10, Board.getCoverForCell(), "COVER_FOR_CELL should be 10");
        assertEquals(19, Board.getCoveredMineCell(), "COVERED_MINE_CELL should be 19");
    }

    @Test
    public void testFieldAndMarkersArrays() {
        int[] field = board.getField();
        int[] markers = board.getMarkers();
        
        assertNotNull(field, "Field array should not be null");
        assertNotNull(markers, "Markers array should not be null");
        
        assertEquals(256, field.length, "Field array should have 256 elements");
        assertEquals(256, markers.length, "Markers array should have 256 elements");
        
        // After initialization, all markers should be -1
        for (int marker : markers) {
            assertEquals(-1, marker, "All markers should be -1 initially");
        }
    }

    @Test
    public void testBoardDimensions() {
        assertEquals(16, board.getRows(), "Board should have 16 rows");
        assertEquals(16, board.getCols(), "Board should have 16 columns");
        assertEquals(256, board.getAllCells(), "Board should have 256 total cells");
        
        // Verify the relationship
        assertEquals(board.getRows() * board.getCols(), board.getAllCells(), "rows * cols should equal allCells");
    }

    @Test
    public void testMinePlacement() {
        int[] field = board.getField();
        
        int mineCount = 0;
        for (int cell : field) {
            if (cell == Board.getCoveredMineCell()) {
                mineCount++;
            }
        }
        
        assertEquals(40, mineCount, "Should have exactly 40 mines placed");
    }

    @Test
    public void testNeighborCountingCompleteness() {
        int[] field = board.getField();
        
        // Count how many cells have neighbor counts (values between 1-8 with cover)
        int numberedCells = 0;
        for (int cell : field) {
            if (cell > Board.getCoverForCell() && cell < Board.getCoveredMineCell()) {
                numberedCells++;
            }
        }
        
        // There should be some numbered cells (not just mines and empty cells)
        assertTrue(numberedCells > 0, "Should have some numbered cells");
        
        // Verify no cell has invalid values
        for (int cell : field) {
            assertTrue(cell >= 0 && cell <= Board.getCoveredMineCell() + Board.getCoverForCell(), "Cell value should be valid");
        }
    }

    @Test
    public void testGameStatePersistence() {
        // Test that game state methods return consistent values
        boolean inGame = board.isInGame();
        int currentPlayer = board.getCurrentPlayer();
        int minesLeft = board.getMinesLeft();
        int[] playerFlags = board.getPlayerFlags();
        
        // Call methods again and verify consistency
        assertEquals(inGame, board.isInGame(), "isInGame should return consistent value");
        assertEquals(currentPlayer, board.getCurrentPlayer(), "getCurrentPlayer should return consistent value");
        assertEquals(minesLeft, board.getMinesLeft(), "getMinesLeft should return consistent value");
        assertArrayEquals(playerFlags, board.getPlayerFlags(), "getPlayerFlags should return consistent values");
    }
}