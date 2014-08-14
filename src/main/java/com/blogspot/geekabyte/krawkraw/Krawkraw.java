package com.blogspot.geekabyte.krawkraw;

import com.blogspot.geekabyte.krawkraw.interfaces.KrawlerAction;
import lombok.Getter;
import lombok.Setter;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Dadepo Aderemi.
 */
public class Krawkraw {

    Logger log = LoggerFactory.getLogger(Krawkraw.class);

    private final String defaultCharset = "UTF-8";


    /**
     * The base URL. This ensures that the crawlers does not download external URLs
     * --SETTER--
     * Sets the base URL
     *
     * @param baseUrl the base URL
     * <p/>
     * --GETTER--
     * Returns the set baseUrl
     * @return baseUrl the base URL
     */
    @Setter @Getter
    private String baseUrl;


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
     * The class implementing {@link com.blogspot.geekabyte.krawkraw.interfaces.KrawlerAction}
     * This class is used to operate on the {@link com.blogspot.geekabyte.krawkraw.FetchedPage}
     * returned by the crawler
     * <p/>
     * --SETTER--
     * Sets the action
     *
     * @param action action class
     * <p/>
     * --GETTER--
     * Gets the set action
     * @return action the set action
     */
    @Setter @Getter
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


    public Krawkraw() {

    }

    public Document getDocumentFromUrl(String url) throws IOException {
        String userAgent = randomSelectUserAgent();
        String referral = randomSelectreferral();
        Document doc = Jsoup
                .connect(url)
                .userAgent(userAgent)
                .referrer(referral).get();
        log.info("Fetched {} with User Agent: {} and Referral {}", url, userAgent, referral);
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
     *
     * @param url         the URL to start extracting from
     * @param excludeURLs a set that contains already crawled URLs. You can include URLS you want omitted
     * @return A set containing all the URL crawled
     *
     * @throws IOException
     * @throws InterruptedException
     */
    public Set<String> extractAllFromUrl(String url, Set<String> excludeURLs)
            throws IOException, InterruptedException {

        if (excludeURLs == null) {
            excludeURLs = new HashSet<>();
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
                excludeURLs.add(url);
                out.put(url, doc);

                fetchedPage.setUrl(url);
                fetchedPage.setStatus(200);
                fetchedPage.setHtml(doc.outerHtml());
                fetchedPage.setTitle(doc.title());
                fetchedPage.setLoadTime(loadTime);

                // perform action on fetchedPage
                action.execute(fetchedPage);
                Thread.sleep(delay);
            } catch (IOException e) {
                excludeURLs.add(url);
                fetchedPage.setStatus(404);
                fetchedPage.setUrl(url);
                action.execute(fetchedPage);
                log.error("Failed to crawl {}", url, e);
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
                extractAllFromUrl(href, excludeURLs);
            }
            return hrefs;
        }
    }

    /**
     * Recursively Extracts all href starting from a given url
     *
     * @param url the URL to start extracting from
     * @return A set containing all the URL crawled
     * @throws IOException
     * @throws InterruptedException
     */
    public Set<String> extractAllFromUrl(String url) throws IOException, InterruptedException {
        extractAllFromUrl(url, new HashSet<>());
        return null;
    }

    private Set<String> filterOutExternalUrls(Set<String> urls) {
        urls = urls.stream().filter(u -> u.contains(baseUrl)).collect(Collectors.toSet());
        urls = urls.stream().filter(u -> !u.contains("mailto")).collect(Collectors.toSet());
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
}
