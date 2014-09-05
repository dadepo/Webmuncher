package com.blogspot.geekabyte.krawkraw;

import lombok.Getter;
import lombok.Setter;

/**
 * Entity representing fetched web pages
 *
 * @author Dadepo Aderemi.
 */

public class FetchedPage {

    @Setter @Getter private String url;
    @Setter @Getter private int status;
    @Setter @Getter private String title;
    @Setter @Getter private long loadTime;
    @Setter @Getter private String html;
    @Setter @Getter private String sourceUrl;

}
