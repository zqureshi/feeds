package in.zqureshi.feeds.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Lists;

import java.util.Map;
import java.util.Set;

public class User implements Comparable<User> {
    private Long id;
    private Map<Long, Long> feeds;

    public User() {
        // Jackson deserialization
    }

    public User(Long id, Map<Long, Long> feeds) {
        this.id = id;
        this.feeds = feeds;
    }

    @JsonProperty
    public Long getId() {
        return id;
    }

    @JsonProperty
    public void setId(Long id) {
        this.id = id;
    }

    @JsonProperty
    public Map<Long, Long> getFeeds() {
        return feeds;
    }

    @JsonProperty
    public void setFeeds(Map<Long, Long> feeds) {
        this.feeds = feeds;
    }

    public void addFeed(Long id, Long startIndex) {
        feeds.putIfAbsent(id, startIndex);
    }

    public void removeFeed(Long id) {
        feeds.remove(id);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("id", id)
            .add("feeds", feeds)
            .toString();
    }

    @Override
    public int compareTo(User that) {
        final int partial = ComparisonChain.start()
            .compare(this.id, that.id)
            .result();

        if (partial != 0) { return partial; }

        if (feeds.equals(that.feeds)) {
            return 0;
        } else if (feeds.size() == that.feeds.size()) {
            return -1;
        } else if (feeds.size() < that.feeds.size()) {
            return -2;
        } else {
            return +2;
        }
    }

    @Override
    public boolean equals(Object that) {
        if (that == null) { return false; }
        if (this == that) { return true; }

        if (that instanceof User) {
            return this.compareTo((User) that) == 0;
        }

        return false;
    }
}
