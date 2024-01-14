package com.onlinelibrary.book.event.model;

import com.onlinelibrary.book.entity.Book;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@AllArgsConstructor
public class BookChangeModel {
    private String type;
    private String action;
    private Book book;
}
