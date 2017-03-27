package in.zqureshi.feeds.resources;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import in.zqureshi.feeds.api.Article;
import in.zqureshi.feeds.api.Feed;
import in.zqureshi.feeds.api.User;
import in.zqureshi.feeds.db.FeedsDB;
import org.assertj.core.api.AssertDelegateTarget;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.NotFoundException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

import static org.assertj.core.api.Assertions.*;

public class UserResourceTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserResourceTest.class);

    private FeedsDB db;
    private ObjectMapper mapper;
    private FeedResource feedResource;
    private UserResource userResource;

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Before
    public void before() throws Exception {
        db = new FeedsDB(folder.getRoot().getPath());
        mapper = new ObjectMapper();
        feedResource = new FeedResource(db, mapper);
        userResource = new UserResource(db, feedResource, mapper);

        FeedResourceTest.populateDB(feedResource);
        UserResourceTest.populateDB(userResource);
    }

    public static void populateDB(UserResource userResource) throws Exception {
        // Populate users and subscribe to feeds
        for (int i = 0; i < 10; i++) {
            User user = userResource.createUser();

            for (int j = 0; j < 5; j++) {
                userResource.subscribe(user.getId(), 10000l + j);
            }
        }
    }

    @Test
    public void initiallyEmpty() throws Exception {
        TemporaryFolder anotherFolder = new TemporaryFolder();
        anotherFolder.create();

        db = new FeedsDB(anotherFolder.getRoot().getPath());
        userResource = new UserResource(db, new FeedResource(db, mapper), mapper);

        assertThat(userResource.listUsers()).isEmpty();
    }

    @Test
    public void validatePopulated() throws Exception {
        // Validate global users counter
        assertThat(db.getCounter("/users")).isEqualTo(10010);

        // Validate record for each user
        FeedsDB.PrefixIterator it = db.scan("/users/");
        for (int i = 10000; i < 10010; i++) {
            User user = mapper.readValue(it.next(), User.class);
            assertThat(user.getId()).isEqualTo(i);
            assertThat(user.getFeeds()).hasSize(5);

            assertThat(user.getFeeds().keySet())
                .isEqualTo(Sets.newHashSet(10000l, 10001l, 10002l, 10003l, 10004l));
            for (Map.Entry<Long, Long> entry : user.getFeeds().entrySet()) {
                assertThat(entry.getValue()).isEqualTo(10255);
            }
        }
        assertThat(it.hasNext()).isFalse();
    }

    @Test
    public void testListUsers() throws Exception {
        List<User> users = userResource.listUsers();
        assertThat(users).hasSize(10);

        for (int i = 0; i < 10; i++) {
            User user = users.get(i);
            assertThat(user.getId()).isEqualTo(10000 + i);
            assertThat(user.getFeeds()).hasSize(5);

            assertThat(user.getFeeds().keySet())
                .isEqualTo(Sets.newHashSet(
                    10000l, 10001l, 10002l, 10003l, 10004l
                ));

            // startIndex for each feed must be 255 since user started
            // tracking it after all articles had been published.
            for (Map.Entry<Long, Long> entry : user.getFeeds().entrySet()) {
                assertThat(entry.getValue()).isEqualTo(10255);
            }
        }
    }

    @Test
    public void testCreateUser() throws Exception {
        User user = userResource.createUser();
        assertThat(user.getId()).isEqualTo(10010);
        assertThat(user.getFeeds()).isEmpty();

        List<User> users = userResource.listUsers();
        assertThat(users).hasSize(11);
        assertThat(users.get(10)).isEqualTo(user);
    }

    @Test
    public void testGetUser() throws Exception {
        User user = userResource.getUser(10005l);
        assertThat(user.getId()).isEqualTo(10005);
        assertThat(user.getFeeds()).hasSize(5);
    }

    @Test(expected = NotFoundException.class)
    public void testGetUserDoesNotExist() throws Exception {
        userResource.getUser(999999l);
    }

    @Test
    public void testSubscribe() throws Exception {
        User user = userResource.getUser(10005l);
        assertThat(user.getFeeds()).hasSize(5);

        user = userResource.subscribe(10005l, 10006l);
        assertThat(user.getFeeds()).hasSize(6);
        assertThat(user.getFeeds().get(10006l)).isEqualTo(10255l);

        user = userResource.getUser(10005l);
        assertThat(user.getFeeds()).hasSize(6);
        assertThat(user.getFeeds().get(10006l)).isEqualTo(10255l);
    }

    @Test
    public void testSubscribeIsIdempotent() throws Exception {
        User user = userResource.getUser(10005l);
        assertThat(user.getFeeds()).hasSize(5);
        assertThat(user.getFeeds().get(10000l)).isEqualTo(10255l);

        user = userResource.subscribe(10005l, 10000l);
        assertThat(user.getFeeds()).hasSize(5);
        assertThat(user.getFeeds().get(10000l)).isEqualTo(10255l);
    }

    @Test
    public void testSubscribeAfterPublishing() throws Exception {
        User user = userResource.getUser(10005l);
        assertThat(user.getFeeds()).hasSize(5);
        assertThat(user.getFeeds().containsKey(10008l)).isFalse();

        Article article = null;
        for (int i = 0; i < 100; i++) {
            article = feedResource.publishArticle(10008l, "#DEADBEEF:" + i);
        }
        assertThat(article.getId()).isEqualTo(10355l);

        user = userResource.subscribe(10005l, 10008l);
        assertThat(user.getFeeds()).hasSize(6);
        assertThat(user.getFeeds().get(10008l)).isEqualTo(10355l);
    }

    @Test(expected = NotFoundException.class)
    public void testSubscribeUserDoesNotExist() throws Exception {
        userResource.subscribe(99999999l, 10006l);
    }

    @Test(expected = NotFoundException.class)
    public void testSubscribeFeedDoesNotExist() throws Exception {
        userResource.subscribe(10005l, 99999999l);
    }

    @Test
    public void testUnsubscribe() throws Exception {
        User user = userResource.getUser(10003l);
        assertThat(user.getFeeds()).hasSize(5);
        assertThat(user.getFeeds().containsKey(10002l)).isTrue();

        user = userResource.unsubscribe(10003l, 10002l);
        assertThat(user.getFeeds()).hasSize(4);
        assertThat(user.getFeeds().containsKey(10002l)).isFalse();

        user = userResource.getUser(10003l);
        assertThat(user.getFeeds()).hasSize(4);
        assertThat(user.getFeeds().containsKey(10002l)).isFalse();
    }

    @Test
    public void testUnsubscribeIsIdempotent() throws Exception {
        User user = userResource.unsubscribe(10003l, 10002l);
        assertThat(user.getFeeds()).hasSize(4);
        assertThat(user.getFeeds().containsKey(10002l)).isFalse();

        user = userResource.unsubscribe(10003l, 10002l);
        assertThat(user.getFeeds()).hasSize(4);
        assertThat(user.getFeeds().containsKey(10002l)).isFalse();
    }

    @Test(expected = NotFoundException.class)
    public void testUnsubscribeUserDoesNotExist() throws Exception {
        userResource.unsubscribe(999999999l, 10005l);
    }

    // Doesn't throw an exception because will short circuit if user
    // is not subscribed to this feed.
    @Test
    public void testUnsubscribeFeedDoesNotExist() throws Exception {
        User user = userResource.getUser(10005l);
        assertThat(user.getFeeds()).hasSize(5);

        user = userResource.unsubscribe(10005l, 999999l);
        assertThat(user.getFeeds()).hasSize(5);
    }

    @Test
    public void testConsumeFeeds() throws Exception {
        List<Feed> feeds = userResource.consumeFeeds(10005l, Collections.emptyMap());
        assertThat(feeds).hasSize(5);

        Set<Long> feedsSeen = new HashSet<>();
        for (Feed feed : feeds) {
            feedsSeen.add(feed.getId());
            // Since default pointers are at the last element because user subscribed
            // after all publishings.
            assertThat(feed.getArticles()).hasSize(1);
            assertThat(feed.getArticles().get(0).getId()).isEqualTo(10255l);
        }

        assertThat(feedsSeen)
            .isEqualTo(Sets.newHashSet(10000l, 10001l, 10002l, 10003l, 10004l));
    }

    @Test
    public void testConsumeFeedsWithIndices() throws Exception {
        List<Feed> feeds = userResource.consumeFeeds(
            10005l,
            ImmutableMap.of(
                10000l, 10200l,
                10003l, 10250l
            )
        );

        assertThat(feeds).hasSize(5);

        Set<Long> feedsSeen = new HashSet<>();
        for (Feed feed : feeds) {
            feedsSeen.add(feed.getId());

            if (feed.getId() == 10000l) {
                assertThat(feed.getArticles()).hasSize(50);
                for (int i = 0, j = 10200; i < 50; i++, j++) {
                    assertThat(feed.getArticles().get(i).getId()).isEqualTo(j);
                }
            } else if (feed.getId() == 10003l) {
                assertThat(feed.getArticles()).hasSize(6);
                for (int i = 0, j = 10250; i < 6; i++, j++) {
                    assertThat(feed.getArticles().get(i).getId()).isEqualTo(j);
                }
            } else {
                assertThat(feed.getArticles()).hasSize(1);
                assertThat(feed.getArticles().get(0).getId()).isEqualTo(10255l);
            }
        }
    }

    @Test(expected = NotFoundException.class)
    public void testConsumeFeedsFeedDoesNotExist() throws Exception {
        userResource.consumeFeeds(10005l, ImmutableMap.of(99999l, 10000l));
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testConsumeFeedsIndexOutOfLowerBound() throws Exception {
        userResource.consumeFeeds(10005l, ImmutableMap.of(10001l, 9999l));
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testConsumeFeedsIndexOutOfUpperBound() throws Exception {
        userResource.consumeFeeds(10005l, ImmutableMap.of(10001l, 99999999l));
    }

    // Same shitty solution to test atomicity, an a better test suite would
    // be continuously fuzz testing.

    @Test
    public void testSubscribeIsSynchronized() throws Exception {
        for (Method method : UserResource.class.getMethods()) {
            if (method.getName() == "subscribe") {
                assertThat(Modifier.isSynchronized(method.getModifiers())).isTrue();
            }
        }
    }

    @Test
    public void testUnsubscribeIsSynchronized() throws Exception {
        for (Method method : UserResource.class.getMethods()) {
            if (method.getName() == "unsubscribe") {
                assertThat(Modifier.isSynchronized(method.getModifiers())).isTrue();
            }
        }
    }
}