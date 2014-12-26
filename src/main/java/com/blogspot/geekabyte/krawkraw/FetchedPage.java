package com.blogspot.geekabyte.krawkraw;

/**
 * Class representing fetched web pages
 *
 * @author Dadepo Aderemi.
 */

public class FetchedPage {

    private String url;
    private int status;
    private String title;
    private long loadTime;
    private String html;
    private String sourceUrl;

    /**
     * Gets the url of the fetched page 
     * @return the url of the fetched page
     */
    public String getUrl() {
        return url;
    }

    /**
     * Sets the url of the fetched page 
     * @param url the url of the fetched page
     */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * Gets the status of the request as HTTP status code 
     * @return the status of the request as HTTP status code
     */
    public int getStatus() {
        return status;
    }

    /**
     * Sets the status of the request as HTTP status code 
     * @param status the status of the request as HTTP status code
     */
    public void setStatus(int status) {
        this.status = status;
    }

    /**
     * Gets the title of the page 
     * @return the title of the page
     */
    public String getTitle() {
        return title;
    }

    /**
     * Sets the title of the page 
     * @param title the title of the page
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Gets the time it took to fetch the page expressed in milliseconds 
     * @return the time it took to fetch the page in milliseconds
     */
    public long getLoadTime() {
        return loadTime;
    }

    /**
     * Sets the time it took to fetch the page expressed in milliseconds 
     * @param loadTime the time it took to fetch the page in milliseconds
     */
    public void setLoadTime(long loadTime) {
        this.loadTime = loadTime;
    }

    /**
     * Gets the content of the page as HTML string 
     * @return the content of the page as HTML string
     */
    public String getHtml() {
        return html;
    }

    /**
     * Sets the content of the page in HTML string
     * @param html the html string of the page
     */
    public void setHtml(String html) {
        this.html = html;
    }

    /**
     * Gets the url from which the fetched page was requested 
     * @return the url from which the fetched page was requested
     */
    public String getSourceUrl() {
        return sourceUrl;
    }

    /**
     * Sets the url from which the fetched page was requested
     * @param sourceUrl the url from which the fetched page was requested
     */
    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }
}
