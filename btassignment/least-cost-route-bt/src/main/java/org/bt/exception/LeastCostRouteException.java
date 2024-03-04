package org.bt.exception;

public class LeastCostRouteException extends Exception {

    public static final String PERSON_NAME_NOT_VALID = "INVALID PERSON NAME";
    public static final String FILE_READ_ERROR = "ERROR WHILE READING CSV FILE";
    public static final String ROUTE_NOT_FOUND = "ROUTE_NOT_AVAILABLE";

    public LeastCostRouteException(String message) {
        super(message);
    }

    public LeastCostRouteException(String message, Throwable cause) {
        super(message, cause);
    }
}