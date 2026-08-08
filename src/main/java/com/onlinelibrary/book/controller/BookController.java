package com.onlinelibrary.book.controller;

import com.onlinelibrary.book.entity.Book;
import com.onlinelibrary.book.responsemodels.ShelfCurrentLoansResponse;
import com.onlinelibrary.book.service.BookService;
import com.onlinelibrary.book.utils.JwtUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/books")
public class BookController {
    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    @GetMapping("/secure/currentloans")
    public List<ShelfCurrentLoansResponse> currentLoans(@AuthenticationPrincipal Jwt jwt) {
        return bookService.currentLoans(JwtUtils.extractEmail(jwt), bearerToken(jwt));
    }

    @GetMapping("/secure/currentloans/count")
    public int currentLoansCount(@AuthenticationPrincipal Jwt jwt) {
        return bookService.currentLoansCount(JwtUtils.extractEmail(jwt), bearerToken(jwt));
    }

    @GetMapping("/secure/ischeckedout/byuser")
    public Boolean checkoutBookByUser(@AuthenticationPrincipal Jwt jwt,
                                      @RequestParam Long bookId) {
        return bookService.checkoutBookByUser(JwtUtils.extractEmail(jwt), bookId, bearerToken(jwt));
    }

    @PutMapping("/secure/checkout")
    public Book checkoutBook(@AuthenticationPrincipal Jwt jwt,
                             @RequestParam Long bookId) {
        return bookService.checkoutBook(JwtUtils.extractEmail(jwt), bookId, bearerToken(jwt));
    }

    @PutMapping("/secure/return")
    public void returnBook(@AuthenticationPrincipal Jwt jwt,
                           @RequestParam Long bookId) {
        bookService.returnBook(JwtUtils.extractEmail(jwt), bookId, bearerToken(jwt));
    }

    @PutMapping("/secure/renew/loan")
    public void renewLoan(@AuthenticationPrincipal Jwt jwt,
                          @RequestParam Long bookId) {
        bookService.renewLoan(JwtUtils.extractEmail(jwt), bookId, bearerToken(jwt));
    }

    @PreAuthorize("hasAuthority('admin')")
    @DeleteMapping("/secure/{bookId}")
    public void deleteBookById(@PathVariable Long bookId) {
        bookService.deleteBookById(bookId);
    }

    @PreAuthorize("hasAuthority('admin')")
    @PostMapping("/secure")
    public Book createBook(@RequestBody Book book) {
        return bookService.createBook(book);
    }

    private static String bearerToken(Jwt jwt) {
        return "Bearer " + jwt.getTokenValue();
    }
}
