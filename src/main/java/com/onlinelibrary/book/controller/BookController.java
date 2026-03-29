package com.onlinelibrary.book.controller;

import com.onlinelibrary.book.entity.Book;
import com.onlinelibrary.book.exception.AdminControlException;
import com.onlinelibrary.book.responsemodels.ShelfCurrentLoansResponse;
import com.onlinelibrary.book.service.BookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.text.ParseException;
import java.util.List;

import static com.onlinelibrary.book.utils.Constants.ROLES_CLAIM;
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
    public List<ShelfCurrentLoansResponse> currentLoans(@AuthenticationPrincipal Jwt jwt)
            throws ParseException {
        String userEmail = jwt.getClaim("email");
        String token = "Bearer " + jwt.getTokenValue();
        return bookService.currentLoans(token, userEmail);
    }

    @GetMapping("/secure/currentloans/count")
    public int currentLoansCount(@AuthenticationPrincipal Jwt jwt) {
        String userEmail = jwt.getClaim("email");
        String token = "Bearer " + jwt.getTokenValue();
        return bookService.currentLoansCount(token, userEmail);
    }

    @GetMapping("/secure/ischeckedout/byuser")
    public Boolean checkoutBookByUser(@AuthenticationPrincipal Jwt jwt,
                                      @RequestParam Long bookId) {
        String userEmail = jwt.getClaim("email");
        String token = "Bearer " + jwt.getTokenValue();
        return bookService.checkoutBookByUser(token, userEmail, bookId);
    }

    @PutMapping("/secure/checkout")
    public Book checkoutBook(@AuthenticationPrincipal Jwt jwt,
                             @RequestParam Long bookId) throws Exception {
        String userEmail = jwt.getClaim("email");
        String token = "Bearer " + jwt.getTokenValue();
        return bookService.checkoutBook(token, userEmail, bookId);
    }

    @PutMapping("/secure/return")
    public void returnBook(@AuthenticationPrincipal Jwt jwt,
                           @RequestParam Long bookId) throws Exception {
        String userEmail = jwt.getClaim("email");
        String token = "Bearer " + jwt.getTokenValue();
        bookService.returnBook(token, userEmail, bookId);
    }

    @PutMapping("/secure/renew/loan")
    public void renewLoan(@AuthenticationPrincipal Jwt jwt,
                          @RequestParam Long bookId) throws Exception {
        String userEmail = jwt.getClaim("email");
        String token = "Bearer " + jwt.getTokenValue();
        bookService.renewLoan(token, userEmail, bookId);
    }

    @DeleteMapping("/secure/{bookId}")
    public void deleteBookById(@AuthenticationPrincipal Jwt jwt,
                               @PathVariable Long bookId) {
        List<String> roles = jwt.getClaim(ROLES_CLAIM);
        if (roles == null || !roles.contains(ADMIN)) {
            throw new AdminControlException(ADMINISTRATION_PAGE_ONLY);
        }
        bookService.deleteBookById(bookId);
    }
}
