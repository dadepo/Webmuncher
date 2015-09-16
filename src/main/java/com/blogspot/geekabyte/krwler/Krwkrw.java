package com.blogspot.geekabyte.krwler;

import com.blogspot.geekabyte.krwler.interfaces.KrwlerAction;
import com.blogspot.geekabyte.krwler.interfaces.callbacks.KrwlerExitCallback;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
    private KrwlerExitCallback krwlerExitCallback;
    private Set<String> excludeURLs = new HashSet<>();

    private Set<Pattern> includePattern = new LinkedHashSet<>();
    private Set<Pattern> excludePattern = new LinkedHashSet<>();
    private RandomDelay randomDelay;


    /**
     * Sets the regex patterns a URL should match against in other to be crawled
     * The patterns are processed based on order of insertion. The first pattern inserted
     * will be processed first, the last inserted pattern will be processed last. It is
     * thus advised to provide the patterns in a LinkedHashSet
     *
     * The first pattern to match fulfills the match requirement, which means specific patterns
     * should be included first.
     *
     * @param includePattern the patterns as a set of regex Strings
     */
    public void match(Set<String> includePattern) {
        includePattern.forEach(pattern -> {
            Pattern compiledRegex = Pattern.compile(pattern);
            this.includePattern.add(compiledRegex);
        });
    }


    /**
     * Sets the regex patterns a URL should match against in other to be crawled
     * The patterns are processed based on order of insertion. The first pattern inserted
     * will be processed first, the last inserted pattern will be processed last. It is
     * thus advised to provide the patterns in a LinkedHashSet
     *
     * The first pattern to match fulfills the match requirement, which means specific patterns
     * should be included first.
     *
     * @param includePattern the patterns as a comma separated list of regex strings
     */
    public void match(String... includePattern) {
        Stream.of(includePattern).forEach(pattern -> {
            Pattern compiledRegex = Pattern.compile(pattern);
            this.includePattern.add(compiledRegex);
        });
    }


    /**
     * Sets the regex patterns a URL should match against in other to be excluded from being crawled
     * The patterns are processed based on order of insertion. The first pattern inserted
     * will be processed first, the last inserted pattern will be processed last.
     *
     * The first pattern to match fulfills the skip requirement, which means specific patterns
     * should be included first.
     *
     * @param excludePattern the patterns as a set of regex Strings
     */
    public void skip(Set<String> excludePattern) {
        excludePattern.forEach(pattern -> {
            Pattern compiledRegex = Pattern.compile(pattern);
            this.excludePattern.add(compiledRegex);
        });
    }

    /**
     * Sets the regex patterns a URL should match against in other to be excluded from being crawled
     * The patterns are processed based on order of insertion. The first pattern inserted
     * will be processed first, the last inserted pattern will be processed last.
     *
     * The first pattern to match fulfills the skip requirement, which means specific patterns
     * should be included first.
     *
     * @param excludePattern the patterns as a comma separated list of regex strings
     */
    public void skip(String... excludePattern) {
        Stream.of(excludePattern).forEach(pattern -> {
            Pattern compiledRegex = Pattern.compile(pattern);
            this.excludePattern.add(compiledRegex);
        });
    }

    /**
     * Gets the URLs to be excluded
     *
     * @return the URLs to be excluded
     */
    public Set<String> getExcludeURLs() {
        return excludeURLs;
    }

    /**
     * Sets the URLs to be excluded
     *
     * @param excludeURLs the url to be excluded passed in as set of strings
     */
    public void setExcludeURLs(Set<String> excludeURLs) {
        this.excludeURLs = excludeURLs;
    }

    /**
     * Sets the URLs to be excluded
     *
     * @param excludeURLs the urls to be excluded passed in as string varargs
     */
    public void setExcludeURLs(String... excludeURLs) {
        Stream.of(excludeURLs).forEach(url -> {
            this.excludeURLs.add(url);
        });
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
     *
     * @param delay delay between each crawling requests
     */
    public void setDelay(int delay) {
        this.delay = delay;
    }

    /**
     * Set's the lower and upper bound for the delay (in seconds) between each crawling requests
     *
     * The required delay is random generated with each requests. The generated random delay will be a number
     * equals or greater than the minDelay but less or equals to the maxDelay
     *
     * @param minDelay the lower bound of delay in seconds
     * @param maxDelay the upper bound of delay in seconds
     */
    public void setDelay(int minDelay, int maxDelay) {
        this.randomDelay = new RandomDelay(minDelay, maxDelay);
    }

    /**
     * The number of times to retry failed request due to time outs. The default is 0, meaning no retries.
     *
     * @param maxRetry the number of max try
     */
    public void setMaxRetry(int maxRetry) {
        this.maxRetry = maxRetry;
    }

    /**
     * Returns the user agents that has been set
     *
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
     * @param userAgents the user agents to use for the crawl requests
     */
    public void setUserAgents(List<String> userAgents) {
        this.userAgents = userAgents;
    }

    /**
     * Returns the referrals that has been set
     *
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
     *
     * @throws java.io.IOException            if any.
     * @throws java.lang.InterruptedException if any.
     * @throws java.net.URISyntaxException    if any.
     */
    private Set<String> doCrawl(String url, Set<String> excludeURLs)
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
     *
     * @throws java.io.IOException            if any.
     * @throws java.lang.InterruptedException if any.
     * @throws java.net.URISyntaxException    if any.
     */
    public Set<String> crawl(String url) throws IOException, InterruptedException, URISyntaxException {
        setBaseUrl(url);
        return doCrawl(url, this.excludeURLs);
    }


    private boolean include(String url) {
        boolean include = shouldInclude(url);
        if (include == true) {
            // although it is in include, check if this is not overridden by being in exclude
            include = !shouldExclude(url);
        }

        return include;
    }

    private boolean shouldExclude(String url) {
        if (excludePattern.isEmpty()) {
            return false;
        }


        boolean shouldExclude;

        shouldExclude = excludePattern.stream().anyMatch(pattern -> {
            Matcher matcher = pattern.matcher(url);
            return matcher.matches();
        });
        return shouldExclude;
    }

    private boolean shouldInclude(String url) {
        if (includePattern.isEmpty()) {
            return true;
        }

        boolean shouldInclude;
        shouldInclude = includePattern.stream().anyMatch(pattern -> {
            Matcher matcher = pattern.matcher(url);
            return matcher.matches();
        });
        return shouldInclude;
    }

    /**
     * Registers callback on krwlerExitCallback
     *
     * @param krwlerExitCallbackCallBack the call back to fire when crawler finishes and exits
     */
    public void onExit(KrwlerExitCallback krwlerExitCallbackCallBack) {
        krwlerExitCallback = krwlerExitCallbackCallBack;
    }

    /**
     * Recursively Extracts all href starting from a given url
     * The method is non blocking as extraction operation is called
     * in another thread
     *
     * @param url         a {@link java.lang.String} object.
     * @param excludeURLs a {@link java.util.Set} object.
     * @return {@link java.util.concurrent.Future} of a set of urls
     *
     * @throws java.io.IOException            if any.
     * @throws java.lang.InterruptedException if any.
     * @throws java.net.URISyntaxException    if any.
     */
    private Future<Set<String>> doCrawlAsync(String url, Set<String> excludeURLs)
            throws IOException, InterruptedException, URISyntaxException {
        setBaseUrl(url);
        assert action != null;

        executorService = Executors.newSingleThreadExecutor();

        Future<Set<String>> future = executorService.submit(
                () -> extractor(url, excludeURLs, new HashSet<>(), "")
        );

        return future;
    }

    /**
     * Recursively Extracts all href starting from a given url
     * The method is non blocking as extraction operation is called
     * in another thread
     *
     * @param url a {@link java.lang.String} object.
     * @return {@link java.util.concurrent.Future} of a set of urls
     *
     * @throws java.io.IOException            if any.
     * @throws java.lang.InterruptedException if any.
     * @throws java.net.URISyntaxException    if any.
     */
    public Future<Set<String>> crawlAsync(String url) throws IOException, InterruptedException, URISyntaxException {
        setBaseUrl(url);
        return doCrawlAsync(url, this.excludeURLs);
    }

    /**
     * Gets the {@link org.jsoup.nodes.Document} from a given URL
     *
     * @param url the URL to crawl
     * @return {@link org.jsoup.nodes.Document}
     *
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

            // If url does not match include, then add to urls to be excluded.
            if (!include(url)) {
                excludeURLs.add(url);
            }

            FetchedPage fetchedPage = new FetchedPage();
            try {
                // Extract HTML from URL passed in
                Long before = new Date().getTime();
                doc = getDocumentFromUrl(url);
                Long after = new Date().getTime();
                Long loadTime = after - before;

                // Get all the href in the retrieved page
                hrefs = new HashSet<>(extractAbsHref(doc));

                Set<String> toExclude = hrefs.stream()
                                             .filter(href -> !include(href)) // filter href that shouldn't be include
                                             .collect(Collectors.toSet()); // collect the href to a set

                if (!toExclude.isEmpty()) {
                    excludeURLs.addAll(toExclude);
                }

                // perform action on fetchedPage
                // Only do so if the url matches url to be included
                // for processing
                if (include(url)) {

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

                    action.execute(fetchedPage);
                    logger.info("Crawled {}", url);
                } else {
                    logger.info("Crawled {} but excluding from processing", url);
                }

                if (randomDelay != null) {
                    delay = randomDelay.getDelay();
                }

                logger.info("{} seconds delay before next request", delay);
                Thread.sleep(delay * 1000);
            } catch (IOException e) {
                if (e instanceof UnsupportedMimeTypeException) {
                    fetchedPage.setStatus(415);
                    crawledURLs.add(url);
                } else if (e instanceof SocketTimeoutException) {
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
        this.baseUrl = uri.getHost();
    }

    /**
     * Cleans up after Async call has been finished
     * Should ideally be called after {@link #crawlAsync(String)} or {@link #doCrawlAsync(String, java.util.Set)}
     */
    private void destroyAsync() {
        if (executorService != null) {
            executorService.shutdown();
        }
    }

    private void fireOnExit(Set<String> urls) {
        if (krwlerExitCallback != null) {
            krwlerExitCallback.callBack(urls);
        }
    }

    private boolean lastExtractorCall() {
        StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
        Stream<StackTraceElement> stream = Arrays.stream(stackTraceElements);
        long count = stream.filter(t -> "extractor".equals(t.getMethodName())).count();
        return count == 1;
    }


    /**
     * Class used to generate delay in seconds between (and could include)
     * a lower and upper bound
     */
    private class RandomDelay {

        private int min;
        private int max;
        private Random rand;

        public RandomDelay(int min, int max) {
            this.min = min;
            this.max = max;

            rand = new Random();
        }

        public int getDelay() {
            return rand.nextInt(max-min  + 1) + min;
        }
    }
}
