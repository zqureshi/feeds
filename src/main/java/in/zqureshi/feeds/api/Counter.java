package in.zqureshi.feeds.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ComparisonChain;
import org.glassfish.jersey.server.JSONP;

import javax.validation.constraints.NotNull;

public class Counter implements Comparable<Counter> {
    private String name;
    private Long value;

    public Counter() {
        // Jackson deserialization
    }

    public Counter(String name, Long value) {
        this.name = name;
        this.value = value;
    }

    @JsonProperty
    public String getName() {
        return name;
    }

    @JsonProperty
    public Long getValue() {
        return value;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("name", name)
            .add("value", value)
            .toString();
    }

    @Override
    public int compareTo(Counter that) {
        return ComparisonChain.start()
            .compare(this.name, that.name)
            .compare(this.value, that.value)
            .result();
    }

    @Override
    public boolean equals(Object that) {
        if (that == null) { return false; }
        if (this == that) { return true; }

        if (that instanceof Counter) {
            return this.compareTo((Counter) that) == 0;
        }

        return false;
    }
}
