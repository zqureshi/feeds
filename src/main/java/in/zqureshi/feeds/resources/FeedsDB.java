package in.zqureshi.feeds.resources;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.primitives.Bytes;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import io.dropwizard.lifecycle.Managed;
import io.dropwizard.setup.Environment;
import org.hibernate.validator.constraints.NotEmpty;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.rocksdb.RocksIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;

public class FeedsDB implements Managed {
    private static final Logger LOGGER = LoggerFactory.getLogger(FeedsDB.class);

    private static final String SYSTEM_PREFIX = "/system";
    private static final String COUNTERS_PREFIX = SYSTEM_PREFIX + "/counters";

    private static final String USERS_PREFIX = "/users";
    private static final String FEEDS_PREFIX = "/feeds";

    private RocksDB db;

    public FeedsDB(String path) throws RocksDBException {
        RocksDB.loadLibrary();

        Options options = new Options();
        options.setCreateIfMissing(true);
        options.setWalSizeLimitMB(1024);

        this.db = RocksDB.open(options, path);
    }

    public synchronized long incrementCounter(final String counter) {
        try {
            byte[] key = (COUNTERS_PREFIX + counter).getBytes();
            byte[] current = this.db.get(key);

            if (current == null) {
                current = Longs.toByteArray(0);
            }

            this.db.put(key, Longs.toByteArray(Longs.fromByteArray(current) + 1));

            return Longs.fromByteArray(current);
        } catch (RocksDBException e) {
            throw new RuntimeException(e);
        }
    }

    public ImmutableMap<String, Long> counters() {
        HashMap<String, Long> map = new HashMap<>(50);

        RocksIterator iterator = this.db.newIterator();

        if (iterator != null) {
            for (iterator.seek(COUNTERS_PREFIX.getBytes());
                 iterator.isValid() && Bytes.indexOf(iterator.key(), COUNTERS_PREFIX.getBytes()) == 0;
                 iterator.next()) {
                map.put(
                    new String(Arrays.copyOfRange(iterator.key(), COUNTERS_PREFIX.length(), iterator.key().length)),
                    Longs.fromByteArray(iterator.value())
                );
            }
        }

        return ImmutableMap.copyOf(map);
    }

    @Override
    public void start() throws RocksDBException {
    }

    @Override
    public void stop() throws RocksDBException {
        this.db.close();
    }

    public static class FeedsDBFactory {
        @NotEmpty
        private String path;

        @JsonProperty
        public String getPath() {
            return path;
        }

        @JsonProperty
        public void setPath(String path) {
            this.path = path;
        }

        public FeedsDB build(Environment environment) throws RocksDBException {
            FeedsDB db = new FeedsDB(getPath());
            environment.lifecycle().manage(db);
            return db;
        }
    }
}
