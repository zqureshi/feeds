package in.zqureshi.feeds.cli;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.primitives.Bytes;
import com.google.common.primitives.Longs;
import in.zqureshi.feeds.FeedsConfiguration;
import in.zqureshi.feeds.api.Feed;
import in.zqureshi.feeds.api.User;
import in.zqureshi.feeds.db.FeedsDB;
import in.zqureshi.feeds.resources.FeedResource;
import in.zqureshi.feeds.resources.UserResource;
import io.dropwizard.cli.ConfiguredCommand;
import io.dropwizard.setup.Bootstrap;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PopulateCommand extends ConfiguredCommand<FeedsConfiguration> {
    public static final Logger LOGGER = LoggerFactory.getLogger(PopulateCommand.class);

    public PopulateCommand() {
        super("populate", "Populate test data");
    }

    @Override
    protected void run(Bootstrap<FeedsConfiguration> bootstrap, Namespace namespace, FeedsConfiguration configuration) throws Exception {
        RocksDB.loadLibrary();

        FeedsDB db = null;
        try {
            db = configuration.getFeedsDBFactory().build();
        } catch (Exception e) {
            LOGGER.error("Could not open database " + configuration.getFeedsDBFactory().getPath());
            throw e;
        }

        ObjectMapper mapper = new ObjectMapper();
        FeedResource feedResource = new FeedResource(db, mapper);
        UserResource userResource = new UserResource(db, feedResource, mapper);

        LOGGER.info("Populating Feeds");
        populateFeeds(feedResource);

        LOGGER.info("Populating Users");
        populateUsers(userResource);
    }

    public static void populateFeeds(FeedResource feedResource) throws Exception {
        // Populate feeds and articles
        for (int i = 0; i < 10; i++) {
            Feed feed = feedResource.creatFeed();

            for (int j = 0; j < 256; j++) {
                feedResource.publishArticle(feed.getId(), "f:" + i + "a:" + j);
            }
        }
    }

    public static void populateUsers(UserResource userResource) throws Exception {
        // Populate users and subscribe to feeds
        for (int i = 0; i < 10; i++) {
            User user = userResource.createUser();

            for (int j = 0; j < 5; j++) {
                userResource.subscribe(user.getId(), 10000l + j);
            }
        }
    }
}