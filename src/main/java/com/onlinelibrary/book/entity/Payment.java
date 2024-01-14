package com.onlinelibrary.book.entity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Payment {

    private Long id;
    private String userEmail;
    private double amount;

}
