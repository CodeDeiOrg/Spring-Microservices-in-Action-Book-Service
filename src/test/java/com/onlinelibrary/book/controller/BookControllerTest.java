package com.onlinelibrary.book.controller;

import com.onlinelibrary.book.entity.Book;
import com.onlinelibrary.book.exception.BookException;
import com.onlinelibrary.book.exception.PaymentException;
import com.onlinelibrary.book.service.BookService;
import com.onlinelibrary.book.support.ControllerTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = BookController.class, excludeAutoConfiguration = OAuth2ResourceServerAutoConfiguration.class)
@Import(ControllerTestSupport.class)
class BookControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean BookService bookService;
    @MockitoBean JwtDecoder jwtDecoder;

    // --- GET /api/books/secure/currentloans ---

    @Test
    void currentLoans_withValidJwt_returns200() throws Exception {
        given(bookService.currentLoans(anyString(), anyString())).willReturn(List.of());

        mockMvc.perform(get("/api/books/secure/currentloans")
                        .with(jwt().jwt(j -> j.claim("email", "user@test.com"))))
                .andExpect(status().isOk());

        verify(bookService).currentLoans(org.mockito.ArgumentMatchers.eq("user@test.com"), anyString());
    }

    @Test
    void currentLoans_withNoToken_returns401() throws Exception {
        mockMvc.perform(get("/api/books/secure/currentloans"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(bookService);
    }

    // --- GET /api/books/secure/currentloans/count ---

    @Test
    void currentLoansCount_withValidJwt_returns200() throws Exception {
        given(bookService.currentLoansCount(anyString(), anyString())).willReturn(2);

        mockMvc.perform(get("/api/books/secure/currentloans/count")
                        .with(jwt().jwt(j -> j.claim("email", "user@test.com"))))
                .andExpect(status().isOk());

        verify(bookService).currentLoansCount(org.mockito.ArgumentMatchers.eq("user@test.com"), anyString());
    }

    // --- GET /api/books/secure/ischeckedout/byuser ---

    @Test
    void checkoutBookByUser_withValidJwt_returns200() throws Exception {
        given(bookService.checkoutBookByUser(anyString(), anyLong(), anyString())).willReturn(true);

        mockMvc.perform(get("/api/books/secure/ischeckedout/byuser")
                        .param("bookId", "1")
                        .with(jwt().jwt(j -> j.claim("email", "user@test.com"))))
                .andExpect(status().isOk());

        verify(bookService).checkoutBookByUser(org.mockito.ArgumentMatchers.eq("user@test.com"), org.mockito.ArgumentMatchers.eq(1L), anyString());
    }

    @Test
    void checkoutBookByUser_withNoToken_returns401() throws Exception {
        mockMvc.perform(get("/api/books/secure/ischeckedout/byuser").param("bookId", "1"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(bookService);
    }

    // --- PUT /api/books/secure/checkout ---

    @Test
    void checkoutBook_withValidJwt_returns200() throws Exception {
        Book book = new Book();
        book.setId(1L);
        given(bookService.checkoutBook(anyString(), anyLong(), anyString())).willReturn(book);

        mockMvc.perform(put("/api/books/secure/checkout")
                        .param("bookId", "1")
                        .with(jwt().jwt(j -> j.claim("email", "user@test.com"))))
                .andExpect(status().isOk());

        verify(bookService).checkoutBook(org.mockito.ArgumentMatchers.eq("user@test.com"), org.mockito.ArgumentMatchers.eq(1L), anyString());
    }

    @Test
    void checkoutBook_withNoToken_returns401() throws Exception {
        mockMvc.perform(put("/api/books/secure/checkout").param("bookId", "1"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(bookService);
    }

    @Test
    void checkoutBook_bookException_returns400() throws Exception {
        willThrow(new BookException("Book doesn't exist or already checked out by user"))
                .given(bookService).checkoutBook(anyString(), anyLong(), anyString());

        mockMvc.perform(put("/api/books/secure/checkout")
                        .param("bookId", "1")
                        .with(jwt().jwt(j -> j.claim("email", "user@test.com"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Book doesn't exist or already checked out by user"));
    }

    @Test
    void checkoutBook_paymentException_returns402() throws Exception {
        willThrow(new PaymentException("Outstanding fees"))
                .given(bookService).checkoutBook(anyString(), anyLong(), anyString());

        mockMvc.perform(put("/api/books/secure/checkout")
                        .param("bookId", "1")
                        .with(jwt().jwt(j -> j.claim("email", "user@test.com"))))
                .andExpect(status().isPaymentRequired())
                .andExpect(jsonPath("$.message").value("Outstanding fees"));
    }

    // --- PUT /api/books/secure/return ---

    @Test
    void returnBook_withValidJwt_returns200() throws Exception {
        mockMvc.perform(put("/api/books/secure/return")
                        .param("bookId", "1")
                        .with(jwt().jwt(j -> j.claim("email", "user@test.com"))))
                .andExpect(status().isOk());

        verify(bookService).returnBook(org.mockito.ArgumentMatchers.eq("user@test.com"), org.mockito.ArgumentMatchers.eq(1L), anyString());
    }

    @Test
    void returnBook_withNoToken_returns401() throws Exception {
        mockMvc.perform(put("/api/books/secure/return").param("bookId", "1"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(bookService);
    }

    @Test
    void returnBook_bookException_returns400() throws Exception {
        willThrow(new BookException("Book does not exist or not checked out by user"))
                .given(bookService).returnBook(anyString(), anyLong(), anyString());

        mockMvc.perform(put("/api/books/secure/return")
                        .param("bookId", "1")
                        .with(jwt().jwt(j -> j.claim("email", "user@test.com"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Book does not exist or not checked out by user"));
    }

    // --- PUT /api/books/secure/renew/loan ---

    @Test
    void renewLoan_withValidJwt_returns200() throws Exception {
        mockMvc.perform(put("/api/books/secure/renew/loan")
                        .param("bookId", "1")
                        .with(jwt().jwt(j -> j.claim("email", "user@test.com"))))
                .andExpect(status().isOk());

        verify(bookService).renewLoan(org.mockito.ArgumentMatchers.eq("user@test.com"), org.mockito.ArgumentMatchers.eq(1L), anyString());
    }

    @Test
    void renewLoan_withNoToken_returns401() throws Exception {
        mockMvc.perform(put("/api/books/secure/renew/loan").param("bookId", "1"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(bookService);
    }

    // --- DELETE /api/books/secure/{bookId} ---

    @Test
    void deleteBookById_withAdminToken_returns200() throws Exception {
        mockMvc.perform(delete("/api/books/secure/1")
                        .with(jwt().authorities(new SimpleGrantedAuthority("admin"))))
                .andExpect(status().isOk());

        verify(bookService).deleteBookById(1L);
    }

    @Test
    void deleteBookById_withUserToken_returns403() throws Exception {
        mockMvc.perform(delete("/api/books/secure/1")
                        .with(jwt()))
                .andExpect(status().isForbidden());

        verifyNoInteractions(bookService);
    }

    @Test
    void deleteBookById_withNoToken_returns401() throws Exception {
        mockMvc.perform(delete("/api/books/secure/1"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(bookService);
    }
}
