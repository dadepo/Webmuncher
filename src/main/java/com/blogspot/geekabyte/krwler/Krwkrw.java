package com.blogspot.geekabyte.krwler;

import com.blogspot.geekabyte.krwler.interfaces.KrwlerAction;
import com.blogspot.geekabyte.krwler.interfaces.callbacks.KrawlerExitCallback;
import org.jsoup.HttpStatusException;
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
import java.util.Arrays;
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
import java.util.stream.Stream;

/**
 * <p>Krwkrw class.</p>
 *
 * @author Dadepo Aderemi.
 */
public class Krwkrw {

    Logger logger = LoggerFactory.getLogger(Krwkrw.class);

    private String baseUrl;
    private ExecutorService executorService;
    private Map<String, Integer> retryLog = new HashMap<>();
    private int delay = 1;
    private int maxRetry = 0;
    private List<String> userAgents = new ArrayList<>();
    private List<String> referrals = new ArrayList<>();
    private KrwlerAction action;
    private KrawlerExitCallback krawlerExitCallback;
    private Set<String> excludeURLs = new HashSet<>();


    /**
     * Gets the URLs to be excluded
     * @return the URLs to be excluded
     */
    public Set<String> getExcludeURLs() {
        return excludeURLs;
    }

    /**
     * Sets the URLs to be excluded
     * @param excludeURLs the url to be excluded
     */
    public void setExcludeURLs(Set<String> excludeURLs) {
        this.excludeURLs = excludeURLs;
    }

    /**
     * Gets the set delay between each requests
     *  
     * @return delay: the set delay (in seconds), between each crawling requests
     */
    public int getDelay() {
        return delay;
    }

    /**
     * Sets the delay (in seconds) between each crawling requests. The default is 1 i.e. 1 second.
     * @param delay delay between each crawling requests
     */
    public void setDelay(int delay) {
        this.delay = delay;
    }
    
    /**
     * The number of times to retry failed request due to time outs. The default is 0, meaning no retries.
     * @param maxRetry the number of max try
     */
    public void setMaxRetry(int maxRetry) {
        this.maxRetry = maxRetry;
    }
    
    /**
     * Returns the user agents that has been set
     * @return the user agents
     */
    public List<String> getUserAgents() {
        return userAgents;
    }

    /**
     * Sets a list of user agents that would be used for crawling a page. One of the given
     * user agents would be selected randomly for each page request. The default is
     * Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
     *
     * @param userAgents
     */
    public void setUserAgents(List<String> userAgents) {
        this.userAgents = userAgents;
    }
    
    /**
     * Returns the referrals that has been set
     * @return the referrals
     */
    public List<String> getReferrals() {
        return referrals;
    }

    /**
     * Sets a list of referrals that would be used for crawling a page. One of the given
     * referrals would be selected randomly for each page request. The default is www.google.com
     *  
     * @param referrals a list of referrals
     */
    public void setReferrals(List<String> referrals) {
        this.referrals = referrals;
    }

    /**
     * Public constructor for {@link Krwkrw}
     * takes an instance of {@link KrwlerAction} which
     * is used to operate on a fetched url
     * represented by {@link com.blogspot.geekabyte.krwler.FetchedPage}
     *
     * @param krwlerAction a {@link KrwlerAction} object.
     */
    public Krwkrw(KrwlerAction krwlerAction) {
        action = krwlerAction;
    }

    /**
     * Recursively Extracts all href starting from a given url
     * The method is blocking. Only returns when all url has been fetched
     *
     * @param url         the URL to start extracting from
     * @param excludeURLs a set that contains already crawled URLs. You can include URLS you want omitted
     * @return A set containing all the URL crawled
     * @throws java.io.IOException if any.
     * @throws java.lang.InterruptedException if any.
     * @throws java.net.URISyntaxException if any.
     */
    private Set<String> doKrawl(String url, Set<String> excludeURLs)
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
     * @throws java.io.IOException if any.
     * @throws java.lang.InterruptedException if any.
     * @throws java.net.URISyntaxException if any.
     */
    public Set<String> doKrawl(String url) throws IOException, InterruptedException, URISyntaxException {
        setBaseUrl(url);
        return doKrawl(url, this.excludeURLs);
    }


