package in.zqureshi.feeds.resources;

import com.google.common.collect.ImmutableMap;
import in.zqureshi.feeds.api.Counter;
import in.zqureshi.feeds.db.FeedsDB;
import jdk.nashorn.internal.ir.annotations.Immutable;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Path("/counters")
@Produces(MediaType.APPLICATION_JSON)
public class CounterResource {
    private FeedsDB db;

    public CounterResource(FeedsDB db) {
        this.db = db;
    }

    @GET
    public List<Counter> listCounters() {
        ImmutableMap<String, Long> rawCounters = db.counters();
        ArrayList<Counter> counters = new ArrayList<>(rawCounters.size());

        for (Map.Entry<String, Long> entry : rawCounters.entrySet()) {
            counters.add(new Counter(entry.getKey(), entry.getValue()));
        }

        return counters;
    }
}
