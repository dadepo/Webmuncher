`krwkrw (formally Krawkraw)` is a tool that can be used to easily retrieve all the contents of a website. More
accurately, contents under a single domain. This is the perfect use case which reflects the original need for
which it was created: Read about more on that [here] (http://geekabyte.blogspot.be/2014/12/a-web-scrapercrawler-in-java-krwkrw.html)

`krwkrw` is available via Maven central, and you can easily drop it into your project with this coordinates:

Maven:

```xml
<dependency>
<groupid>com.blogspot.geekabyte.krwkrw</groupid>
<artifactid>krwler</artifactid>
<version>${krwkrw.version}</version>
</dependency>
```
Gradle:

```groovy
dependencies {
    compile "com.blogspot.geekabyte.krwkrw:krwler:$krwkrw.version}"
}
```
Or you can also build from source and have the built jar in your classpath.

The available releases can be seen [here] (https://github.com/dadepo/Krwkrw/releases)

###How to use krwkrw.

`krwkrw` is designed around the [Strategy Pattern] (http://en.wikipedia.org/wiki/Strategy_pattern). The main object that
would be used is the `krwkrw` object, while the client using `krwkrw` would need to provide an implementation of the
`krwlerAction` interface which contains code that operates on every fetched page represented by the `FetchedPage` object

The `krwlerAction` interface has only one method that needs to be implemented. The `execute()` method. The `execute()`
method is given a `FetchedPage` object which contains the information extracted from every crawled pages. e.g, the HTML
content of the page, the uri of the page, the title of the page, the time it took `krwkrw` to retrieve the page etc.

Since _version 0.1.2_ `Krwkrw` comes with utility `KrwlerActions`, that makes it easy to persist pages crawled.
The included utility actions are:

1. *JDBCAction* - for persisting web pages into a relational database. _(since 0.1.2)_
2. *ElasticSearchAction* - for indexing web pages into ElasticSearch. _(since 0.1.2)_
3. *CSVAction* - for saving web pages into a CSV file. _(since 0.1.2)_

For example to use `Krwkrw` to extract all the contents of `http://www.example.com` into a CSV file, you do:

```java

    CSVAction action = CSVAction.builder()
                .convertToPlainText(true) // converts HTML to plain text
                .setDestination(Paths.get("example-com.csv"))
                .buildAction();

        Krwkrw crawler = new Krwkrw(action); // creates an instance of the crawler with the action

        // Configure the crawler to your hearts desire
        crawler.setDelay(20); // Crawler will wait 20 seconds between each requests
        crawler.setMaxRetry(3) // When at first you don't succeed? Give up and move onto the next one, after 3 attempts!

        // the crawler would select randomly from the list of user agents you give for each request
        crawler.setUserAgents(Arrays.asList(
                                "Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6",
                                "Opera/9.80 (X11; Linux i686; Ubuntu/14.10) Presto/2.12.388 Version/12.16")
                        );

        // Provide the list of addresses to use for the referral. So the folks at example.com when checking
        // the webserver logs:sometimes the request comes from google, sometimes, yahoo, sometimes bing...
        crawler.setReferrals(Arrays.asList("http://www.google.com","http://www.yahoo.com", "http://www.bing.com"));

        // Start the crawling operation as a blocking call.
        Set<String> strings = crawler.doKrawl("http://www.example.com");

        // If you want to execute the crawling in another thread, so the current thread does not block, then do:
        Set<String> strings = crawler.doKrawlAsync("http://www.example.com");

        // in case you do the crawling in another thread, you most likely want to be notified when the crawling operations
        // terminates. in such a case, you should use crawler.onExit(KrwlerExitCallback callback) to register the callback
```

The above steps makes use of the `CSVAction` that comes with the library. In case you have custom operations you want
applied to the fetched web pages, then you can easily implement your own `KrwlerAction`. for example a JPA backed
 `krwlerAction` implementation may look like:


```java
class CustomJpaAction implements krwlerAction {

        private EntityManager em;
        private EntityManagerFactory emf;

        /**
         * Operates on given {@link com.blogspot.geekabyte.krwkrw.FetchedPage}
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


###Overview of krwkrw API.

The accompanying Javadoc should be helpful in having an overview of the API. It can be gotten using the
[Javadoc tool] (http://www.oracle.com/technetwork/articles/java/index-jsp-135444.html) or via Maven using the
[Maven Javadoc plugin] (http://maven.apache.org/plugins/maven-javadoc-plugin/).

More conveniently, thanks to [Javadoc.io](http://www.javadoc.io), you can also access the most recent Javadoc [online](http://www.javadoc.io/doc/com.blogspot.geekabyte.krwkrw/krwler/)

The API for the older version: (Krakraw) can be find online [here](http://www.javadoc.io/doc/com.blogspot.geekabyte.krawkraw/krawler/)

### Licenses
[The MIT License (MIT)] (http://www.opensource.org/licenses/mit-license.php)
