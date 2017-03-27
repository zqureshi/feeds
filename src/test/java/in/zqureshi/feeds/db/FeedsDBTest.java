package in.zqureshi.feeds.db;

import com.google.common.collect.ImmutableMap;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import org.rocksdb.RocksDBException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.*;

public class FeedsDBTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(FeedsDBTest.class);

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
        // Should start with minimum counter
        assertThat(db.incrementCounter("/users")).isEqualTo(10000);
        // Then increase monotonically
        assertThat(db.incrementCounter("/users")).isEqualTo(10001);
        // Should be independent for a different key
        assertThat(db.incrementCounter("/feeds")).isEqualTo(10000);
        assertThat(db.incrementCounter("/feeds")).isEqualTo(10001);
        // And increment properly for previous key
        assertThat(db.incrementCounter("/users")).isEqualTo(10002);

        for (int i = 0; i < 10000; i++) {
            assertThat(db.incrementCounter("/loop")).isEqualTo(10000 + i);
        }
    }

    // Shitty method of making sure it's atomic, in a better test suite would
    // try to trigger race conditions by overloading with threads and force
    // context switching.
    @Test
    public void testIncrementCounterIsSynchronized() {
        for (Method method : FeedsDB.class.getMethods()) {
            if (method.getName() == "incrementCounter") {
                assertThat(Modifier.isSynchronized(method.getModifiers())).isTrue();
            }
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

        assertThat(db.counters())
            .isEqualTo(ImmutableMap.of(
                "/first", 10002L,
                "/second", 10001L,
                "/popular", 10100L)
            );
    }

    @Test
    public void simpleGetAndPut() {
        // Should return null for nonexistant key.
        assertThat(db.get("/foobar")).isNull();

        // Put and get
        db.put("/memory", "#DEADBEEF".getBytes());
        assertThat(db.get("/memory")).isEqualTo("#DEADBEEF".getBytes());

        db.put("/memory", "".getBytes());
        assertThat(db.get("/memory")).isEmpty();
    }

    @Test(expected = NoSuchElementException.class)
    public void testIterator() {
        db.put("/users/1", "/1".getBytes());
        db.put("/users/3", "/3".getBytes());;
        db.put("/users/2", "/2".getBytes());
        db.put("/counters/1", Longs.toByteArray(10000L));

        FeedsDB.PrefixIterator it = db.scan("/users");

        // Keys should be read in sorted order
        assertThat(it.hasNext()).isTrue();
        assertThat(it.next()).isEqualTo("/1".getBytes());
        assertThat(it.next()).isEqualTo("/2".getBytes());
        assertThat(it.next()).isEqualTo("/3".getBytes());
        assertThat(it.hasNext()).isFalse();

        // Now trigger the exception
        it.next();
    }

    @Test(expected = NoSuchElementException.class)
    public void testIteratorWithStartIndex() {
        for (int i = 1000; i < 1100; i++) {
            db.put("/users/" + i, Ints.toByteArray(i));
        }

        FeedsDB.PrefixIterator it = db.scan("/users", "/1050");

        for (int i = 1050; i < 1100; i++) {
            assertThat(Ints.fromByteArray(it.next())).isEqualTo(i);
        }

        assertThat(it.hasNext()).isFalse();
        it.next();
    }
}