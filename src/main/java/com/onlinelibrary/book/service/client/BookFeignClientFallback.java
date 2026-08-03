package com.onlinelibrary.book.service.client;

import com.onlinelibrary.book.entity.Checkout;
import com.onlinelibrary.book.entity.History;
import com.onlinelibrary.book.entity.Payment;
import com.onlinelibrary.book.exception.BookException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class BookFeignClientFallback implements BookFeignClient {

    private static final Logger logger = LoggerFactory.getLogger(BookFeignClientFallback.class);
    private static final String UNAVAILABLE = "Administration service is currently unavailable. Please try again later.";

    @Override
    public Checkout findByUserEmailAndBookId(String userEmail, Long bookId, String token) {
        logger.warn("Circuit open: findByUserEmailAndBookId fallback triggered for user={} bookId={}", userEmail, bookId);
        throw new BookException(UNAVAILABLE);
    }

    @Override
    public List<Checkout> findBooksByUserEmail(String userEmail, String token) {
        logger.warn("Circuit open: findBooksByUserEmail fallback triggered for user={}", userEmail);
        throw new BookException(UNAVAILABLE);
    }

    @Override
    public void saveCheckout(Checkout checkout, String token) {
        logger.warn("Circuit open: saveCheckout fallback triggered for bookId={}", checkout.getBookId());
        throw new BookException(UNAVAILABLE);
    }

    @Override
    public void deleteCheckoutById(Long checkoutId, String token) {
        logger.warn("Circuit open: deleteCheckoutById fallback triggered for checkoutId={}", checkoutId);
        throw new BookException(UNAVAILABLE);
    }

    @Override
    public void saveHistory(History history, String token) {
        logger.warn("Circuit open: saveHistory fallback triggered for user={}", history.getUserEmail());
        throw new BookException(UNAVAILABLE);
    }

    @Override
    public void savePayment(Payment payment, String token) {
        logger.warn("Circuit open: savePayment fallback triggered for user={}", payment.getUserEmail());
        throw new BookException(UNAVAILABLE);
    }

    @Override
    public Payment findPaymentByUserEmail(String userEmail, String token) {
        logger.warn("Circuit open: findPaymentByUserEmail fallback triggered for user={}", userEmail);
        throw new BookException(UNAVAILABLE);
    }
}
