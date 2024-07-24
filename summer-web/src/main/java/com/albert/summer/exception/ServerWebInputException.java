package com.albert.summer.exception;

/**
 * 400 client error.
 * @author admin
 */
public class ServerWebInputException extends ErrorResponseException {

    public ServerWebInputException() {
        super(400);
    }

    public ServerWebInputException(String message) {
        super(400, message);
    }

    public ServerWebInputException(Throwable cause) {
        super(400, cause);
    }

    public ServerWebInputException(String message, Throwable cause) {
        super(400, message, cause);
    }
}
