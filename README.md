A little something. Placed on the interweb. It crawls it.

###How to use Krawkraw.

Grab the `Krawkraw` package and have it in your classpath. `*Krawkraw*` is the main class, 
`*KrawlerAction*` is the interface you need to have implemented that contains code that deals with the pages crawled 
which would be an instance of `*FetchedPage*`.

What a JPA backed KrawlerAction implementation may look like:

```
class Action implements KrawlerAction {

        private EntityManager em;
        private EntityManagerFactory emf;

        /**
         * Operates on given {@link com.blogspot.geekabyte.krawkraw.FetchedPage}
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

Using KrawKraw in synchronous call may look like this:
 
```
...
    public Set<String> doCraw(Krawkaw krawkraw) throws IOException, InterruptedException {

        krawkraw.setAction(new Action()); // sets the action handling fetched pages
        krawkraw.setBaseUrl("example.com"); // base url of the destination to fetch
        krawkraw.setDelay(3000); // Delay between subsequent request. 0 for no delay. 1000 is default
        
        // Sets pages to be skipped
        Set<String> excludedUrl = new HashSet<>();
        excludedUrl.add("http://blog.example.com");
        excludedUrl.add("http://info.example.com");
        
        return krawkraw.doKrawl("http://www.example.com", excludedUrl);
    }
...

```

Using KrawKraw in Asynchronous call may look like this:
 
```
...

    public Future<Set<String>> doCraw(Krawkaw krawkraw) throws IOException, InterruptedException {

        krawkraw.setAction(new Action()); // sets the action handling fetched pages
        krawkraw.setBaseUrl("example.com"); // base url of the destination to fetch
        krawkraw.setDelay(3000); // Delay between subsequent request. 0 for no delay. 1000 is default
        
        // Initialize KrawKraw for Asynchronous call
        krawkraw.initializeAsync() //destroyAsync() should be called when Future is resolved
        
        
        // Sets pages to be skipped
        Set<String> excludedUrl = new HashSet<>();
        excludedUrl.add("http://blog.example.com");
        excludedUrl.add("http://info.example.com");
        
        return krawkraw.doKrawlAsync("http://www.example.com", excludedUrl);
    }
...

```
  
