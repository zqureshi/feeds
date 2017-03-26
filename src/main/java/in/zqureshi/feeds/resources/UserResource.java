package in.zqureshi.feeds.resources;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.collect.Sets;
import in.zqureshi.feeds.api.User;
import in.zqureshi.feeds.db.FeedsDB;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.io.IOError;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Path("/v1/users")
@Produces(MediaType.APPLICATION_JSON)
public class UserResource {
    private static final String USERS_COUNTER = "/users";
    private static final String USERS_PREFIX = "/users/";

    private FeedsDB db;
    private ObjectMapper mapper;

    public UserResource(FeedsDB db, ObjectMapper mapper) {
        this.db = db;
        this.mapper = mapper;
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

    @POST
    public User createUser() throws JsonProcessingException {
        final long id = db.incrementCounter(USERS_COUNTER);
        User user = new User(id, Collections.emptyMap());
        db.put(USERS_PREFIX + id, mapper.writeValueAsBytes(user));

        return user;
    }
}
