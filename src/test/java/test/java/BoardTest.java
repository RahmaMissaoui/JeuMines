package test.java;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import main.java.mines.Board;

import javax.swing.JLabel;
import java.lang.reflect.Field;
import java.util.Random;
import static org.junit.jupiter.api.Assertions.*;


class BoardTest {

    private Board board;
    private JLabel dummyLabel;

    @BeforeEach
    void setUp() throws Exception {
        dummyLabel = new JLabel();
        board = new Board(dummyLabel);

        setField("rows", 3);
        setField("cols", 3);
        setField("mines", 2);
        setField("allCells", 9);
        setField("inGame", true);

        Random r = new Random(12345L);
        setField("random", r);
    }

    @Test
    void newGamePlacesExactlyTwoMines() throws Exception {
        board.newGame();
        int[] field = (int[]) getField("field");
        long mines = java.util.Arrays.stream(field)
                .filter(v -> v == Board.getCoveredMineCell())
                .count();
        assertEquals(2, mines);
    }

    @Test
    void findEmptyCellsRevealsArea() throws Exception {
        setField("rows", 3);
        setField("cols", 3);
        setField("mines", 1);
        setField("inGame", true);
        board.newGame(); 

        int[] field = (int[]) getField("field");
        int emptyCell = -1;
        for (int i = 0; i < 9; i++) {
            if (field[i] == Board.getCoverForCell()) { 
                emptyCell = i;
                break;
            }
        }

        board.findEmptyCells(emptyCell);

        field = (int[]) getField("field");
        
        assertTrue(java.util.Arrays.stream(field).anyMatch(v -> v >= 0 && v <= 8));
    }
    // --- helpers ---
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