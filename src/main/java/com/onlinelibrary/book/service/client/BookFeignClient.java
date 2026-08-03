package com.onlinelibrary.book.service.client;

import com.onlinelibrary.book.entity.Checkout;
import com.onlinelibrary.book.entity.History;
import com.onlinelibrary.book.entity.Payment;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(name = "administration-service", fallback = BookFeignClientFallback.class)
public interface BookFeignClient {

    @GetMapping("/api/checkout/secure/{userEmail}/{bookId}")
    Checkout findByUserEmailAndBookId(@PathVariable String userEmail,
                                      @PathVariable Long bookId,
                                      @RequestHeader("Authorization") String token);

    @GetMapping("/api/checkout/secure/{userEmail}")
    List<Checkout> findBooksByUserEmail(@PathVariable String userEmail,
                                        @RequestHeader("Authorization") String token);

    @PostMapping(value = "/api/checkout/secure", consumes = "application/json")
    void saveCheckout(Checkout checkout, @RequestHeader("Authorization") String token);

    @DeleteMapping(value = "/api/checkout/secure/{checkoutId}")
    void deleteCheckoutById(@PathVariable Long checkoutId, @RequestHeader("Authorization") String token);

    @PostMapping(value = "/api/history/secure", consumes = "application/json")
    void saveHistory(History history, @RequestHeader("Authorization") String token);

    @PostMapping(value = "/api/payment/secure", consumes = "application/json")
    void savePayment(Payment payment, @RequestHeader("Authorization") String token);

    @GetMapping("/api/payment/secure/{userEmail}")
    Payment findPaymentByUserEmail(@PathVariable String userEmail, @RequestHeader("Authorization") String token);

}
