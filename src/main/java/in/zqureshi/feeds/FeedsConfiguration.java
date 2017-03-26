package in.zqureshi.feeds;

import in.zqureshi.feeds.db.FeedsDB;
import io.dropwizard.Configuration;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.Valid;
import javax.validation.constraints.*;

public class FeedsConfiguration extends Configuration {
    @Valid
    @NotNull
    private FeedsDB.FeedsDBFactory db;

    @JsonProperty("db")
    public FeedsDB.FeedsDBFactory getFeedsDBFactory() {
        return db;
    }

    @JsonProperty("db")
    public void setFeedsDBFactory(FeedsDB.FeedsDBFactory db) {
        this.db = db;
    }
}
