package com.blogspot.geekabyte.webmuncher.interfaces;

import com.blogspot.geekabyte.webmuncher.FetchedPage;
import com.blogspot.geekabyte.webmuncher.Webmuncher;

/**
 * Interface for class that operates on {@link com.blogspot.geekabyte.webmuncher.FetchedPage}
 * returned by {@link Webmuncher}
 *
 * @author Dadepo Aderemi
 */
@FunctionalInterface
public interface FetchAction {

    /**
     * Operates on given {@link com.blogspot.geekabyte.webmuncher.FetchedPage}
     *
     * @param page a {@link com.blogspot.geekabyte.webmuncher.FetchedPage} object.
     */
    void execute(FetchedPage page);
}
