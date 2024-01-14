package com.onlinelibrary.book.service.client;

import com.onlinelibrary.book.entity.Checkout;
import com.onlinelibrary.book.entity.History;
import com.onlinelibrary.book.entity.Payment;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(name = "administration-service", url = "http://administration-service:8081")
public interface BookFeignClient {

    @GetMapping("/api/checkout/secure/{userEmail}/{bookId}")
    Checkout findByUserEmailAndBookId(@RequestHeader(value = "Authorization") String token,
                                      @PathVariable("userEmail") String userEmail,
                                      @PathVariable("bookId") Long bookId);

    @GetMapping("/api/checkout/secure/{userEmail}")
    List<Checkout> findBooksByUserEmail(@RequestHeader(value = "Authorization") String token,
                                        @PathVariable("userEmail") String userEmail);

    @PostMapping(value = "/api/checkout/secure", consumes = "application/json")
    void saveCheckout(@RequestHeader(value = "Authorization") String token, Checkout checkout);

    @DeleteMapping(value = "/api/checkout/secure/{checkoutId}", consumes = "application/json")
    void deleteCheckoutById(@RequestHeader(value = "Authorization") String token,
                            @PathVariable("checkoutId") Long checkoutId);

    @PostMapping(value = "/api/history/secure", consumes = "application/json")
    void saveHistory(@RequestHeader(value = "Authorization") String token, History history);

    @PostMapping(value = "/api/payment/secure", consumes = "application/json")
    void savePayment(@RequestHeader(value = "Authorization") String token, Payment payment);

    @GetMapping("/api/payment/secure/{userEmail}")
    Payment findPaymentByUserEmail(@RequestHeader(value = "Authorization") String token,
                                   @PathVariable("userEmail") String userEmail);

}
