package in.zqureshi.feeds.resources;

import com.google.common.collect.ImmutableMap;
import in.zqureshi.feeds.db.FeedsDB;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import org.rocksdb.RocksDBException;

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
        assertEquals(0, db.incrementCounter("/users"));
        // Then increase monotonically
        assertEquals(1, db.incrementCounter("/users"));
        // Should be independent for a different key
        assertEquals(0, db.incrementCounter("/feeds"));
        assertEquals(1, db.incrementCounter("/feeds"));
        // And increment properly for previous key
        assertEquals(2, db.incrementCounter("/users"));

        for (int i = 0; i < 10000; i++) {
            assertEquals(i, db.incrementCounter("/loop"));
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
            ImmutableMap.of("/first", 2L, "/second", 1L, "/popular", 100L),
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
}