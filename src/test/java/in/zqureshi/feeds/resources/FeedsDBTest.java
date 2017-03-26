package in.zqureshi.feeds.resources;

import com.google.common.collect.ImmutableMap;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import org.rocksdb.RocksDBException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;

import static org.junit.Assert.*;

public class FeedsDBTest {
    private FeedsDB db;

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Before
    public void before() throws RocksDBException {
        this.db = new FeedsDB(folder.getRoot().getPath());
    }

    @After
    public void after() throws Exception {
        this.db.stop();
    }

    @Test
    public void incrementCounter() {
        // Should start with zero
        assertEquals(0, this.db.incrementCounter("/users"));
        // Then increase monotonically
        assertEquals(1, this.db.incrementCounter("/users"));
        // Should be independent for a different key
        assertEquals(0, this.db.incrementCounter("/feeds"));
        assertEquals(1, this.db.incrementCounter("/feeds"));
        // And increment properly for previous key
        assertEquals(2, this.db.incrementCounter("/users"));

        for (int i = 0; i < 100000; i++) {
            assertEquals(i, this.db.incrementCounter("/loop"));
        }
    }

    @Test
    public void counters() {
        this.db.incrementCounter("/first");
        this.db.incrementCounter("/first");

        this.db.incrementCounter("/second");
        this.db.incrementCounter("/third");

        assertEquals(
            ImmutableMap.of("/first", 2L, "/second", 1L, "/third", 1L),
            this.db.counters());
    }
}