package com.blogspot.geekabyte.krwler.interfaces;

import com.blogspot.geekabyte.krwler.FetchedPage;
import com.blogspot.geekabyte.krwler.Krwkrw;

/**
 * Interface for class that operates on {@link com.blogspot.geekabyte.krwler.FetchedPage}
 * returned by {@link Krwkrw}
 *
 * @author Dadepo Aderemi
 */
public interface KrwlerAction {

    /**
     * Operates on given {@link com.blogspot.geekabyte.krwler.FetchedPage}
     *
     * @param page a {@link com.blogspot.geekabyte.krwler.FetchedPage} object.
     */
    void execute(FetchedPage page);
}
