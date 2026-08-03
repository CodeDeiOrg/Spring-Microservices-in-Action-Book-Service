package com.onlinelibrary.book.requestmodels;

import lombok.Data;

@Data
public class MessageRequest {
    private String title;
    private String question;
}