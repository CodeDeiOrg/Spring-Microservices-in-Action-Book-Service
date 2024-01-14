package com.onlinelibrary.book.exception;

public class EmailMissingException extends RuntimeException {
    public EmailMissingException(String message) {
        super(message);
    }
}
