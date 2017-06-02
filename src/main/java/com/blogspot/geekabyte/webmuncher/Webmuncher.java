package com.blogspot.geekabyte.webmuncher;

import com.blogspot.geekabyte.webmuncher.interfaces.ErrorAction;
import com.blogspot.geekabyte.webmuncher.interfaces.FetchAction;
import com.blogspot.geekabyte.webmuncher.interfaces.callbacks.FetchExitCallback;
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
 * <p>Webmuncher class.</p>
 *
 * @author Dadepo Aderemi.
 */
public class Webmuncher {

    Logger logger = LoggerFactory.getLogger(Webmuncher.class);

    private String baseUrl;
    private ExecutorService executorService;
    private Map<String, Integer> retryLog = new HashMap<>();
    private int delay = 1;
    private int maxRetry = 0;
    private List<String> userAgents = new ArrayList<>();
    private List<String> referrals = new ArrayList<>();
    private FetchAction action;
    private FetchExitCallback fetchExitCallback;
    private ErrorAction errorAction;
    private Set<String> excludeURLs = new HashSet<>();

    private Set<Pattern> includePattern = new LinkedHashSet<>();
    private Set<Pattern> excludePattern = new LinkedHashSet<>();
    private RandomDelay randomDelay;
    private int timeout = 1000;

