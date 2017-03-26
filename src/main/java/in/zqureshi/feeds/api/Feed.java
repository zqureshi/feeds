package in.zqureshi.feeds.api;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class Feed {
    private Long id;
    private List<Article> articles;

    public Feed() {
        // Jackson deserialization
    }

    public Feed(Long id, List<Article> articles) {
        this.id = id;
        this.articles = articles;
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
    public List<Article> getArticles() {
        return articles;
    }

    @JsonProperty
    public void setArticles(List<Article> articles) {
        this.articles = articles;
    }
}
