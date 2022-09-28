package tablut;

import org.junit.Test;

import static org.junit.Assert.*;
import ucb.junit.textui;

/** The suite of all JUnit tests for the enigma package.
 *  @author aarini
 */
public class UnitTest {

    /** Run the JUnit tests in this package. Add xxxTest.class entries to
     *  the arguments of runClasses to run other JUnit tests. */
    public static void main(String[] ignored) {
        textui.runClasses(UnitTest.class);
    }

    /** Copy test for board.. */
    @Test public void copyTest() {
        Board b1 = new Board();
        Board b2 = new Board(b1);
        assertEquals(b1.encodedBoard(), b2.encodedBoard());
    }
}


