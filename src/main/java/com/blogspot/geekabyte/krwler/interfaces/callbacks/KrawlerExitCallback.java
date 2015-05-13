package com.blogspot.geekabyte.krwler.interfaces.callbacks;

import java.util.Set;

/**
 * Interface for callback on crawler exits. Most useful when using Asynchronous mode  
 * Created by dadepo on 1/6/15.
 */
@FunctionalInterface
public interface KrawlerExitCallback {

    /**
     * Method to execute when crawler finishes
     * @param crawledUrls the crawled urls
     */
    public void callBack(Set<String> crawledUrls);
    
}
