package in.zqureshi.feeds.db;

import com.google.common.primitives.Longs;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.junit.Assert.*;

public class RocksDBTest {
    private RocksDB db;

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Before
    public void before() throws Exception {
        RocksDB.loadLibrary();
        this.db = RocksDB.open(folder.getRoot().getPath());
    }

    @After
    public void after() throws Exception {
        this.db.close();
    }

    @Test
    public void serializeLong() throws Exception {
        final long largeNum = 124937190270174L;
        this.db.put("/serializeLong".getBytes(), Longs.toByteArray(largeNum));
        assertEquals(largeNum, Longs.fromByteArray(this.db.get("/serializeLong".getBytes())));
    }
}