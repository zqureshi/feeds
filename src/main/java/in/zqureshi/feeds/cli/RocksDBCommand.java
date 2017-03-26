package in.zqureshi.feeds.cli;

import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import in.zqureshi.feeds.FeedsConfiguration;
import io.dropwizard.cli.ConfiguredCommand;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
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
        super("rocksdb", "Dump RocksDB");
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
            feedsConfiguration.getFeedsDBFactory().build().stop();
            db = RocksDB.open(options, feedsConfiguration.getFeedsDBFactory().getPath());
        } catch (Exception e) {
            LOGGER.error("Could not open database", e);
        }

        RocksIterator iterator = db.newIterator();
        for (iterator.seekToFirst(); iterator.isValid(); iterator.next()) {
            System.out.println(new String(iterator.key()) + " => " + Longs.fromByteArray(iterator.value()));
        }
    }
}
