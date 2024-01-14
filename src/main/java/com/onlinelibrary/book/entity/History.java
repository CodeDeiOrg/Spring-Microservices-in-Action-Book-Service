package com.onlinelibrary.book.entity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class History {

    public History() {
    }

    public History(String userEmail, String checkoutDate, String returnedDate, String title,
                   String author, String description, String img) {
        this.userEmail = userEmail;
        this.checkoutDate = checkoutDate;
        this.returnedDate = returnedDate;
        this.title = title;
        this.author = author;
        this.description = description;
        this.img = img;
    }

    private Long id;
    private String userEmail;
    private String checkoutDate;
    private String returnedDate;
    private String title;
    private String author;
    private String description;
    private String img;
}
