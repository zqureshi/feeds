package in.zqureshi.feeds.api;

import org.junit.Test;

import static org.junit.Assert.*;

public class CounterTest {
    @Test
    public void equality() {
        assertEquals(new Counter("/foo", 1L), new Counter("/foo", 1L));
        assertNotEquals(new Counter("/bar", 1L), new Counter("/bar", 100L));
    }

    @Test
    public void ordering() {
        Counter c1 = new Counter("/foo", 1L);
        Counter c2 = new Counter("/foo", 2L);

        assertTrue(c1.compareTo(c2) < 0);
        assertTrue(c2.compareTo(c1) > 0);
        assertTrue(c1.compareTo(c1) == 0);
    }
}