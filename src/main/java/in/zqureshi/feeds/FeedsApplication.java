package in.zqureshi.feeds;

import in.zqureshi.feeds.cli.RocksDBCommand;
import in.zqureshi.feeds.db.FeedsDB;
import in.zqureshi.feeds.resources.CounterResource;
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
        bootstrap.addCommand(new RocksDBCommand());
    }

    @Override
    public void run(final FeedsConfiguration configuration,
                    final Environment environment) throws Exception {
        FeedsDB db = configuration.getFeedsDBFactory().build(environment);
        environment.jersey().register(new CounterResource(db));
        environment.jersey().register(new UserResource(db, environment.getObjectMapper()));
    }

}
