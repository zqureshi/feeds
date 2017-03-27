# Feeds

## ❯ Design Overview
- `RocksDB` WAL for durability
- Atomic counters persisted in `/system` used by all write operations
- User, Feed, Articles persisted in `/data`
- 3 synchronized write methods and everything else is lock free  
  Counters maintained for each resource type and articles per feed  
  - `/system/counters/{users|feeds}`
  - `/system/counters/articles/{feedId}`

  When adding a new resource, counter is incremented and then resource written
  under that id. This means that resource ids will always be monotonically
  increasing although there might be gaps in cases where a counter is acquired
  but server fails before writing actual data.
  - `/data/users/{userId}`
  - `/data/feeds/{feedId}`
  - `/data/articles/{feedId}/{articleId}`

  This gives nice scan operations when fetching entries per feed since keys
  are ordered in the LSM. For a production system, would just use the raw WAL
  and not commit to LSM / perform compaction.
- Async Master -> Slave replication using `RocksDB.getUpdatesSince()`  
  (Planned for but not implemented)

## ❯ Installation
```shell
# need rocksdb shared library to generate JNI
$ brew install rocksdb

# build project and run tests
$ maven clean package

# populate test data
$ java -jar target/feeds-1.0-SNAPSHOT.jar populate config.yml

# run server
$ java -jar target/feeds-1.0-SNAPSHOT.jar server config.yml

# dump database to console for inspection (server shouldn't be running)
$ java -jar target/feeds-1.0-SNAPSHOT.jar dump config.yml
```

## ❯ API
```bash
# List all feeds (doesn't list contents)
GET /v1/feeds

# Show individual feed
GET /v1/feeds/{feedId} ? startId = {articleId}

# Create new feed
#
# Automatically assign an id and returns new feed
POST /v1/feeds

# Publish an article to feed
#
# Plaintext POST contents are inserted as the article
# text and an id auto-generated and assigned
POST /v1/feeds/{feedId}/publishArticle

# List all users
GET /v1/users

# Show individual user
GET /v1/users/{userId}

# Create new user
#
# Automatically assigns an id and returns new user
POST /v1/users

# Subscribe to feed
#
# This would have been better implemented with an HTTP PATCH,
# but at least guarantees that subscribe / unsubscribe are idempotent
#
# This operation records the current latest {articleId} from {feedId}
# and when consuming all feeds for user will start reading each feed
# from the recorded {articleId}. This mapping is only recorded when the user
# subscribes to a new feed, not when they consume more events, for which
# they have to maintain state on their side.
POST /v1/users/{userId}/subscribe ? feedId = {feedId}

# Unsubscribe from feed
#
# This would have been better implemented with an HTTP PATCH,
# but at least guarantees that subscribe / unsubscribe are idempotent
POST /v1/users/{userId}/unsubscribe ? feedId = {feedId}

# Consume subscribed feeds for a user
#
# If no POST body then gives 1 page (50 items) from each feed starting
# from the recorded {articleId} when the feed was subscribed.
#
# If a mapping of {feedId} -> {articleId} is provided, then for each named
# feed returns 1 page (50 items) starting from given {articleId} and for the
# rest of the subscribed feeds 1 page of articles from initially recorded
# {articleId}
POST /v1/users/{userId}/consumeFeeds

REQUEST BODY
{
  "{feedId}": "{articleId}",
  ...
}

RESPONSE BODY
[
  {
    "id": "{feedId}",
    "articles": List<Article>
  },
  ...
]
```

## ❯ Reading Guide
- `FeedsDB.java` is the wrapper around `RocksDB` and provides
  - `getCounter` and `incrementCounter (atomic)`
  - `get` and `put` for a key
  - `scan` for a named prefix with ability to start at any position  
    in that range. It returns an iterator wrapping `RocksIterator`
    that stops consumer from crossing prefix boundaries.
- `FeedResource.java` contains all `Feed` operations
- `UserResource.java` contains all `User` operations
- Tests for each class
  - `FeedsDBTest.java`
  - `FeedResourceTest.java`
  - `UserResourceTest.java`
