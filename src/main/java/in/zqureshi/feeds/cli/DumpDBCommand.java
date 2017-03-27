package in.zqureshi.feeds.cli;

import com.google.common.primitives.Bytes;
import com.google.common.primitives.Longs;
import in.zqureshi.feeds.FeedsConfiguration;
import io.dropwizard.cli.ConfiguredCommand;
import io.dropwizard.setup.Bootstrap;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DumpDBCommand extends ConfiguredCommand<FeedsConfiguration> {
    public static final Logger LOGGER = LoggerFactory.getLogger(DumpDBCommand.class);

    public DumpDBCommand() {
        super("dump", "Dump Database");
    }

    @Override
    protected void run(Bootstrap<FeedsConfiguration> bootstrap, Namespace namespace, FeedsConfiguration configuration) throws Exception {
        RocksDB.loadLibrary();

        Options options = new Options().setCreateIfMissing(false);
        RocksDB db = null;
        try {
            db = RocksDB.open(options, configuration.getFeedsDBFactory().getPath());
        } catch (Exception e) {
            LOGGER.error("Could not open database " + configuration.getFeedsDBFactory().getPath());
            throw e;
        }

        System.out.println("===== COUNTERS =====");
        RocksIterator it = db.newIterator();
        for (it.seek("/system".getBytes());
             it.isValid() && (Bytes.indexOf(it.key(), "/system".getBytes()) == 0);
             it.next()) {
            System.out.println("Counter " + new String(it.key()) + " => " + Longs.fromByteArray(it.value()));
        }

        System.out.println("===== KEYS =====");
        for (it.seek("/data".getBytes());
             it.isValid() && (Bytes.indexOf(it.key(), "/data".getBytes()) == 0);
             it.next()) {
            System.out.println(new String(it.key()) + " => " + new String(it.value()));
        }
    }
}
