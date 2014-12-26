package com.blogspot.geekabyte.krawkraw;

import com.blogspot.geekabyte.krawkraw.interfaces.KrawlerAction;
import lombok.Getter;
import lombok.Setter;
import org.jsoup.Jsoup;
import org.jsoup.UnsupportedMimeTypeException;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

/**
 * @author Dadepo Aderemi.
 */
public class Krawkraw {

    Logger logger = LoggerFactory.getLogger(Krawkraw.class);

    private final String defaultCharset = "UTF-8";
    private String baseUrl;
    private ExecutorService executorService;
    private Map<String, Integer> retryLog = new HashMap<>();

    /**
     * The delay between each krawkraw requests
     * --SETTER--
     * Sets the delay
     *
     * @param delay delay in milliseconds
     * <p/>
     * --GETTER--
     * Gets the set delay between krawkraw requests
     * @return delay the set delay in milliseconds
     */
    @Setter @Getter
    private int delay = 1000;

    /**
     * The number of tries for failed request due to time outs
     * --SETTER--
     * Sets the max retry attempt. Default is 0
     *
     * @param the number of max try
     */
    @Setter
    private int maxRetry = 0;

    private KrawlerAction action;

    /**
     * Sets a list of user agents that would be used for crawling a page. One of the given
     * user agents would be selected randomly for each page request. The default is
     * Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
     * <p/>
     * --SETTER--
     * Sets the user agents
     *
     * @param userAgents a list of user agents
     * <p/>
     * --GETTER--
     * Returns the user agents that has been set
     * @Return the user agents
     */
    @Setter @Getter
    private List<String> userAgents = new ArrayList<>();

    /**
     * Sets a list of referrals that would be used for crawling a page. One of the given
     * referrals would be selected randomly for each page request. The default is www.google.com
     * <p/>
     * --SETTER--
     * Sets the referrals
     *
     * @param referrals a list of referrals
     * <p/>
     * --GETTER--
     * Returns the referrals that has been set
     * @Return the referrals
     */
    @Setter @Getter
    private List<String> referrals = new ArrayList<>();

    /**
     * Public constructor for {@link com.blogspot.geekabyte.krawkraw.Krawkraw}
     * takes an instance of {@link com.blogspot.geekabyte.krawkraw.Krawkraw} which
     * is used to operate on a fetched url
     * represented by {@link com.blogspot.geekabyte.krawkraw.FetchedPage}
     * @param krawlerAction
     */
    public Krawkraw(KrawlerAction krawlerAction) {
        action = krawlerAction;
    }

    /**
     * Gets the {@link org.jsoup.nodes.Document} from a given URL
     *
     * @param url the URL to crawl
     * @return {@link org.jsoup.nodes.Document}
     *
     * @throws IOException
     */
    public Document getDocumentFromUrl(String url) throws IOException {
        String userAgent = randomSelectUserAgent();
        String referral = randomSelectreferral();
        Document doc = Jsoup
                .connect(url)
                .userAgent(userAgent)
                .referrer(referral)
                .get();
        logger.info("Fetched {} with User Agent: {} and Referral {}", url, userAgent, referral);
        return doc;
    }


    /**
     * Extracts all href from a {@link org.jsoup.nodes.Document}
     *
     * @param doc
     * @return list of {@link org.jsoup.nodes.Document}
     */
    public List<String> extractHref(Document doc) {
        List<String> hrefString = new ArrayList<String>();
        Element content = doc.body();
        Elements links = content.getElementsByTag("a");
        for (Element link : links) {
            String href = link.attr("href");
            hrefString.add(href);
        }
        return hrefString;
    }


    /**
     * Extracts all href from a {@link org.jsoup.nodes.Document} using absolute resolution
     *
     * @param doc the {@link org.jsoup.nodes.Document} to extrach hrefs from
     * @return list of {@link org.jsoup.nodes.Document}
     */
    public List<String> extractAbsHref(Document doc) {
        List<String> hrefString = new ArrayList<String>();
        Element content = doc.body();
        Elements links = content.getElementsByTag("a");
        for (Element link : links) {
            String href = link.attr("abs:href");
            hrefString.add(href);
        }
        return hrefString;
    }


