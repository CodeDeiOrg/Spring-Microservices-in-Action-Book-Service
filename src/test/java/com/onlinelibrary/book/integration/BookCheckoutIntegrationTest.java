package com.onlinelibrary.book.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.onlinelibrary.book.entity.Book;
import com.onlinelibrary.book.entity.Checkout;
import com.onlinelibrary.book.entity.Payment;
import com.onlinelibrary.book.event.service.KafkaProducerService;
import com.onlinelibrary.book.repository.BookRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.wiremock.spring.ConfigureWireMock;
import org.wiremock.spring.EnableWireMock;
import org.wiremock.spring.InjectWireMock;

import java.time.LocalDate;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full-stack integration tests for the checkout flow.
 * What runs for real:
 *   - Spring MVC layer (real HTTP dispatch, security filter chain)
 *   - BookService business logic
 *   - BookRepository → H2 in-memory database
 *   - BookFeignClient → WireMock server (intercepts all HTTP calls to administration-service)
 * What is mocked:
 *   - JWT verification: jwt() post-processor bypasses the JwtDecoder entirely
 *   - KafkaProducerService: no Kafka broker needed in tests
 *   - administration-service HTTP calls: WireMock stubs return controlled responses
 * How WireMock intercepts Feign:
 *   @EnableWireMock + @ConfigureWireMock sets the property
 *   spring.cloud.openfeign.client.config.administration-service.url
 *   to the WireMock server's base URL before the Spring context starts.
 *   Feign then sends all calls to WireMock instead of the real service.
 */
@SpringBootTest
@AutoConfigureMockMvc
@EnableWireMock(@ConfigureWireMock(
        name = "administration-service",
        baseUrlProperties = "spring.cloud.openfeign.client.config.administration-service.url"
))
class BookCheckoutIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired BookRepository bookRepository;
    @Autowired ObjectMapper objectMapper;

    // WireMock server whose URL is injected into Feign's config
    @InjectWireMock("administration-service")
    WireMockServer wireMock;

    @MockitoBean
    KafkaProducerService kafkaProducerService;

    private Book savedBook;

    @BeforeEach
    void setUp() {
        bookRepository.deleteAll();
        wireMock.resetAll();

        Book book = new Book();
        book.setTitle("Clean Code");
        book.setAuthor("Robert Martin");
        book.setDescription("A handbook of agile software craftsmanship");
        book.setCategory("Tech");
        book.setCopies(5);
        book.setCopiesAvailable(5);
        savedBook = bookRepository.save(book);
    }

    // -------------------------------------------------------------------------
    // PUT /api/books/secure/checkout — success path
    // -------------------------------------------------------------------------

    @Test
    void checkoutBook_success_returns200AndDecrementsAvailableCopies() throws Exception {
        // No existing checkout for this user+book → 200 with null body (how admin-service behaves)
        wireMock.stubFor(get(urlPathEqualTo(
                "/api/checkout/secure/user%40test.com/" + savedBook.getId()))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("null")));

        // User has no other active checkouts
        wireMock.stubFor(get(urlPathEqualTo("/api/checkout/secure/user%40test.com"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("[]")));

        // No existing payment record → 200 with null body (admin-service returns null when not found)
        wireMock.stubFor(get(urlPathEqualTo("/api/payment/secure/user%40test.com"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("null")));

        // savePayment — administration-service accepts the new payment record
        wireMock.stubFor(post(urlPathEqualTo("/api/payment/secure"))
                .willReturn(aResponse().withStatus(200)));

        // saveCheckout — administration-service persists the checkout
        wireMock.stubFor(post(urlPathEqualTo("/api/checkout/secure"))
                .willReturn(aResponse().withStatus(200)));

        mockMvc.perform(put("/api/books/secure/checkout")
                        .param("bookId", savedBook.getId().toString())
                        .with(jwt().jwt(j -> j.claim("email", "user@test.com"))))
                .andExpect(status().isOk());

        // Book copies decremented in the real DB
        Book updated = bookRepository.findById(savedBook.getId()).orElseThrow();
        assertThat(updated.getCopiesAvailable()).isEqualTo(4);
    }

    // -------------------------------------------------------------------------
    // PUT /api/books/secure/checkout — outstanding fees path
    // -------------------------------------------------------------------------

    @Test
    void checkoutBook_outstandingFees_returns402() throws Exception {
        // No existing checkout for this book → 200 with null body
        wireMock.stubFor(get(urlPathEqualTo(
                "/api/checkout/secure/user%40test.com/" + savedBook.getId()))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("null")));

        // No overdue loans
        wireMock.stubFor(get(urlPathEqualTo("/api/checkout/secure/user%40test.com"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("[]")));

        // Payment record with outstanding balance — WireMock returns real JSON
        Payment payment = new Payment();
        payment.setUserEmail("user@test.com");
        payment.setAmount(9.99);
        wireMock.stubFor(get(urlPathEqualTo("/api/payment/secure/user%40test.com"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(payment))));

        mockMvc.perform(put("/api/books/secure/checkout")
                        .param("bookId", savedBook.getId().toString())
                        .with(jwt().jwt(j -> j.claim("email", "user@test.com"))))
                .andExpect(status().isPaymentRequired());

        // Copies must NOT be decremented
        Book unchanged = bookRepository.findById(savedBook.getId()).orElseThrow();
        assertThat(unchanged.getCopiesAvailable()).isEqualTo(5);
    }

    // -------------------------------------------------------------------------
    // PUT /api/books/secure/return — success path
    // -------------------------------------------------------------------------

    @Test
    void returnBook_success_returns200AndIncrementsAvailableCopies() throws Exception {
        // Simulate book already fully checked out
        savedBook.setCopiesAvailable(0);
        bookRepository.save(savedBook);

        // Active checkout exists for this user+book
        Checkout checkout = new Checkout(
                "user@test.com",
                LocalDate.now().minusDays(3).toString(),
                LocalDate.now().plusDays(4).toString(),
                savedBook.getId()
        );
        checkout.setId(42L);

        wireMock.stubFor(get(urlPathEqualTo(
                "/api/checkout/secure/user%40test.com/" + savedBook.getId()))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(checkout))));

        // deleteCheckoutById
        wireMock.stubFor(delete(urlPathEqualTo("/api/checkout/secure/42"))
                .willReturn(aResponse().withStatus(200)));

        // saveHistory
        wireMock.stubFor(post(urlPathEqualTo("/api/history/secure"))
                .willReturn(aResponse().withStatus(200)));

        mockMvc.perform(put("/api/books/secure/return")
                        .param("bookId", savedBook.getId().toString())
                        .with(jwt().jwt(j -> j.claim("email", "user@test.com"))))
                .andExpect(status().isOk());

        // Copies incremented in the real DB
        Book updated = bookRepository.findById(savedBook.getId()).orElseThrow();
        assertThat(updated.getCopiesAvailable()).isEqualTo(1);
    }
}
