package in.zqureshi.feeds.resources;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.lifecycle.Managed;
import io.dropwizard.setup.Environment;
import org.hibernate.validator.constraints.NotEmpty;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FeedsDB implements Managed {
    private static final Logger LOGGER = LoggerFactory.getLogger(FeedsDB.class);

    private static final String SYSTEM_PREFIX = "/system";
    private static final String COUNTERS_PREFIX = SYSTEM_PREFIX + "/counters";

    private static final String USERS_PREFIX = "/users";
    private static final String FEEDS_PREFIX = "/feeds";

    private RocksDB db;

    public FeedsDB(String path) throws RocksDBException {
        RocksDB.loadLibrary();

        Options options = new Options().setCreateIfMissing(true);
        this.db = RocksDB.open(options, path);
    }

    @Override
    public void start() throws Exception {
    }

    @Override
    public void stop() throws Exception {
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
