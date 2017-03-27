package in.zqureshi.feeds.resources;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import in.zqureshi.feeds.api.Article;
import in.zqureshi.feeds.api.Feed;
import in.zqureshi.feeds.db.FeedsDB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.*;

@Path("/v1/feeds")
@Produces(MediaType.APPLICATION_JSON)
public class FeedResource {
    private static final Logger LOGGER = LoggerFactory.getLogger(FeedResource.class);

    private static final String FEEDS_COUNTER = "/feeds";
    private static final String ARTICLES_COUNTER_PREFIX = "/articles/";

    private static final String FEEDS_PREFIX = "/feeds/";
    private static final String ARTICLES_PREFIX = "/articles/";

    private static final int PAGE_SIZE = 50;

    private FeedsDB db;
    private ObjectMapper mapper;

    public FeedResource(FeedsDB db, ObjectMapper mapper) {
        this.db = db;
        this.mapper = mapper;
    }

    // This method purposely does not fill the articles for each feed since
    // it is only meant to list what feeds are available.
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

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Feed showFeed(@PathParam("id") Long id, @QueryParam("startId") Optional<Long> startId) throws IOException {
        // Check if feed exists
        if (db.get(FEEDS_PREFIX + id) == null) {
            throw new NotFoundException();
        }

        final long articleCount = db.getCounter(ARTICLES_COUNTER_PREFIX + id);
        long startIndex = FeedsDB.INITIAL_COUNTER_VALUE;
        if (startId.isPresent()) {
            startIndex = startId.get();

            if (startIndex < FeedsDB.INITIAL_COUNTER_VALUE
                || startIndex > articleCount) {
                throw new IndexOutOfBoundsException();
            }
        } else {
            // return last 50 articles
            startIndex = Math.max(FeedsDB.INITIAL_COUNTER_VALUE, articleCount - PAGE_SIZE);
        }

        List<Article> articles = new ArrayList<>(PAGE_SIZE);
        for (FeedsDB.PrefixIterator it = db.scan(constructArticlePrefix(id), Long.toString(startIndex));
             it.hasNext() && articles.size() < PAGE_SIZE;) {
            articles.add(mapper.readValue(it.next(), Article.class));
        }

        return new Feed(id, articles);
    }

    @POST
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.TEXT_PLAIN)
    public Article publishArticle(@PathParam("id") Long feedId, String text) throws JsonProcessingException {
        if (db.get(FEEDS_PREFIX + feedId) == null) {
            throw new NotFoundException();
        }

        final long articleId = db.incrementCounter(ARTICLES_COUNTER_PREFIX + feedId);
        Article article = new Article(articleId, text);

        db.put(constructArticleKey(feedId, articleId), mapper.writeValueAsBytes(article));

        return article;
    }

    private String constructArticlePrefix(final Long feedId) {
        return ARTICLES_PREFIX + feedId + "/";
    }

    private String constructArticleKey(final Long feedId, final Long startIndex) {
         return constructArticlePrefix(feedId) + startIndex;
    }
}
