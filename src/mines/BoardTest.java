package mines;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import mines.Board;

import javax.swing.JLabel;
import java.lang.reflect.Field;
import java.util.Random;
import static org.junit.jupiter.api.Assertions.*;

//  mvn clean verify sonar:sonar -Dsonar.projectKey=minesweeper -Dsonar.host.url=http://localhost:9000 -Dsonar.login=sqp_ca407b89ccd69a87f02b9810a9d598f1eb2b2e1f

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
        setField("inGame", true); // AJOUTÉ

        Random r = new Random(12345L);
        setField("random", r);
    }

    @Test
    void newGamePlacesExactlyTwoMines() throws Exception {
        board.newGame();
        int[] field = (int[]) getField("field");
        long mines = java.util.Arrays.stream(field)
                .filter(v -> v == Board.COVERED_MINE_CELL)
                .count();
        assertEquals(2, mines);
    }

    @Test
    void adjacentNumbersAreCorrect() throws Exception {
        board.newGame();
        int[] field = (int[]) getField("field");
        int center = field[4]; // centre (1,1)
        assertEquals(2, center - Board.COVER_FOR_CELL);
    }

    @Test
    void findEmptyCellsRevealsArea() throws Exception {
        setField("rows", 3);
        setField("cols", 3);
        setField("mines", 1);
        setField("inGame", true);
        board.newGame(); // plateau correctement initialisé

        int[] field = (int[]) getField("field");
        int emptyCell = -1;
        for (int i = 0; i < 9; i++) {
            if (field[i] == Board.COVER_FOR_CELL) { // case vide couverte
                emptyCell = i;
                break;
            }
        }

        board.findEmptyCells(emptyCell);

        field = (int[]) getField("field");
        // Vérifier qu'au moins une case adjacente est révélée
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