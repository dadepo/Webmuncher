krwkrw is a tool that can be used to easily retrieve all the contents of a website. More accurately contents under a 
single domain. This is its perfect use case which reflects the original need for which it was written: Read about more
on that [here] (http://geekabyte.blogspot.be/2014/12/a-web-scrapercrawler-in-java-krwkrw.html)

###How to use krwkrw.

`krwkrw` is designed around the [Strategy Pattern] (http://en.wikipedia.org/wiki/Strategy_pattern). The main object that
would be used is the `krwkrw` object, while the client using `krwkrw` would need to provide an implementation of the
`krwlerAction` interface which contains code that operates on every fetched page represented by the `FetchedPage` object

The `krwlerAction` interface has only one method that needs to be implemented. The `execute()` method. The `execute()`
method is injected with a `FetchedPage` which contains the information extracted from every crawled pages. e.g, the HTML
content of the page, the uri of the page, the title of the page, the time it took `krwkrw` to retrieve the page etc.

`krwkrw` is available via Maven central, and you can easily drop it into your project with this coordinates

```xml
<dependency>
<groupid>com.blogspot.geekabyte.krwkrw</groupid>
<artifactid>krwler</artifactid>
<version>${krwkrw.version}</version>
</dependency>
```

Or you can also build from source and have the built jar in your classpath.

What a JPA backed `krwlerAction` implementation may look like:

```
class JpaAction implements krwlerAction {

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
```

Or a pure JDBC implementation

```
class JdbcAction implements krwlerAction {
    public static final String JDBC_CONN_STRING = 
    "jdbc:mysql://localhost/pages?user=root";
    
    private Connection connect = null;
    private PreparedStatement preparedStatement = null;
    
    @Override
    public void execute(FetchedPage page) {
        try {
            Class.forName("com.mysql.jdbc.Driver");

            connect = DriverManager.getConnection(JDBC_CONN_STRING);

            preparedStatement = connect
                    .prepareStatement("insert into pages.page values 
                    (default, ?, ?, ?, ? , ?, ?)");


            preparedStatement.setString(1, page.getUrl());
            preparedStatement.setString(2, page.getTitle());
            preparedStatement.setString(3, page.getHtml());
            preparedStatement.setString(4, page.getSourceUrl());
            preparedStatement.setLong(5, page.getLoadTime());
            preparedStatement.setInt(6, page.getStatus());
            preparedStatement.executeUpdate();

        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
}
```

Once you have the `krwlerAction` implemented, you can proceed and use it with an instance of `krwkrw` There are two
call mode supported. A blocking synchronous call, and a non blocking asynchronous call.

Using `krwkrw` in synchronous call may look like this:
 
```
// initiates a jdbc action
JdbcAction krwlerAction = new JdbcAction();
// creates an instance of krwkrw with an implementation of krwlerAction
krwkrw krwkrw = new krwkrw(krwlerAction);
        
public Set<String> fetchPage(krwkaw krwkrw) 
				   throws IOException, InterruptedException {
                
        // gets all the pages from www.example.com
        return krwkrw.dokrwl("http://www.example.com");
}
```

Using `krwkrw` in asynchronous call may look like this:
 
```
// initiates a jdbc action
JdbcAction krwlerAction = new JdbcAction();
// creates an instance of krwkrw with an implementation of krwlerAction
krwkrw krwkrw = new krwkrw(krwlerAction);

public Future<Set<String>> doCrawAsync(krwkaw krwkrw) 
                           throws IOException, InterruptedException {
        // Initialize krwkrw for Asynchronous call
        //destroyAsync() should be called when Future is resolved
        krwkrw.initializeAsync()
        
        return krwkrw.dokrwlAsync("http://www.example.com");
}
```
###Brief Overview of krwkrw API.

| Modifier and Type  | Method and Description |
| ------------- | ------------- |
| void  | destroyAsync() Cleans up after Async call has been finished Should ideally be called after dokrwlAsync(String) or dokrwlAsync(String, java.util.Set)  |
| Set<String>  | dokrwl(String url) Recursively Extracts all href starting from a given url The method is blocking. |
| Set<String>  | dokrwl(String url, Set<String> excludeURLs) Recursively Extracts all href starting from a given url The method is blocking. |
| Future<Set<String>>  | dokrwlAsync(String url) Recursively Extracts all href starting from a given url The method is non blocking as extraction operation is called in another thread. |
| Future<Set<String>> | dokrwlAsync(String url) Recursively Extracts all href starting from a given url The method is non blocking as extraction operation is called in another thread. |
| Future<Set<String>> | dokrwlAsync(String url, Set<String> excludeURLs) Recursively Extracts all href starting from a given url The method is non blocking as extraction operation is called in another thread. |
| int | getDelay() Gets the set delay between krwkrw request. |
| List<String> | getReferrals() Returns the referrals that has been set. |
| List<String> | getUserAgents() Returns the user agents that has been set. |
| void | initializeAsync() Sets up for crawling in Async |
| void | setDelay(int delay) Sets the delay |
| void | setMaxRetry(int maxRetry) The number of tries for failed request due to time outs |
| void | setReferrals(List<String> referrals) Sets a list of referrals that would be used for crawling a page. |
| void | setUserAgents(List<String> userAgents) Sets a list of user agents that would be used for crawling a page. |

For more documentation, the accompanying Javadoc should be helpful. It can be gotten using the
[Javadoc tool] (http://www.oracle.com/technetwork/articles/java/index-jsp-135444.html) or via Maven using the
[Maven Javadoc plugin] (http://maven.apache.org/plugins/maven-javadoc-plugin/).

Thanks to Javadoc.io, you can also access the most recent Javadoc [online](http://www.javadoc.io/doc/com.blogspot.geekabyte.krwkrw/krwler/)

###Change log
0.1.1

1. Fix multiple crawl of 404 links https://github.com/dadepo/krwkrw/issues/1
2. Some JavaDoc enhancement https://github.com/dadepo/krwkrw/issues/2