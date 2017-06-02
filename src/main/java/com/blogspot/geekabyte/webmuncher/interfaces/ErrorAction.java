package com.blogspot.geekabyte.webmuncher.interfaces;

import com.blogspot.geekabyte.webmuncher.Webmuncher;

/**
 * Interface for class that operates on {@link com.blogspot.geekabyte.webmuncher.FetchedPage}
 * returned by {@link Webmuncher}
 *
 * @author Dadepo Aderemi
 */
@FunctionalInterface
public interface ErrorAction {
    void process(String attemptedUrl, Exception exception);
}
