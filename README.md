[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.blogspot.geekabyte.webmuncher/webmuncher/badge.svg)](com.blogspot.geekabyte.webmuncher/webmuncher/)
[![Javadoc](https://javadoc-emblem.rhcloud.com/doc/com.blogspot.geekabyte.webmuncher/webmuncher/badge.svg)](com.blogspot.geekabyte.webmuncher/webmuncher/)

`webmuncher` is a tool that can be used to easily retrieve all the contents of a website. More
accurately, contents under a single domain. This is the perfect use case which reflects the original need for
which it was created: [Read more about that here](http://geekabyte.blogspot.be/2014/12/a-web-scrapercrawler-in-java-krawkraw.html)

`webmuncher` is available via Maven central, and you can easily drop it into your project with this coordinates:

Maven:

```xml
<dependency>
<groupid>com.blogspot.geekabyte.webmuncher</groupid>
<artifactid>webmuncher</artifactid>
<version>${webmuncher.version}</version>
</dependency>
```
Gradle:

```groovy
dependencies {
    compile "com.blogspot.geekabyte.webmuncher:webmuncher:$webmuncher.version}"
}
```
Or you can also build from source and have the built jar in your classpath.

The available releases can be seen [here](https://github.com/dadepo/webmuncher/releases)

The announcement for release of version 0.1.3 can be found [here](http://geekabyte.blogspot.in/2015/09/krwkrw-013-released.html)

###How to use webmuncher.

`webmuncher` is designed around the [Strategy Pattern] (http://en.wikipedia.org/wiki/Strategy_pattern). The main object that
would be used is the `webmuncher` object, while the client using `webmuncher` would need to provide an implementation of the
`FetchAction` interface which contains code that operates on every fetched page represented by the `FetchedPage` object

The `FetchAction` interface has only one method that needs to be implemented. The `execute()` method. The `execute()`
method is given a `FetchedPage` object which contains the information extracted from every crawled pages. e.g, the HTML
content of the page, the uri of the page, the title of the page, the time it took `webmuncher` to retrieve the page etc.

Since _version 0.1.2_ `webmuncher` comes with utility `FetchActions`, that makes it easy to persist pages crawled.
The included utility actions are:

1. *JDBCAction* - for persisting web pages into a relational database. _(since 0.1.2)_
2. *ElasticSearchAction* - for indexing web pages into ElasticSearch. _(since 0.1.2)_
3. *CSVAction* - for saving web pages into a CSV file. _(since 0.1.2)_

For example, to use `webmuncher` to extract all the contents of `http://www.example.com` into a CSV file, you do:

```java

    // Use the builder to build the CSVAction
    CSVAction action = CSVAction.builder()
                .convertToPlainText(true) // converts HTML to plain text
                .setDestination(Paths.get("example-com.csv"))
                .buildAction();

    // creates an instance of the crawler with the action
    webmuncher crawler = new webmuncher(action);

    // Configure the crawler to your hearts desire

    // Crawler will wait 20 seconds between each requests
    crawler.setDelay(20);

    // When at first you don't succeed?
    // Give up and move onto the next one, after 3 attempts!
    crawler.setMaxRetry(3)

    // the crawler would select randomly from the list of user agents
    // you give for each request
    crawler.setUserAgents(Arrays.asList(
      "Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6)...",
      "Opera/9.80 (X11; Linux i686; Ubuntu/14.10)...")
    );

    // Provide the list of addresses to use for the referral. So the folks at
    // example.com when checking the webserver logs:sometimes the request
    // comes from google, sometimes, yahoo, sometimes bing...
    crawler.setReferrals(Arrays.asList(
                    "http://www.google.com",
                    "http://www.yahoo.com",
                    "http://www.bing.com"));

        // Start the crawling operation as a blocking call.
        Set<String> strings = crawler.crawl("http://www.example.com");

        // If you want to execute the crawling in another thread,
        // so the current thread does not block, then do:
        Set<String> strings = crawler.crawlAsync("http://www.example.com");

        // in case you do the crawling in another thread,
        // you most likely want to be notified when the
        // crawling operations terminates. in such a case,
        // you should use crawler.onExit(FetchExitCallback callback)
        // to register the callback
```

The above steps makes use of the `CSVAction` that comes with the library. In case you have custom operations you want
applied to the fetched web pages, then you can easily implement your own `FetchAction`. for example a JPA backed
 `FetchAction` implementation may look like:


```java
class CustomJpaAction implements FetchAction {

        private EntityManager em;
        private EntityManagerFactory emf;

        /**
         * Operates on given {@link com.blogspot.geekabyte.webmuncher.FetchedPage}
         *
         * @param page
         */
        @Override
        public void execute(FetchedPage page) {
            emf = Persistence.createEntityManagerFactory("FetchedPage");
            em = emf.createEntityManager();
            em.getTransaction().begin();

            FetchedPageEntity entity = new FetchedPageEntity();
            entity.setHtml(page.getHtml());
            entity.setLoadTime(page.getLoadTime());
            entity.setStatus(page.getStatus());
            entity.setTitle(page.getTitle());
            entity.setUrl(page.getUrl());
            entity.setSourceUrl(page.getSourceUrl());

            em.persist(entity);
            em.flush();
            em.getTransaction().commit();
        }
}
```

###Overview of webmuncher API.

The accompanying Javadoc should be helpful in having an overview of the API. It can be gotten using the
[Javadoc tool] (http://www.oracle.com/technetwork/articles/java/index-jsp-135444.html) or via Maven using the
[Maven Javadoc plugin] (http://maven.apache.org/plugins/maven-javadoc-plugin/).

More conveniently, thanks to Javadoc.io, you can also access the most recent Javadoc [online](http://www.javadoc.io/doc/com.blogspot.geekabyte.webmuncher/webmuncher/)

### Licenses
[The MIT License (MIT)] (http://www.opensource.org/licenses/mit-license.php)
