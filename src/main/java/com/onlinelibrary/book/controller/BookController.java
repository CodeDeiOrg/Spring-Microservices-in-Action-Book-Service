package com.onlinelibrary.book.controller;

import com.onlinelibrary.book.entity.Book;
import com.onlinelibrary.book.exception.AdminControlException;
import com.onlinelibrary.book.responsemodels.ShelfCurrentLoansResponse;
import com.onlinelibrary.book.service.BookService;
import com.onlinelibrary.book.utils.ExtractJWT;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.text.ParseException;
import java.util.List;

import static com.onlinelibrary.book.utils.Constants.SUB;
import static com.onlinelibrary.book.utils.Constants.USER_TYPE;
import static com.onlinelibrary.book.utils.Constants.ADMIN;
import static com.onlinelibrary.book.utils.Constants.ADMINISTRATION_PAGE_ONLY;

@RestController
@RequestMapping("/api/books")
public class BookController {
    private final BookService bookService;

    @Autowired
    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    @GetMapping("/secure/currentloans")
    public List<ShelfCurrentLoansResponse> currentLoans(@RequestHeader(value = "Authorization") String token)
            throws ParseException {
        String userEmail = ExtractJWT.payloadJWTExtraction(token, SUB);
        return bookService.currentLoans(token, userEmail);
    }

    @GetMapping("/secure/currentloans/count")
    public int currentLoansCount(@RequestHeader(value = "Authorization") String token) {
        String userEmail = ExtractJWT.payloadJWTExtraction(token, SUB);
        return bookService.currentLoansCount(token, userEmail);
    }

    @GetMapping("/secure/ischeckedout/byuser")
    public Boolean checkoutBookByUser(@RequestHeader(value = "Authorization") String token,
                                      @RequestParam Long bookId) {
        String userEmail = ExtractJWT.payloadJWTExtraction(token, SUB);
        return bookService.checkoutBookByUser(token, userEmail, bookId);
    }

    @PutMapping("/secure/checkout")
    public Book checkoutBook(@RequestHeader(value = "Authorization") String token,
                             @RequestParam Long bookId) throws Exception {
        String userEmail = ExtractJWT.payloadJWTExtraction(token, SUB);
        return bookService.checkoutBook(token, userEmail, bookId);
    }

    @PutMapping("/secure/return")
    public void returnBook(@RequestHeader(value = "Authorization") String token,
                           @RequestParam Long bookId) throws Exception {
        String userEmail = ExtractJWT.payloadJWTExtraction(token, SUB);
        bookService.returnBook(token, userEmail, bookId);
    }

    @PutMapping("/secure/renew/loan")
    public void renewLoan(@RequestHeader(value = "Authorization") String token,
                          @RequestParam Long bookId) throws Exception {
        String userEmail = ExtractJWT.payloadJWTExtraction(token, SUB);
        bookService.renewLoan(token, userEmail, bookId);
    }

    @DeleteMapping("/secure/{bookId}")
    public void deleteBookById(@RequestHeader(value = "Authorization") String token,
                               @PathVariable Long bookId) {
        String admin = ExtractJWT.payloadJWTExtraction(token, USER_TYPE);
        if (admin == null || !admin.equals(ADMIN)) {
            throw new AdminControlException(ADMINISTRATION_PAGE_ONLY);
        }
        bookService.deleteBookById(bookId);
    }
}
