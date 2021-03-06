package in.zqureshi.feeds.db;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableMap;
import com.google.common.primitives.Bytes;
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

import javax.validation.constraints.NotNull;
import java.util.*;

public class FeedsDB implements Managed {
    private static final Logger LOGGER = LoggerFactory.getLogger(FeedsDB.class);

    private static final String SYSTEM_PREFIX = "/system";
    private static final String COUNTERS_PREFIX = SYSTEM_PREFIX + "/counters";

    private static final String DATA_PREFIX = "/data";

    // Hacky way to provide stable ordering of article, feeds.
    public static final Long INITIAL_COUNTER_VALUE = 10000L;

    private RocksDB db;

    public FeedsDB(String path) throws RocksDBException {
        RocksDB.loadLibrary();

        Options options = new Options();
        options.setCreateIfMissing(true);
        options.setWalSizeLimitMB(1024);

        db = RocksDB.open(options, path);
    }

    public long getCounter(final String counter) {
        return getCounterInternal(counter);
    }

    public synchronized long incrementCounter(final String counter) {
        long current = getCounterInternal(counter);
        putCounterInternal(counter, current + 1);

        return current;
    }

    public ImmutableMap<String, Long> counters() {
        HashMap<String, Long> map = new HashMap<>(50);

        RocksIterator iterator = db.newIterator();

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

    public byte[] get(String key) {
        try {
            return db.get((DATA_PREFIX + key).getBytes());
        } catch (RocksDBException e) {
            throw new RuntimeException(e);
        }
    }

    // No synchronization done, it is up to consumer to add guarantee.
    // Should use counter service to guarantee unique keys.
    //
    // TODO: RocksDB java doesn't have OptimisticTransactionDB yet.
    public void put(String key, byte[] value) {
        try {
            db.put((DATA_PREFIX + key).getBytes(), value);
        } catch (RocksDBException e) {
            throw new RuntimeException(e);
        }
    }

    public PrefixIterator scan(String prefix) {
        return scan(prefix, Optional.empty());
    }

    public PrefixIterator scan(String prefix, String startIndex) {
        return scan(prefix, Optional.of(startIndex));
    }

    @Override
    public void start() throws RocksDBException {
    }

    @Override
    public void stop() throws RocksDBException {
        db.close();
    }

    public static class FeedsDBFactory {
        @NotEmpty
        private String path;

        @NotNull
        private Map<String, Long> counters = Collections.emptyMap();

        @JsonProperty
        public String getPath() {
            return path;
        }

        @JsonProperty
        public void setPath(String path) {
            this.path = path;
        }

        @JsonProperty
        public Map<String, Long> getCounters() {
            return counters;
        }

        @JsonProperty
        public void setCounters(Map<String, Long> counters) {
            this.counters = counters;
        }

        public FeedsDB build(Environment environment) throws RocksDBException {
            FeedsDB db = build();
            environment.lifecycle().manage(db);

            return db;
        }

        public FeedsDB build() throws RocksDBException {
            FeedsDB db = new FeedsDB(getPath());

            // Load all configured counters
            for (Map.Entry<String, Long> entry : getCounters().entrySet()) {
                if (db.getCounterInternal(entry.getKey()) == INITIAL_COUNTER_VALUE) {
                    db.putCounterInternal(entry.getKey(), entry.getValue());
                }
            }

            return db;
        }
    }

    public class PrefixIterator implements Iterator<byte[]> {
        private byte[] prefix;
        private RocksIterator rit;

        public PrefixIterator(RocksIterator rit, String prefix, Optional<String> startIndex) {
            this.prefix = prefix.getBytes();
            this.rit = rit;

            String key = prefix;
            if (startIndex.isPresent()) {
                key += startIndex.get();
            }

            if (this.rit != null) { rit.seek(key.getBytes()); }
        }

        @Override
        public boolean hasNext() {
            return rit != null && rit.isValid() && Bytes.indexOf(rit.key(), prefix) == 0;
        }

        @Override
        public byte[] next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }

            byte[] current = rit.value();
            rit.next();

            return current;
        }
    }

    private long getCounterInternal(String counter) {
        try {
            byte[] current = db.get((COUNTERS_PREFIX + counter).getBytes());

            if (current == null) {
                return INITIAL_COUNTER_VALUE;
            }

            return Longs.fromByteArray(current);
        } catch (RocksDBException e) {
            throw new RuntimeException(e);
        }
    }

    private void putCounterInternal(String counter, long value) {
        try {
            db.put(
                (COUNTERS_PREFIX + counter).getBytes(),
                Longs.toByteArray(value)
            );
        } catch (RocksDBException e) {
            throw new RuntimeException(e);
        }
    }

    private PrefixIterator scan(String prefix, Optional<String> startIndex) {
        return new PrefixIterator(db.newIterator(), DATA_PREFIX + prefix, startIndex);
    }
}
