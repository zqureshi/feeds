package in.zqureshi.feeds.cli;

import com.google.common.primitives.Ints;
import in.zqureshi.feeds.FeedsConfiguration;
import io.dropwizard.cli.ConfiguredCommand;
import io.dropwizard.setup.Bootstrap;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.rocksdb.RocksIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;

public class RocksDBCommand extends ConfiguredCommand<FeedsConfiguration> {
    private static final Logger LOGGER = LoggerFactory.getLogger(RocksDBCommand.class);

    public RocksDBCommand() {
        super("rocksdb", "RocksDB WAL operations");
    }

    @Override
    public void configure(Subparser subparser) {
        super.configure(subparser);
    }

    @Override
    protected void run(Bootstrap<FeedsConfiguration> bootstrap,
                       Namespace namespace,
                       FeedsConfiguration feedsConfiguration) throws Exception {
        RocksDB.loadLibrary();

        Options options = new Options().setCreateIfMissing(true);
        RocksDB db = null;
        try {
            db = RocksDB.open(options, "/tmp/feeds.rocksdb");
            LOGGER.info("Successfully opened database!");
        } catch (Exception e) {
            LOGGER.error("Could not open database", e);
        }

        final int rand = (new Random()).nextInt(128);
        LOGGER.info("Inserting " + rand);
        db.put(Ints.toByteArray(rand), "".getBytes());

        LOGGER.info("Dumping database");
        RocksIterator iterator = db.newIterator();
        for (iterator.seekToFirst(); iterator.isValid(); iterator.next()) {
            System.out.println(Ints.fromByteArray(iterator.key()));
        }
    }
}
