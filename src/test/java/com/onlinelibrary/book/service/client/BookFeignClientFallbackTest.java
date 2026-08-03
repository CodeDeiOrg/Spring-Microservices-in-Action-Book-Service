package com.onlinelibrary.book.service.client;

import com.onlinelibrary.book.entity.Checkout;
import com.onlinelibrary.book.entity.History;
import com.onlinelibrary.book.entity.Payment;
import com.onlinelibrary.book.exception.BookException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BookFeignClientFallbackTest {

    private static final String TOKEN = "Bearer test-token";

    private final BookFeignClientFallback fallback = new BookFeignClientFallback();

    @Test
    void findByUserEmailAndBookId_throwsBookException() {
        assertThatThrownBy(() -> fallback.findByUserEmailAndBookId("user@test.com", 1L, TOKEN))
                .isInstanceOf(BookException.class)
                .hasMessageContaining("unavailable");
    }

    @Test
    void findBooksByUserEmail_throwsBookException() {
        assertThatThrownBy(() -> fallback.findBooksByUserEmail("user@test.com", TOKEN))
                .isInstanceOf(BookException.class);
    }

    @Test
    void saveCheckout_throwsBookException() {
        Checkout checkout = new Checkout("user@test.com", "2026-01-01", "2026-01-08", 1L);
        assertThatThrownBy(() -> fallback.saveCheckout(checkout, TOKEN))
                .isInstanceOf(BookException.class);
    }

    @Test
    void deleteCheckoutById_throwsBookException() {
        assertThatThrownBy(() -> fallback.deleteCheckoutById(1L, TOKEN))
                .isInstanceOf(BookException.class);
    }

    @Test
    void saveHistory_throwsBookException() {
        History history = new History("user@test.com", "2026-01-01", "2026-01-08",
                "Title", "Author", "Desc", "img.jpg");
        assertThatThrownBy(() -> fallback.saveHistory(history, TOKEN))
                .isInstanceOf(BookException.class);
    }

    @Test
    void savePayment_throwsBookException() {
        Payment payment = new Payment();
        payment.setUserEmail("user@test.com");
        payment.setAmount(0.0);
        assertThatThrownBy(() -> fallback.savePayment(payment, TOKEN))
                .isInstanceOf(BookException.class);
    }

    @Test
    void findPaymentByUserEmail_throwsBookException() {
        assertThatThrownBy(() -> fallback.findPaymentByUserEmail("user@test.com", TOKEN))
                .isInstanceOf(BookException.class);
    }
}
