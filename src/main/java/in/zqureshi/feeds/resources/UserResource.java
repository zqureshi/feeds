package in.zqureshi.feeds.resources;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.collect.Sets;
import in.zqureshi.feeds.api.Article;
import in.zqureshi.feeds.api.Feed;
import in.zqureshi.feeds.api.User;
import in.zqureshi.feeds.db.FeedsDB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jws.soap.SOAPBinding;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.io.IOError;
import java.io.IOException;
import java.util.*;

@Path("/v1/users")
@Produces(MediaType.APPLICATION_JSON)
public class UserResource {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserResource.class);

    private static final String USERS_COUNTER = "/users";
    private static final String USERS_PREFIX = "/users/";

    private FeedsDB db;
    private ObjectMapper mapper;
    private FeedResource feedResource;

    public UserResource(FeedsDB db, FeedResource feedResource, ObjectMapper mapper) {
        this.db = db;
        this.mapper = mapper;
        this.feedResource = feedResource;
    }

    @GET
    public List<User> listUsers() throws IOException {
        List<User> users = new ArrayList<>();

        for (FeedsDB.PrefixIterator it = db.scan(USERS_PREFIX);
             it.hasNext();) {
            users.add(mapper.readValue(it.next(), User.class));
        }

        return users;
    }

    // Don't need synchronization because uniqueness is guaranteed by atomic incrementCounter.
    @POST
    public User createUser() throws JsonProcessingException {
        final long id = db.incrementCounter(USERS_COUNTER);
        User user = new User(id, Collections.emptyMap());

        return updateUser(user);
    }

    public User updateUser(User user) throws JsonProcessingException {
        db.put(USERS_PREFIX + user.getId(), mapper.writeValueAsBytes(user));
        return user;
    }

    @GET
    @Path("/{id}")
    public User getUser(@PathParam("id") Long id) throws IOException {
        byte[] result = db.get(USERS_PREFIX + id);
        if (result == null) {
            throw new NoSuchElementException();
        }

        return mapper.readValue(result, User.class);
    }

    @POST
    @Path("/{id}/subscribe")
    @Produces(MediaType.APPLICATION_JSON)
    public synchronized User subscribe(@PathParam("id") Long id, @QueryParam("feedId") Long feedId) throws IOException {
        User user = getUser(id);

        if (!user.getFeeds().containsKey(feedId)) {
            Feed feed = feedResource.showFeed(feedId, Optional.empty());
            Long feedIndex = FeedsDB.INITIAL_COUNTER_VALUE;

            if (feed.getArticles().size() > 0) {
                Article lastArticle = feed.getArticles().get(feed.getArticles().size() - 1);
                feedIndex = lastArticle.getId();
            }

            user.getFeeds().put(feedId, feedIndex);
            return updateUser(user);
        }

        return user;
    }

    @POST
    @Path("/{id}/unsubscribe")
    @Produces(MediaType.APPLICATION_JSON)
    public synchronized User unsubscribe(@PathParam("id") Long id, @QueryParam("feedId") Long feedId) throws IOException {
        User user = getUser(id);

        if (user.getFeeds().containsKey(feedId)) {
            user.getFeeds().remove(feedId);
            return updateUser(user);
        }

        return user;
    }

    @POST
    @Path("/{id}/consumeFeeds")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public List<Feed> consumeFeeds(@PathParam("id") Long id, Map<Long, Long> startIds) throws IOException {
        User user = getUser(id);
        user.getFeeds().putAll(startIds);

        List<Feed> feeds = new ArrayList<>(user.getFeeds().size());
        for (Map.Entry<Long, Long> entry : user.getFeeds().entrySet()) {
            feeds.add(feedResource.showFeed(entry.getKey(), Optional.of(entry.getValue())));
        }

        return feeds;
    }
}
