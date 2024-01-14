package com.onlinelibrary.book.entity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Checkout {

    public Checkout() {
    }

    public Checkout(String userEmail, String checkoutDate, String returnDate, Long bookId) {
        this.userEmail = userEmail;
        this.checkoutDate = checkoutDate;
        this.returnDate = returnDate;
        this.bookId = bookId;
    }

    private Long id;
    private String userEmail;
    private String checkoutDate;
    private String returnDate;
    private Long bookId;
}
