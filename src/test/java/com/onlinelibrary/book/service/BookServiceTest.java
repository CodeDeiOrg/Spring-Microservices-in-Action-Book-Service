package com.onlinelibrary.book.service;

import com.onlinelibrary.book.entity.Book;
import com.onlinelibrary.book.entity.Checkout;
import com.onlinelibrary.book.entity.Payment;
import com.onlinelibrary.book.exception.BookException;
import com.onlinelibrary.book.exception.PaymentException;
import com.onlinelibrary.book.repository.BookRepository;
import com.onlinelibrary.book.responsemodels.ShelfCurrentLoansResponse;
import com.onlinelibrary.book.service.client.BookFeignClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    private static final String TOKEN = "Bearer test-token";
    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-08-03T10:00:00Z"), ZoneOffset.UTC);
    private static final LocalDate TODAY = LocalDate.now(FIXED_CLOCK);

    @Mock BookRepository bookRepository;
    @Mock BookFeignClient bookFeignClient;

    BookService bookService;

    @BeforeEach
    void setUp() {
        bookService = new BookService(bookRepository, bookFeignClient, FIXED_CLOCK);
    }

    // -------------------------------------------------------------------------
    // checkoutBook
    // -------------------------------------------------------------------------

    @Test
    void checkoutBook_success_decrementsAndCreatesCheckout() {
        Book book = bookWithCopies(2);
        given(bookRepository.findById(1L)).willReturn(Optional.of(book));
        given(bookFeignClient.findByUserEmailAndBookId("user@test.com", 1L, TOKEN)).willReturn(null);
        given(bookFeignClient.findBooksByUserEmail("user@test.com", TOKEN)).willReturn(List.of());
        given(bookFeignClient.findPaymentByUserEmail("user@test.com", TOKEN)).willReturn(null);

        bookService.checkoutBook("user@test.com", 1L, TOKEN);

        // copies decremented
        ArgumentCaptor<Book> bookCaptor = ArgumentCaptor.forClass(Book.class);
        verify(bookRepository).save(bookCaptor.capture());
        assertThat(bookCaptor.getValue().getCopiesAvailable()).isEqualTo(1);

        // payment record created for first-time user
        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(bookFeignClient).savePayment(paymentCaptor.capture(), org.mockito.ArgumentMatchers.eq(TOKEN));
        assertThat(paymentCaptor.getValue().getAmount()).isEqualTo(0.0);
        assertThat(paymentCaptor.getValue().getUserEmail()).isEqualTo("user@test.com");

        // checkout persisted
        ArgumentCaptor<Checkout> checkoutCaptor = ArgumentCaptor.forClass(Checkout.class);
        verify(bookFeignClient).saveCheckout(checkoutCaptor.capture(), org.mockito.ArgumentMatchers.eq(TOKEN));
        assertThat(checkoutCaptor.getValue().getUserEmail()).isEqualTo("user@test.com");
        assertThat(checkoutCaptor.getValue().getBookId()).isEqualTo(1L);
    }

    @Test
    void checkoutBook_noAvailableCopies_throwsBookException() {
        Book book = bookWithCopies(0);
        given(bookRepository.findById(1L)).willReturn(Optional.of(book));
        given(bookFeignClient.findByUserEmailAndBookId("user@test.com", 1L, TOKEN)).willReturn(null);

        assertThatThrownBy(() -> bookService.checkoutBook("user@test.com", 1L, TOKEN))
                .isInstanceOf(BookException.class);

        verify(bookRepository, never()).save(any());
        verify(bookFeignClient, never()).saveCheckout(any(), any());
    }

    @Test
    void checkoutBook_alreadyCheckedOut_throwsBookException() {
        Book book = bookWithCopies(2);
        given(bookRepository.findById(1L)).willReturn(Optional.of(book));
        given(bookFeignClient.findByUserEmailAndBookId("user@test.com", 1L, TOKEN))
                .willReturn(new Checkout()); // existing checkout

        assertThatThrownBy(() -> bookService.checkoutBook("user@test.com", 1L, TOKEN))
                .isInstanceOf(BookException.class);

        verify(bookRepository, never()).save(any());
    }

    @Test
    void checkoutBook_existingLoanNotOverdue_succeeds() {
        Book book = bookWithCopies(2);
        given(bookRepository.findById(1L)).willReturn(Optional.of(book));
        given(bookFeignClient.findByUserEmailAndBookId("user@test.com", 1L, TOKEN)).willReturn(null);

        // existing checkout that is still within the return window
        Checkout notOverdue = new Checkout("user@test.com",
                TODAY.minusDays(3).toString(),
                TODAY.plusDays(4).toString(), // future date → lambda returns false
                2L);
        given(bookFeignClient.findBooksByUserEmail("user@test.com", TOKEN)).willReturn(List.of(notOverdue));
        given(bookFeignClient.findPaymentByUserEmail("user@test.com", TOKEN)).willReturn(null);

        bookService.checkoutBook("user@test.com", 1L, TOKEN);

        verify(bookRepository).save(any());
    }

    @Test
    void checkoutBook_outstandingFees_throwsPaymentException() {
        Book book = bookWithCopies(2);
        given(bookRepository.findById(1L)).willReturn(Optional.of(book));
        given(bookFeignClient.findByUserEmailAndBookId("user@test.com", 1L, TOKEN)).willReturn(null);
        given(bookFeignClient.findBooksByUserEmail("user@test.com", TOKEN)).willReturn(List.of());

        Payment payment = new Payment();
        payment.setAmount(9.99);
        payment.setUserEmail("user@test.com");
        given(bookFeignClient.findPaymentByUserEmail("user@test.com", TOKEN)).willReturn(payment);

        assertThatThrownBy(() -> bookService.checkoutBook("user@test.com", 1L, TOKEN))
                .isInstanceOf(PaymentException.class)
                .hasMessageContaining("Outstanding fees");

        verify(bookRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // returnBook
    // -------------------------------------------------------------------------

    @Test
    void returnBook_success_incrementsCopiesAndRecordsHistory() {
        Book book = bookWithCopies(0);
        book.setTitle("Clean Code");
        book.setAuthor("Robert Martin");

        Checkout checkout = new Checkout("user@test.com",
                TODAY.minusDays(3).toString(),
                TODAY.plusDays(4).toString(), // not overdue
                1L);
        checkout.setId(10L);

        given(bookRepository.findById(1L)).willReturn(Optional.of(book));
        given(bookFeignClient.findByUserEmailAndBookId("user@test.com", 1L, TOKEN)).willReturn(checkout);

        bookService.returnBook("user@test.com", 1L, TOKEN);

        // copies incremented
        ArgumentCaptor<Book> bookCaptor = ArgumentCaptor.forClass(Book.class);
        verify(bookRepository).save(bookCaptor.capture());
        assertThat(bookCaptor.getValue().getCopiesAvailable()).isEqualTo(1);

        // checkout removed
        verify(bookFeignClient).deleteCheckoutById(10L, TOKEN);

        // history recorded
        ArgumentCaptor<com.onlinelibrary.book.entity.History> historyCaptor =
                ArgumentCaptor.forClass(com.onlinelibrary.book.entity.History.class);
        verify(bookFeignClient).saveHistory(historyCaptor.capture(), org.mockito.ArgumentMatchers.eq(TOKEN));
        assertThat(historyCaptor.getValue().getUserEmail()).isEqualTo("user@test.com");
        assertThat(historyCaptor.getValue().getTitle()).isEqualTo("Clean Code");

        // no late fee applied
        verify(bookFeignClient, never()).savePayment(any(), any());
    }

    @Test
    void returnBook_overdue_addsLateFeeToPayment() {
        Book book = bookWithCopies(0);

        Checkout checkout = new Checkout("user@test.com",
                TODAY.minusDays(10).toString(),
                TODAY.minusDays(3).toString(), // 3 days overdue
                1L);
        checkout.setId(11L);

        Payment payment = new Payment();
        payment.setAmount(0.0);
        payment.setUserEmail("user@test.com");

        given(bookRepository.findById(1L)).willReturn(Optional.of(book));
        given(bookFeignClient.findByUserEmailAndBookId("user@test.com", 1L, TOKEN)).willReturn(checkout);
        given(bookFeignClient.findPaymentByUserEmail("user@test.com", TOKEN)).willReturn(payment);

        bookService.returnBook("user@test.com", 1L, TOKEN);

        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(bookFeignClient).savePayment(paymentCaptor.capture(), org.mockito.ArgumentMatchers.eq(TOKEN));
        assertThat(paymentCaptor.getValue().getAmount()).isEqualTo(3.0); // 3 days late
    }

    @Test
    void returnBook_notCheckedOut_throwsBookException() {
        given(bookRepository.findById(1L)).willReturn(Optional.of(bookWithCopies(1)));
        given(bookFeignClient.findByUserEmailAndBookId("user@test.com", 1L, TOKEN)).willReturn(null);

        assertThatThrownBy(() -> bookService.returnBook("user@test.com", 1L, TOKEN))
                .isInstanceOf(BookException.class);

        verify(bookRepository, never()).save(any());
        verify(bookFeignClient, never()).deleteCheckoutById(any(), any());
    }

    // -------------------------------------------------------------------------
    // renewLoan
    // -------------------------------------------------------------------------

    @Test
    void renewLoan_success_extendsReturnDateBySevenDays() {
        String originalReturnDate = TODAY.plusDays(2).toString();
        Checkout checkout = new Checkout("user@test.com", TODAY.minusDays(5).toString(),
                originalReturnDate, 1L);

        given(bookFeignClient.findByUserEmailAndBookId("user@test.com", 1L, TOKEN)).willReturn(checkout);

        bookService.renewLoan("user@test.com", 1L, TOKEN);

        ArgumentCaptor<Checkout> captor = ArgumentCaptor.forClass(Checkout.class);
        verify(bookFeignClient).saveCheckout(captor.capture(), org.mockito.ArgumentMatchers.eq(TOKEN));
        assertThat(LocalDate.parse(captor.getValue().getReturnDate()))
                .isEqualTo(TODAY.plusDays(7));
    }

    @Test
    void renewLoan_alreadyExpired_throwsBookException() {
        Checkout checkout = new Checkout("user@test.com",
                TODAY.minusDays(10).toString(),
                TODAY.minusDays(1).toString(), // overdue
                1L);

        given(bookFeignClient.findByUserEmailAndBookId("user@test.com", 1L, TOKEN)).willReturn(checkout);

        assertThatThrownBy(() -> bookService.renewLoan("user@test.com", 1L, TOKEN))
                .isInstanceOf(BookException.class)
                .hasMessageContaining("overdue");

        verify(bookFeignClient, never()).saveCheckout(any(), any());
    }

    @Test
    void renewLoan_notCheckedOut_throwsBookException() {
        given(bookFeignClient.findByUserEmailAndBookId("user@test.com", 1L, TOKEN)).willReturn(null);

        assertThatThrownBy(() -> bookService.renewLoan("user@test.com", 1L, TOKEN))
                .isInstanceOf(BookException.class);
    }

    // -------------------------------------------------------------------------
    // checkoutBookByUser
    // -------------------------------------------------------------------------

    @Test
    void checkoutBookByUser_existingCheckout_returnsTrue() {
        given(bookFeignClient.findByUserEmailAndBookId("user@test.com", 1L, TOKEN))
                .willReturn(new Checkout());

        assertThat(bookService.checkoutBookByUser("user@test.com", 1L, TOKEN)).isTrue();
    }

    @Test
    void checkoutBookByUser_noCheckout_returnsFalse() {
        given(bookFeignClient.findByUserEmailAndBookId("user@test.com", 1L, TOKEN)).willReturn(null);

        assertThat(bookService.checkoutBookByUser("user@test.com", 1L, TOKEN)).isFalse();
    }

    // -------------------------------------------------------------------------
    // currentLoansCount
    // -------------------------------------------------------------------------

    @Test
    void currentLoansCount_returnsNumberOfActiveCheckouts() {
        given(bookFeignClient.findBooksByUserEmail("user@test.com", TOKEN))
                .willReturn(List.of(new Checkout(), new Checkout()));

        assertThat(bookService.currentLoansCount("user@test.com", TOKEN)).isEqualTo(2);
    }

    @Test
    void currentLoansCount_noCheckouts_returnsZero() {
        given(bookFeignClient.findBooksByUserEmail("user@test.com", TOKEN)).willReturn(List.of());

        assertThat(bookService.currentLoansCount("user@test.com", TOKEN)).isZero();
    }

    // -------------------------------------------------------------------------
    // currentLoans
    // -------------------------------------------------------------------------

    @Test
    void currentLoans_returnsDaysRemainingPerBook() {
        String returnDate = TODAY.plusDays(5).toString();
        Checkout checkout = new Checkout("user@test.com", TODAY.toString(), returnDate, 1L);

        Book book = bookWithCopies(0);
        book.setTitle("Effective Java");

        given(bookFeignClient.findBooksByUserEmail("user@test.com", TOKEN)).willReturn(List.of(checkout));
        given(bookRepository.findBooksByBookIds(List.of(1L))).willReturn(List.of(book));

        List<ShelfCurrentLoansResponse> result = bookService.currentLoans("user@test.com", TOKEN);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getDaysLeft()).isEqualTo(5);
        assertThat(result.getFirst().getBook().getTitle()).isEqualTo("Effective Java");
    }

    @Test
    void currentLoans_noActiveCheckouts_returnsEmpty() {
        given(bookFeignClient.findBooksByUserEmail("user@test.com", TOKEN)).willReturn(List.of());

        List<ShelfCurrentLoansResponse> result = bookService.currentLoans("user@test.com", TOKEN);

        assertThat(result).isEmpty();
        verify(bookRepository).findBooksByBookIds(List.of());
    }

    // -------------------------------------------------------------------------
    // deleteBookById
    // -------------------------------------------------------------------------

    @Test
    void deleteBookById_bookExists_callsDeleteById() {
        given(bookRepository.findById(1L)).willReturn(Optional.of(bookWithCopies(1)));

        bookService.deleteBookById(1L);

        verify(bookRepository).deleteById(1L);
    }

    @Test
    void deleteBookById_bookNotFound_doesNothing() {
        given(bookRepository.findById(99L)).willReturn(Optional.empty());

        bookService.deleteBookById(99L);

        verify(bookRepository, never()).deleteById(any());
    }

    // -------------------------------------------------------------------------
    // helpers
    // -------------------------------------------------------------------------

    private Book bookWithCopies(int copiesAvailable) {
        Book book = new Book();
        book.setId(1L);
        book.setCopies(5);
        book.setCopiesAvailable(copiesAvailable);
        return book;
    }
}
