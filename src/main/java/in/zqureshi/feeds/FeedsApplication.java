package in.zqureshi.feeds;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.zqureshi.feeds.cli.DumpDBCommand;
import in.zqureshi.feeds.cli.PopulateCommand;
import in.zqureshi.feeds.db.FeedsDB;
import in.zqureshi.feeds.resources.CounterResource;
import in.zqureshi.feeds.resources.FeedResource;
import in.zqureshi.feeds.resources.UserResource;
import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

public class FeedsApplication extends Application<FeedsConfiguration> {

    public static void main(final String[] args) throws Exception {
        new FeedsApplication().run(args);
    }

    @Override
    public String getName() {
        return "Feeds";
    }

    @Override
    public void initialize(final Bootstrap<FeedsConfiguration> bootstrap) {
        bootstrap.addCommand(new DumpDBCommand());
        bootstrap.addCommand(new PopulateCommand());
    }

    @Override
    public void run(final FeedsConfiguration configuration,
                    final Environment environment) throws Exception {
        FeedsDB db = configuration.getFeedsDBFactory().build(environment);
        ObjectMapper mapper = environment.getObjectMapper();

        FeedResource feedResource = new FeedResource(db, mapper);
        environment.jersey().register(feedResource);

        environment.jersey().register(new CounterResource(db));
        environment.jersey().register(new UserResource(db, feedResource, mapper));
    }

}
