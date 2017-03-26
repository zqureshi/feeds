package in.zqureshi.feeds.resources;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import in.zqureshi.feeds.api.Feed;
import in.zqureshi.feeds.db.FeedsDB;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Path("/v1/feeds")
@Produces(MediaType.APPLICATION_JSON)
public class FeedResource {
    private static final String FEEDS_COUNTER = "/feeds";
    private static final String ARTICLES_COUNTER_PREFIX = "/articles";

    private static final String FEEDS_PREFIX = "/feeds/";
    private static final String ARTICLES_PREFIX = "/articles/";

    private FeedsDB db;
    private ObjectMapper mapper;

    public FeedResource(FeedsDB db, ObjectMapper mapper) {
        this.db = db;
        this.mapper = mapper;
    }

    @GET
    public List<Feed> listFeeds() throws IOException {
        List<Feed> feeds = new ArrayList<>();

        for (FeedsDB.PrefixIterator it = db.scan(FEEDS_PREFIX);
            it.hasNext();) {
            feeds.add(mapper.readValue(it.next(), Feed.class));
        }

        return feeds;
    }

    // Don't need synchronization because uniqueness is guaranteed by atomic incrementCounter.
    @POST
    public Feed creatFeed() throws JsonProcessingException {
        final Long id = db.incrementCounter(FEEDS_COUNTER);
        Feed feed = new Feed(id, Collections.emptyList());
        db.put(FEEDS_PREFIX + id, mapper.writeValueAsBytes(feed));

        return feed;
    }
}