    /**
     * Recursively Extracts all href starting from a given url
     * The method is blocking. Only returns when all url has been fetched
     *
     * @param url         the URL to start extracting from
     * @param excludeURLs a set that contains already crawled URLs. You can include URLS you want omitted
     * @return A set containing all the URL crawled
     *
     * @throws IOException
     * @throws InterruptedException
     * @throws URISyntaxException
     */
    public Set<String> doKrawl(String url, Set<String> excludeURLs)
            throws IOException, InterruptedException, URISyntaxException {
        setBaseUrl(url);
        return extractor(url, excludeURLs, new HashSet<String>(), "");
    }

    /**
     * Recursively Extracts all href starting from a given url
     * The method is blocking. Only returns when all url has been fetched
     *
     * @param url the URL to start extracting from
     * @return A set containing all the URL crawled
     * @throws IOException
     * @throws InterruptedException
     * @throws URISyntaxException
     */
    public Set<String> doKrawl(String url) throws IOException, InterruptedException, URISyntaxException {
        setBaseUrl(url);
        return doKrawl(url, new HashSet<>());
    }

    /**
     * Sets up for crawling in Async mode.
     * Should be called before {@link #doKrawlAsync(String)} or {@link #doKrawlAsync(String, java.util.Set)}
     * is called.
     */
    public void initializeAsync() {
        if (executorService == null) {
            executorService = Executors.newSingleThreadExecutor();
        }
    }

    /**
     * Cleans up after Async call has been finished
     * Should ideally be called after {@link #doKrawlAsync(String)} or {@link #doKrawlAsync(String, java.util.Set)}
     */
    public void destroyAsync() {
        if (executorService != null) {
            executorService.shutdown();
        }
    }


    /**
     * Recursively Extracts all href starting from a given url
     * The method is non blocking as extraction operation is called
     * in another thread
     *
     * @param url
     * @param excludeURLs
     * @throws IOException
     * @throws InterruptedException
     * @throws URISyntaxException
     * @return {@link java.util.concurrent.Future} of a set of urls
     */
    public Future<Set<String>> doKrawlAsync(String url, Set<String> excludeURLs)
            throws IOException, InterruptedException, URISyntaxException {
        setBaseUrl(url);
        assert action != null;

        ExecutorService service = Executors.newSingleThreadExecutor();

        Future<Set<String>> future = service.submit(new Callable<Set<String>>() {
            @Override
            public Set<String> call() throws Exception {
                return extractor(url, excludeURLs, new HashSet<String>(), "");
            }
        });

        return future;
    }

    /**
     * Recursively Extracts all href starting from a given url
     * The method is non blocking as extraction operation is called
     * in another thread
     *
     * @param url
     * @throws IOException
     * @throws InterruptedException
     * @throws URISyntaxException
     * @return {@link java.util.concurrent.Future} of a set of urls
     */
    public Future<Set<String>> doKrawlAsync(String url) throws IOException, InterruptedException, URISyntaxException {
        setBaseUrl(url);
        return doKrawlAsync(url, new HashSet<>());
    }

