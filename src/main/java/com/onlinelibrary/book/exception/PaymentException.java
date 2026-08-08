package com.onlinelibrary.book.exception;

import lombok.Getter;

@Getter
public class PaymentException extends RuntimeException {
    private final String overdueBookTitle;
    private final String overdueBookDueDate;

    public PaymentException(String message) {
        this(message, null, null);
    }

    public PaymentException(String message, String overdueBookTitle, String overdueBookDueDate) {
        super(message);
        this.overdueBookTitle = overdueBookTitle;
        this.overdueBookDueDate = overdueBookDueDate;
    }

}
