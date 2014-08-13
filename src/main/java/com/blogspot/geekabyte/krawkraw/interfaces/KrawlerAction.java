package com.blogspot.geekabyte.krawkraw.interfaces;

import com.blogspot.geekabyte.krawkraw.FetchedPage;

/**
 * Interface for class that operates on {@link com.blogspot.geekabyte.krawkraw.FetchedPage}
 * returned by {@link com.blogspot.geekabyte.krawkraw.Krawkraw}
 */
public interface KrawlerAction {

    /**
     * Operates on given {@link com.blogspot.geekabyte.krawkraw.FetchedPage}
     *
     * @param page
     */
    public void execute(FetchedPage page);
}
