package in.zqureshi.feeds.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.glassfish.jersey.server.JSONP;

public class Article {
    private Long id;
    private String text;

    public Article() {
        // Jackson deserialization
    }

    public Article(Long id, String text) {
        this.id = id;
        this.text = text;
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
    public String getText() {
        return text;
    }

    @JsonProperty
    public void setText(String text) {
        this.text = text;
    }
}
