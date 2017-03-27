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

import javax.ws.rs.NotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

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
        assertThat(it.hasNext()).isFalse();
    }

    @Test
    public void testListFeeds() throws Exception {
        List<Feed> feeds = feedResource.listFeeds();

        assertThat(feeds.size()).isEqualTo(10);
        for (int i = 0; i < 10; i++) {
            Feed feed = feeds.get(i);
            assertThat(feed.getId()).isEqualTo(10000 + i);
            assertThat(feed.getArticles()).isEmpty();
        }
    }

    @Test
    public void testCreateFeed() throws Exception {
        Feed feed = feedResource.creatFeed();
        assertThat(feed.getId()).isEqualTo(10010);
        assertThat(feed.getArticles()).isEmpty();
    }

    @Test
    public void testShowFeed() throws Exception {
        Feed feed = feedResource.showFeed(10005l, Optional.empty());
        assertThat(feed.getId()).isEqualTo(10005l);
        assertThat(feed.getArticles()).hasSize(50);

        // Should have the last 50 articles in feed.
        for (int i = 0, j = 206; i < 50; i++, j++) {
            Article article = feed.getArticles().get(i);
            assertThat(article.getId()).isEqualTo(10000 + j);
            assertThat(article.getText()).isEqualTo("f:" + 5 + "a:" + j);
        }
    }

    @Test(expected = NoSuchElementException.class)
    public void testShowFeedNotFound() throws Exception {
        feedResource.showFeed(999999999l, Optional.empty());
    }
}