    public Webmuncher() {

    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public void setFetchAction(FetchAction fetchAction) {
        this.action = fetchAction;
    }

    public void setErrorAction(ErrorAction errorAction) {
        this.errorAction = errorAction;
    }

    public static class Builder {
        private Webmuncher webmuncher;

        public Builder withFetchAction(FetchAction fetchAction) {
            webmuncher.setFetchAction(fetchAction);
            return this;
        }

        public Builder withMatchPattern(Set<String> includePattern) {
            webmuncher.match(includePattern);
            return this;
        }

        public Builder withExcludePattern(Set<String> excludePattern) {
            webmuncher.skip(excludePattern);
            return this;
        }

        public Builder withExcludeUrl(Set<String> excludeURLs) {
            webmuncher.setExcludeURLs(excludeURLs);
            return this;
        }

        public Builder withDelayInBetweenRequest(int delay) {
            webmuncher.setDelay(delay);
            return this;
        }

        public Builder withRequestTimeOut(int timeOut) {
            webmuncher.setTimeout(timeOut);
            return this;
        }

        public Builder withMaxRetry(int maxRetry) {
            webmuncher.setMaxRetry(maxRetry);
            return this;
        }

        public Builder withUserAgents(List<String> userAgents) {
            webmuncher.setUserAgents(userAgents);
            return this;
        }

        public Builder withReferrals(List<String> referrals) {
            webmuncher.setReferrals(referrals);
            return this;
        }

        public Builder withExitCallBack(FetchExitCallback fetchExitCallback) {
            webmuncher.onExit(fetchExitCallback);
            return this;
        }

        ;

        public Builder withErrorAction(ErrorAction errorAction) {
            webmuncher.setErrorAction(errorAction);
            return this;
        }

        public Webmuncher build() {
            return this.webmuncher;
        }

        private Builder() {
            this.webmuncher = new Webmuncher();
        }
    }

    /**
     * Sets the regex patterns a URL should match against in other to be crawled
     * The patterns are processed based on order of insertion. The first pattern inserted
     * will be processed first, the last inserted pattern will be processed last. It is
     * thus advised to provide the patterns in a LinkedHashSet
     * <p>
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
     * <p>
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
     * <p>
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
     * <p>
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
     * <p>
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
     * Set's the time in seconds for the connection to wait before
     * a {@link SocketTimeoutException} is thrown. The Default is 10 seconds
     *
     * @param timeout the amount of seconds to wait before a timeout
     */
    public void setTimeout(int timeout) {
        this.timeout = timeout * 1000;
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
     * Public constructor for {@link Webmuncher}
     * takes an instance of {@link FetchAction} which
     * is used to operate on a fetched url
     * represented by {@link com.blogspot.geekabyte.webmuncher.FetchedPage}
     *
     * @param webAction a {@link FetchAction} object.
     */
    public Webmuncher(FetchAction webAction) {
        action = webAction;
    }

    /**
     * Recursively Extracts all href starting from a given url
     * The method is blocking. Only returns when all url has been fetched
     *
     * @param url         the URL to start extracting from
     * @param excludeURLs a set that contains already crawled URLs. You can include URLS you want omitted
     * @return A set containing all the URL crawled
     * @throws java.io.IOException            if any.
     * @throws java.lang.InterruptedException if any.
     * @throws java.net.URISyntaxException    if any.
     */
    private Set<String> doCrawl(String url, Set<String> excludeURLs)
            throws IOException, InterruptedException, URISyntaxException {
        setBaseUrl(url);
        return nonRecursiveExtractor(url, excludeURLs);
    }

    /**
     * Recursively Extracts all href starting from a given url
     * The method is blocking. Only returns when all url has been fetched
     *
     * @param url the URL to start extracting from
     * @return A set containing all the URL crawled
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

        return excludePattern.stream().anyMatch(pattern -> {
            Matcher matcher = pattern.matcher(url);
            return matcher.matches();
        });
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
     * Registers callback on fetchExitCallback
     *
     * @param fetchExitCallbackCallBack the call back to fire when crawler finishes and exits
     */
    public void onExit(FetchExitCallback fetchExitCallbackCallBack) {
        fetchExitCallback = fetchExitCallbackCallBack;
    }

    /**
     * Recursively Extracts all href starting from a given url
     * The method is non blocking as extraction operation is called
     * in another thread
     *
     * @param url         a {@link java.lang.String} object.
     * @param excludeURLs a {@link java.util.Set} object.
     * @return {@link java.util.concurrent.Future} of a set of urls
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
                () -> nonRecursiveExtractor(url, excludeURLs)
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
     * @throws java.io.IOException if any.
     */
    private Document getDocumentFromUrl(String url) throws IOException {
        String userAgent = randomSelectUserAgent();
        String referral = randomSelectReferral();
        Document doc = Jsoup
                .connect(url)
                .timeout(timeout)
                .userAgent(userAgent)
                .referrer(referral)
                .get();
        logger.info("Fetched {} with User Agent: {} and Referral {}", url, userAgent, referral);
        return doc;
    }

    /**
     * Extracts all href from a {@link org.jsoup.nodes.Document} using absolute resolution
     *
     * @param doc the {@link Document} to extrach hrefs from
     * @return set of {@link org.jsoup.nodes.Document}
     */
    private Set<String> extractAbsHref(Document doc) {
        Set<String> hrefString = new HashSet<>();
        Element content = doc.body();
        Elements links = content.getElementsByTag("a");
        for (Element link : links) {
            String href = link.attr("abs:href");
            hrefString.add(href);
        }

        // filter out external urls
        hrefString = filterOutParamsGeneratedString(hrefString);
        hrefString = filterOutExternalUrls(hrefString);
        return hrefString;
    }

    private Set<String> nonRecursiveExtractor(String url, Set<String> excludeURLs)
            throws IOException, InterruptedException {

        if (!include(url)) {
            return Collections.EMPTY_SET;
        }

        Set<String> crawledURLs = new HashSet<>();

        if (excludeURLs == null) {
            excludeURLs = new HashSet<>();
        }

        Long before = new Date().getTime();
        Document document = getDocumentFromUrl(url);
        Long after = new Date().getTime();
        Long loadTime = after - before;

        crawledURLs.add(url);

        FetchedPage firstPage = new FetchedPage();
        firstPage.setUrl(url);
        firstPage.setStatus(200);
        firstPage.setHtml(document.outerHtml());
        firstPage.setPlainText(Jsoup.parse(document.outerHtml()).text());
        firstPage.setTitle(document.title());
        firstPage.setLoadTime(loadTime);
        firstPage.setSourceUrl("index");
        action.execute(firstPage);

        Set<Url> urls = extractAbsHref(document)
                .stream()
                .map(fetched -> stringToUrlWithSource(fetched, url))
                .collect(Collectors.toSet());

        while (stillToFetch(urls.stream().map(x -> x.getUrl()).collect(Collectors.toSet()), excludeURLs, crawledURLs)) {
            for (Url toCrawl : new ArrayList<>(urls)) {
                if (shouldBeCrawled(toCrawl.getUrl(), crawledURLs)) {
                    FetchedPage fetchedPage = new FetchedPage();
                    try {
                        document = getDocumentFromUrl(toCrawl.getUrl());
                        fetchedPage.setUrl(toCrawl.getUrl());
                        fetchedPage.setStatus(200);
                        fetchedPage.setHtml(document.outerHtml());
                        fetchedPage.setPlainText(Jsoup.parse(document.outerHtml()).text());
                        fetchedPage.setTitle(document.title());
                        fetchedPage.setLoadTime(loadTime);
                        fetchedPage.setSourceUrl(toCrawl.getSourceUrl());
                        action.execute(fetchedPage);


                        Set<Url> urlsToFetch = extractAbsHref(document)
                                .stream()
                                .map(fetched -> stringToUrlWithSource(fetched, url))
                                .collect(Collectors.toSet());

                        urls.addAll(urlsToFetch);
                        urls.remove(url);
                        crawledURLs.add(toCrawl.getUrl());
                    } catch (IOException e) {
                            if (e instanceof UnsupportedMimeTypeException) {
                                fetchedPage.setStatus(415);
                                crawledURLs.add(toCrawl.getUrl());
                            } else if (e instanceof SocketTimeoutException) {
                                // it is a SocketTimeout Exception, it is probably a good idea to chill init?
                                Thread.sleep(3000);
                                // TODO add this link to a list of timedout url to be fetched later
                            } else {
                                fetchedPage.setStatus(404);
                                crawledURLs.add(toCrawl.getUrl());
                            }
                            fetchedPage.setUrl(toCrawl.getUrl());
                            fetchedPage.setSourceUrl(toCrawl.getUrl()); // TODO is it possible to find this?
                            // the action's execute is still called because
                            // we want to save the url that were broken, for instance
                            action.execute(fetchedPage);
                            if (errorAction != null) {
                                errorAction.process(url, e);
                            }
                            logger.error("Failed to crawl {}. With error message: {}", url, e);
                    }
                }
            }
        }

        destroyAsync();
        fireOnExit(crawledURLs);

        return crawledURLs;
    }

    private boolean shouldBeCrawled(String toCrawl, Set<String> crawledURLs) {
        if (crawledURLs.contains(toCrawl) || excludeURLs.contains(toCrawl)) {
            return false;
        }

        if (!include(toCrawl)) {
            excludeURLs.add(toCrawl);
            // should stop processing right?
            logger.info("Encountered {} but excluding from crawling", toCrawl);
            return false;
        }
        return true;
    }

    private boolean stillToFetch(Set<String> urlsToFetch, Set<String> excludeURLs, Set<String> crawledURLs) {
        HashSet<String> copyOfUrlsToFetch = new HashSet<>(urlsToFetch);
        copyOfUrlsToFetch.removeAll(excludeURLs);
        copyOfUrlsToFetch.removeAll(crawledURLs);
        return !copyOfUrlsToFetch.isEmpty();
    }


    private Url stringToUrlWithSource(String fetched, String sourceUrl) {
            Url fetchedUrl = new Url();
            fetchedUrl.setSourceUrl(sourceUrl);
            fetchedUrl.setUrl(fetched);
            return fetchedUrl;
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
        if (fetchExitCallback != null) {
            fetchExitCallback.callBack(urls);
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
            return rand.nextInt(max - min + 1) + min;
        }
    }


    private class Url {
        private String url;
        private String sourceUrl;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getSourceUrl() {
            return sourceUrl;
        }

        public void setSourceUrl(String sourceUrl) {
            this.sourceUrl = sourceUrl;
        }
    }
}
