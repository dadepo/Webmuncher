package com.blogspot.geekabyte.krawkraw;

import lombok.Getter;
import lombok.Setter;

/**
 * Entity representing fetched web pages
 *
 * @author Dadepo Aderemi.
 */

public class FetchedPage {

    @Setter @Getter String url;
    @Setter @Getter int status;
    @Setter @Getter String title;
    @Setter @Getter long loadTime;
    @Setter @Getter String html;
}
