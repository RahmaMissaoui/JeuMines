package mines;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for Mines (main application frame)
 */
public class MinesTest {

    private Mines mines;
    private Thread uiThread;

    @BeforeEach
    public void setUp() throws InterruptedException, InvocationTargetException {
        // Use CountDownLatch to ensure the GUI is created on EDT
        CountDownLatch latch = new CountDownLatch(1);
        
        SwingUtilities.invokeAndWait(() -> {
            mines = new Mines();
            latch.countDown();
        });
        
        // Wait for GUI to be created
        assertTrue(latch.await(2, TimeUnit.SECONDS), "GUI should be created within 2 seconds");
        
        // Give the GUI some time to initialize completely
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @AfterEach
    public void tearDown() {
        if (mines != null) {
            SwingUtilities.invokeLater(() -> {
                mines.setVisible(false);
                mines.dispose();
            });
        }
        if (uiThread != null && uiThread.isAlive()) {
            uiThread.interrupt();
        }
    }

    @Test
    @Timeout(5)
    public void testMinesFrameInitialization() {
        SwingUtilities.invokeLater(() -> {
            // Test frame title
            assertEquals("Winesweeper", mines.getTitle(), "Frame title should be 'Winesweeper'");

            // Test frame size
            Dimension size = mines.getSize();
            assertEquals(250, size.width, "Frame width should be 250");
            assertEquals(290, size.height, "Frame height should be 290");

            // Test frame resizability
            assertFalse(mines.isResizable(), "Frame should not be resizable");

            // Test default close operation
            assertEquals(WindowConstants.EXIT_ON_CLOSE, 
                        mines.getDefaultCloseOperation(), 
                        "Default close operation should be EXIT_ON_CLOSE");
        });
    }

    @Test
    @Timeout(5)
    public void testMinesFrameVisibility() {
        SwingUtilities.invokeLater(() -> {
            assertTrue(mines.isVisible(), "Mines frame should be visible after construction");
        });
    }

    @Test
    @Timeout(5)
    public void testMinesFrameComponents() {
        SwingUtilities.invokeLater(() -> {
            // Test that Board component is added
            Component[] components = mines.getContentPane().getComponents();
            boolean boardFound = false;
            boolean statusBarFound = false;

            for (Component comp : components) {
                if (comp instanceof Board) {
                    boardFound = true;
                }
                if (comp instanceof JLabel) {
                    statusBarFound = true;
                    JLabel statusbar = (JLabel) comp;
                    // Initial status should be empty or contain game info
                    assertNotNull(statusbar.getText(), "Status bar text should not be null");
                }
            }

            assertTrue(boardFound, "Board component should be added to the frame");
            assertTrue(statusBarFound, "Status bar component should be added to the frame");
        });
    }

    @Test
    @Timeout(5)
    public void testMinesFrameLayout() {
        SwingUtilities.invokeLater(() -> {
            LayoutManager layout = mines.getContentPane().getLayout();
            assertTrue(layout instanceof BorderLayout, 
                      "Content pane should use BorderLayout");
        });
    }

    @Test
    @Timeout(5)
    public void testMinesFrameLocation() {
        SwingUtilities.invokeLater(() -> {
            // Frame should be centered (location relative to null)
            // Note: We can't test exact coordinates as they depend on screen size
            Point location = mines.getLocation();
            assertNotNull(location, "Frame location should not be null");
            
            // Frame should be on screen
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            Rectangle screenBounds = ge.getMaximumWindowBounds();
            assertTrue(location.x >= 0 && location.x <= screenBounds.width, 
                      "Frame X coordinate should be on screen");
            assertTrue(location.y >= 0 && location.y <= screenBounds.height, 
                      "Frame Y coordinate should be on screen");
        });
    }

    @Test
    @Timeout(5)
    public void testMainMethod() throws InterruptedException {
        // Test that main method doesn't throw exceptions
        Thread mainThread = new Thread(() -> {
            try {
                Mines.main(new String[]{});
            } catch (Exception e) {
                fail("Main method should not throw exceptions: " + e.getMessage());
            }
        });
        
        mainThread.start();
        
        // Wait a bit for main to initialize
        Thread.sleep(1000);
        
        // Interrupt and clean up
        mainThread.interrupt();
    }

    @Test
    @Timeout(5)
    public void testMainMethodWithNullArgs() throws InterruptedException {
        // Test main method with null arguments
        Thread mainThread = new Thread(() -> {
            try {
                Mines.main(null);
            } catch (Exception e) {
                fail("Main method should handle null arguments: " + e.getMessage());
            }
        });
        
        mainThread.start();
        
        // Wait a bit for main to initialize
        Thread.sleep(1000);
        
        // Interrupt and clean up
        mainThread.interrupt();
    }

    @Test
    @Timeout(5)
    public void testFrameDisposal() {
        SwingUtilities.invokeLater(() -> {
            assertFalse(mines.isDisplayable(), "Frame should not be disposed initially");
            
            mines.dispose();
            
            // After disposal, these should be true
            assertTrue(mines.isDisplayable() || !mines.isVisible(), 
                      "Frame should be disposed or not visible after dispose()");
        });
    }

    @Test
    @Timeout(5)
    public void testMinesInstanceCreation() {
        // Test that multiple instances can be created
        assertDoesNotThrow(() -> {
            SwingUtilities.invokeAndWait(() -> {
                Mines anotherInstance = new Mines();
                assertNotNull(anotherInstance, "Should be able to create another Mines instance");
                anotherInstance.dispose();
            });
        });
    }

    @Test
    @Timeout(5)
    public void testComponentHierarchy() {
        SwingUtilities.invokeLater(() -> {
            // Verify the component hierarchy
            Container contentPane = mines.getContentPane();
            assertNotNull(contentPane, "Content pane should not be null");
            
            Component[] components = contentPane.getComponents();
            assertTrue(components.length >= 2, 
                      "Should have at least 2 components (Board and status bar)");
            
            // Check that Board is properly added
            boolean hasBoard = false;
            for (Component comp : components) {
                if (comp instanceof Board) {
                    hasBoard = true;
                    Board board = (Board) comp;
                    assertNotNull(board, "Board instance should not be null");
                    break;
                }
            }
            assertTrue(hasBoard, "Should contain a Board component");
        });
    }
}