    /**
     * Registers callback on krawlerExitCallback
     * 
     * @param krawlerExitCallbackCallBack the call back to fire when crawler finishes and exits
     */
    public void onExit(KrawlerExitCallback krawlerExitCallbackCallBack) {
        krawlerExitCallback = krawlerExitCallbackCallBack;
    }

    /**
     * Recursively Extracts all href starting from a given url
     * The method is non blocking as extraction operation is called
     * in another thread
     *
     * @param url a {@link java.lang.String} object.
     * @param excludeURLs a {@link java.util.Set} object.
     * @throws java.io.IOException if any.
     * @throws java.lang.InterruptedException if any.
     * @throws java.net.URISyntaxException if any.
     * @return {@link java.util.concurrent.Future} of a set of urls
     */
    private Future<Set<String>> doKrawlAsync(String url, Set<String> excludeURLs)
            throws IOException, InterruptedException, URISyntaxException {
        setBaseUrl(url);
        assert action != null;

        executorService = Executors.newSingleThreadExecutor();

        Future<Set<String>> future = executorService.submit(new Callable<Set<String>>() {
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
     * @param url a {@link java.lang.String} object.
     * @throws java.io.IOException if any.
     * @throws java.lang.InterruptedException if any.
     * @throws java.net.URISyntaxException if any.
     * @return {@link java.util.concurrent.Future} of a set of urls
     */
    public Future<Set<String>> doKrawlAsync(String url) throws IOException, InterruptedException, URISyntaxException {
        setBaseUrl(url);
        return doKrawlAsync(url, this.excludeURLs);
    }

    /**
     * Gets the {@link org.jsoup.nodes.Document} from a given URL
     *
     * @param url the URL to crawl
     * @return {@link org.jsoup.nodes.Document}
     * @throws java.io.IOException if any.
     */
    private Document getDocumentFromUrl(String url) throws IOException {
        String userAgent = randomSelectUserAgent();
        String referral = randomSelectReferral();
        Document doc = Jsoup
                .connect(url)
                .userAgent(userAgent)
                .referrer(referral)
                .get();
        logger.info("Fetched {} with User Agent: {} and Referral {}", url, userAgent, referral);
        return doc;
    }

    /**
     * Extracts all href from a {@link org.jsoup.nodes.Document} using absolute resolution
     *
     * @param doc the {@link org.jsoup.nodes.Document} to extrach hrefs from
     * @return list of {@link org.jsoup.nodes.Document}
     */
    private List<String> extractAbsHref(Document doc) {
        List<String> hrefString = new ArrayList<String>();
        Element content = doc.body();
        Elements links = content.getElementsByTag("a");
        for (Element link : links) {
            String href = link.attr("abs:href");
            hrefString.add(href);
        }
        return hrefString;
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
                fetchedPage.setPlainText(Jsoup.parse(doc.outerHtml()).text());
                fetchedPage.setTitle(doc.title());
                fetchedPage.setLoadTime(loadTime);
                fetchedPage.setSourceUrl(sourceUrl);

                // perform action on fetchedPage
                action.execute(fetchedPage);
                logger.info("Crawled {}", url);
                Thread.sleep(delay*1000);
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
                        fetchedPage.setStatus(408);
                        crawledURLs.add(url);
                    }

                } else {
                    fetchedPage.setStatus(404);
                    crawledURLs.add(url);
                }
                fetchedPage.setUrl(url);
                fetchedPage.setSourceUrl(sourceUrl);
                action.execute(fetchedPage);
                logger.error("Failed to crawl {}. With error message: {}", url, e.getLocalizedMessage());
            }
        } else {
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

            if (lastExtractorCall()) {
                destroyAsync();
                fireOnExit(crawledURLs);
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

    private String randomSelectReferral() {
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

    /**
     * Cleans up after Async call has been finished
     * Should ideally be called after {@link #doKrawlAsync(String)} or {@link #doKrawlAsync(String, java.util.Set)}
     */
    private void destroyAsync() {
        if (executorService != null) {
            executorService.shutdown();
        }
    }
    
    private void fireOnExit(Set<String> urls) {
        if (krawlerExitCallback != null) {
            krawlerExitCallback.callBack(urls);
        }
    }
    
    private boolean lastExtractorCall() {
        StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
        Stream<StackTraceElement> stream = Arrays.stream(stackTraceElements);
        long count = stream.filter(t -> "extractor".equals(t.getMethodName())).count();
        return count == 1;
    }
}