    private Set<String> extractor(String url, Set<String> excludeURLs, Set<String> crawledURLs, String sourceUrl)
            throws IOException, InterruptedException {

        if (excludeURLs == null) {
            excludeURLs = new HashSet<>();
        }

        if (crawledURLs == null) {
            crawledURLs = new HashSet<>();
        } else {
            excludeURLs.addAll(crawledURLs);
        }

        Document doc;
        Set<String> hrefs = new HashSet<>();
        Map<String, Document> out = new HashMap<>();
        // first recursive break condition
        if (!excludeURLs.contains(url)) {
            // create fetechedPage
            FetchedPage fetchedPage = new FetchedPage();
            try {
                // Extract HTML from URL passed in
                Long before = new Date().getTime();
                doc = getDocumentFromUrl(url);
                Long after = new Date().getTime();
                Long loadTime = after - before;

                // Get all the href in the retrieved page
                hrefs = new HashSet<>(extractAbsHref(doc));

                // update processing variables
                // tempresult for keeping processed URL to prevent double processing
                // pagesOut the map<String, Document> that would be the final result
                crawledURLs.add(url);
                out.put(url, doc);

                fetchedPage.setUrl(url);
                fetchedPage.setStatus(200);
                fetchedPage.setHtml(doc.outerHtml());
                fetchedPage.setTitle(doc.title());
                fetchedPage.setLoadTime(loadTime);
                fetchedPage.setSourceUrl(sourceUrl);

                // perform action on fetchedPage
                action.execute(fetchedPage);
                logger.info("Crawled {}", url);
                Thread.sleep(delay);
            } catch (IOException e) {
                if (e instanceof UnsupportedMimeTypeException) {
                    fetchedPage.setStatus(415);
                    crawledURLs.add(url);
                } else if(e instanceof SocketTimeoutException) {
                    Integer attempts = retryLog.get(url);
                    if (attempts == null) {
                        attempts = 0;
                    }
                    if (attempts == 0 && maxRetry != 0) {
                        retryLog.put(url, 1);
                        logger.info("Attempting the first retry to fetch url: {}", url);
                        extractor(url, excludeURLs, crawledURLs, "");
                        return hrefs;
                    } else if (attempts < maxRetry) {
                        retryLog.put(url, ++attempts);
                        logger.info("Attempting to fetch url: {} as {} attempts", url, attempts);
                        extractor(url, excludeURLs, crawledURLs, "");
                        return hrefs;
                    } else {
                        fetchedPage.setStatus(404);
                        crawledURLs.add(url);
                    }

                } else {
                    fetchedPage.setStatus(404);
                    crawledURLs.add(url);
                }
                fetchedPage.setUrl(url);
                fetchedPage.setSourceUrl(sourceUrl);
                action.execute(fetchedPage);
                logger.error("Failed to crawl {}", url, e);
            }
        } else {
            crawledURLs.add(url);
            return hrefs;
        }
        // second recursive break condition
        if (hrefs.size() == 0) {
            return hrefs;
        } else {
            // filter out external url
            hrefs = filterOutParamsGeneratedString(hrefs);
            hrefs = filterOutExternalUrls(hrefs);
            for (String href : hrefs) {
                extractor(href, excludeURLs, crawledURLs, url);
            }
            return crawledURLs;
        }
    }

    private Set<String> filterOutExternalUrls(Set<String> urls) {
        urls = urls.stream()
                   .filter(u -> u.contains(baseUrl))
                   .filter(u -> !u.contains("mailto"))
                   .collect(Collectors.toSet());

        return urls;
    }

    private Set<String> filterOutParamsGeneratedString(Set<String> urls) {
        urls = urls.stream().filter(u -> !u.contains("?C=")).collect(Collectors.toSet());
        return urls;
    }


    private String randomSelectUserAgent() {
        if (userAgents.size() == 0) {
            return "Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6";
        }

        Collections.shuffle(userAgents);
        Random random = new Random();
        int randomIndex = random.nextInt(userAgents.size());
        return userAgents.get(randomIndex);
    }

    private String randomSelectreferral() {
        if (referrals.size() == 0) {
            return "www.google.com";
        }

        Collections.shuffle(referrals);
        Random random = new Random();
        int randomIndex = random.nextInt(referrals.size());
        return referrals.get(randomIndex);
    }

    private void setBaseUrl(String url) throws URISyntaxException {
        URI uri = new URI(url);
        String host = uri.getHost();
        int breaker = 0;

        StringBuilder stringBuilder = new StringBuilder();
        for (int i = host.length(); i > 0; i--) {
            char charAt = host.charAt(i - 1);
            stringBuilder.append(charAt);
            if (charAt == '.') {
                if (++breaker == 2) {
                    stringBuilder.deleteCharAt(stringBuilder.length() - 1);
                    break;
                }
            }
        }

        String baseUrl = stringBuilder.reverse().toString();
        this.baseUrl = baseUrl;
    }
}
