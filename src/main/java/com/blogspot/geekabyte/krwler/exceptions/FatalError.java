package com.blogspot.geekabyte.krwler.exceptions;

/**
 * A fatal error exception that terminates the execution of the crawler. Should not be caught.
 * Created by dadepo on 5/16/15.
 */
public class FatalError extends RuntimeException {
    private final String message;

    /**
     * Creates an instances of {@link com.blogspot.geekabyte.krwler.exceptions.FatalError}
     * Supplies the reason for the fatal error as contructor argument
     * @param message the reason for the fatal error
     */
    public FatalError(String message) {
        this.message = message;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
