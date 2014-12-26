package com.blogspot.geekabyte.krawkraw.interfaces;

import com.blogspot.geekabyte.krawkraw.FetchedPage;

/**
 * Interface for class that operates on {@link com.blogspot.geekabyte.krawkraw.FetchedPage}
 * returned by {@link com.blogspot.geekabyte.krawkraw.Krawkraw}
 *
 * @author Dadepo Aderemi
 */
public interface KrawlerAction {

    /**
     * Operates on given {@link com.blogspot.geekabyte.krawkraw.FetchedPage}
     *
     * @param page a {@link com.blogspot.geekabyte.krawkraw.FetchedPage} object.
     */
    void execute(FetchedPage page);
}
