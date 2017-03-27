package in.zqureshi.feeds.resources;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.zqureshi.feeds.api.Article;
import in.zqureshi.feeds.api.Feed;
import in.zqureshi.feeds.db.FeedsDB;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.rocksdb.RocksDBException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

public class FeedResourceTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(FeedResourceTest.class);

    private FeedsDB db;
    private ObjectMapper mapper;
    private FeedResource feedResource;

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Before
    public void before() throws Exception {
        db = new FeedsDB(folder.getRoot().getPath());
        mapper = new ObjectMapper();
        feedResource = new FeedResource(db, mapper);

        // Populate feeds and articles
        for (int i = 0; i < 10; i++) {
            Feed feed = feedResource.creatFeed();

            for(int j = 0; j < 256; j++) {
                feedResource.publishArticle(feed.getId(), "f:" + i + "a:" + j);
            }
        }
    }

    @After
    public void after() throws Exception {
        db.stop();
    }

    @Test
    public void initiallyEmpty() throws Exception {
        TemporaryFolder anotherFolder = new TemporaryFolder();
        anotherFolder.create();

        feedResource = new FeedResource(
            new FeedsDB(anotherFolder.getRoot().getPath()),
            mapper
        );

        assertThat(feedResource.listFeeds()).isEmpty();
    }

    @Test
    public void validatePopulated() throws Exception {
        // Validate global feeds counter
        assertThat(db.getCounter("/feeds")).isEqualTo(10010);

        // Validate article count for each feed
        for (int i = 10000; i < 10010; i++) {
            assertThat(db.getCounter("/articles/" + i)).isEqualTo(10256);
        }

        // Validate record for each feed
        FeedsDB.PrefixIterator it = db.scan("/feeds/");
        for (int i = 10000; i < 10010; i++) {
            Feed feed = mapper.readValue(it.next(), Feed.class);
            assertThat(feed.getId()).isEqualTo(i);
            assertThat(feed.getArticles()).isEmpty();
        }
        assertThat(it.hasNext()).isFalse();

        // Validate record for each article
        it = db.scan("/articles");
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 256; j++) {
                Article article = mapper.readValue(it.next(), Article.class);
                assertThat(article.getId()).isEqualTo(10000 + j);
                assertThat(article.getText()).isEqualToIgnoringCase("f:" + i + "a:" + j);
            }
        }
    }
}