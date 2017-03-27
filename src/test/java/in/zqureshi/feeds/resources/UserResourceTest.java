package in.zqureshi.feeds.resources;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import in.zqureshi.feeds.api.User;
import in.zqureshi.feeds.db.FeedsDB;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

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

    @Test(expected = NoSuchElementException.class)
    public void testGetUserDoesNotExist() throws Exception {
        userResource.getUser(999999l);
    }

    @Test
    public void testSubscribeIsSynchronized() throws Exception {
        for (Method method : UserResource.class.getMethods()) {
            if (method.getName() == "subscribe") {
                assertThat(Modifier.isSynchronized(method.getModifiers())).isTrue();
            }
        }
    }
}