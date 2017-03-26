package in.zqureshi.feeds.resources;

import com.google.common.collect.ImmutableMap;
import com.google.common.primitives.Longs;
import in.zqureshi.feeds.db.FeedsDB;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import org.rocksdb.RocksDBException;

import java.util.NoSuchElementException;

import static org.junit.Assert.*;

public class FeedsDBTest {
    private FeedsDB db;

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Before
    public void before() throws RocksDBException {
        db = new FeedsDB(folder.getRoot().getPath());
    }

    @After
    public void after() throws Exception {
        db.stop();
    }

    @Test
    public void incrementCounter() {
        // Should start with zero
        assertEquals(10000, db.incrementCounter("/users"));
        // Then increase monotonically
        assertEquals(10001, db.incrementCounter("/users"));
        // Should be independent for a different key
        assertEquals(10000, db.incrementCounter("/feeds"));
        assertEquals(10001, db.incrementCounter("/feeds"));
        // And increment properly for previous key
        assertEquals(10002, db.incrementCounter("/users"));

        for (int i = 0; i < 10000; i++) {
            assertEquals(10000 + i, db.incrementCounter("/loop"));
        }
    }

    @Test
    public void counters() {
        db.incrementCounter("/first");
        db.incrementCounter("/first");

        db.incrementCounter("/second");

        for (int i = 0; i < 100; i++) {
            db.incrementCounter("/popular");
        }

        assertEquals(
            ImmutableMap.of("/first", 10002L, "/second", 10001L, "/popular", 10100L),
            db.counters());
    }

    @Test
    public void simpleGetAndPut() {
        // Should return null for nonexistant key.
        assertEquals(null, db.get("/foobar"));

        // Put and get
        db.put("/memory", "#DEADBEEF".getBytes());
        assertArrayEquals("#DEADBEEF".getBytes(), db.get("/memory"));

        db.put("/memory", "".getBytes());
        assertArrayEquals("".getBytes(), db.get("/memory"));
    }

    @Test(expected = NoSuchElementException.class)
    public void testIterator() {
        db.put("/users/1", "/1".getBytes());
        db.put("/users/3", "/3".getBytes());;
        db.put("/users/2", "/2".getBytes());
        db.put("/counters/1", Longs.toByteArray(10000L));

        FeedsDB.PrefixIterator it = db.scan("/users");

        // Keys should be read in sorted order
        assertTrue(it.hasNext());
        assertArrayEquals("/1".getBytes(), it.next());
        assertArrayEquals("/2".getBytes(), it.next());
        assertArrayEquals("/3".getBytes(), it.next());
        assertFalse(it.hasNext());

        // Now trigger the exception
        it.next();
    }
}