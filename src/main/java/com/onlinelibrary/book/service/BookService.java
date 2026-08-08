package com.onlinelibrary.book.service;

import com.onlinelibrary.book.entity.Book;
import com.onlinelibrary.book.entity.Checkout;
import com.onlinelibrary.book.entity.History;
import com.onlinelibrary.book.entity.Payment;
import com.onlinelibrary.book.exception.BookException;
import com.onlinelibrary.book.exception.PaymentException;
import com.onlinelibrary.book.repository.BookRepository;
import com.onlinelibrary.book.responsemodels.ShelfCurrentLoansResponse;
import com.onlinelibrary.book.service.client.BookFeignClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class BookService {
    private static final Logger logger = LoggerFactory.getLogger(BookService.class);
    private static final double LATE_FEE_PER_DAY = 1.0;
    private final BookRepository bookRepository;
    private final BookFeignClient bookFeignClient;
    private final Clock clock;

    public BookService(BookRepository bookRepository,
                       BookFeignClient bookFeignClient,
                       Clock clock) {
        this.bookRepository = bookRepository;
        this.bookFeignClient = bookFeignClient;
        this.clock = clock;
    }

    public Book checkoutBook(String userEmail, Long bookId, String token) {
        logger.debug("Checkout Book");

        Optional<Book> book = bookRepository.findById(bookId);
        Checkout validateCheckout = bookFeignClient.findByUserEmailAndBookId(userEmail, bookId, token);

        if (book.isEmpty() || validateCheckout != null || book.get().getCopiesAvailable() <= 0) {
            throw new BookException("Book doesn't exist or already checked out by user");
        }

        List<Checkout> currentBooksCheckedOut = bookFeignClient.findBooksByUserEmail(userEmail, token);

        Optional<Checkout> overdueCheckout = currentBooksCheckedOut.stream()
                .filter(c -> LocalDate.parse(c.getReturnDate()).isBefore(LocalDate.now(clock)))
                .findFirst();

        Payment userPayment = bookFeignClient.findPaymentByUserEmail(userEmail, token);

        if (userPayment != null && (userPayment.getAmount() > 0 || overdueCheckout.isPresent())) {
            if (overdueCheckout.isPresent()) {
                Checkout overdue = overdueCheckout.get();
                String overdueTitle = bookRepository.findById(overdue.getBookId())
                        .map(Book::getTitle)
                        .orElse("a book");
                throw new PaymentException("You have an overdue book. Please return it before checking out more books.",
                        overdueTitle, overdue.getReturnDate());
            }
            throw new PaymentException("Outstanding fees");
        }

        if (userPayment == null) {
            Payment payment = new Payment();
            payment.setAmount(0.00);
            payment.setUserEmail(userEmail);
            bookFeignClient.savePayment(payment, token);
        }

        book.get().setCopiesAvailable(book.get().getCopiesAvailable() - 1);
        bookRepository.save(book.get());

        bookFeignClient.saveCheckout(new Checkout(
                userEmail,
                LocalDate.now(clock).toString(),
                LocalDate.now(clock).plusDays(7).toString(),
                book.get().getId()
        ), token);

        return book.get();
    }

    public Boolean checkoutBookByUser(String userEmail, Long bookId, String token) {
        logger.debug("Checkout Book by User");
        return bookFeignClient.findByUserEmailAndBookId(userEmail, bookId, token) != null;
    }

    public int currentLoansCount(String userEmail, String token) {
        logger.debug("Getting current Loans Count");
        return bookFeignClient.findBooksByUserEmail(userEmail, token).size();
    }

    public List<ShelfCurrentLoansResponse> currentLoans(String userEmail, String token) {
        logger.debug("Getting Shelf");

        List<Checkout> checkoutList = bookFeignClient.findBooksByUserEmail(userEmail, token);

        List<Long> bookIdList = new ArrayList<>();
        for (Checkout c : checkoutList) {
            bookIdList.add(c.getBookId());
        }

        List<Book> books = bookRepository.findBooksByBookIds(bookIdList);
        List<ShelfCurrentLoansResponse> shelfCurrentLoansResponses = new ArrayList<>();

        for (Book book : books) {
            Optional<Checkout> checkout = checkoutList.stream()
                    .filter(c -> c.getBookId().equals(book.getId()))
                    .findFirst();

            if (checkout.isPresent()) {
                long daysLeft = ChronoUnit.DAYS.between(
                        LocalDate.now(clock),
                        LocalDate.parse(checkout.get().getReturnDate())
                );
                shelfCurrentLoansResponses.add(new ShelfCurrentLoansResponse(book, (int) daysLeft));
            }
        }
        return shelfCurrentLoansResponses;
    }

    public void returnBook(String userEmail, Long bookId, String token) {
        logger.debug("Returning book");

        Optional<Book> book = bookRepository.findById(bookId);
        Checkout validateCheckout = bookFeignClient.findByUserEmailAndBookId(userEmail, bookId, token);

        if (book.isEmpty() || validateCheckout == null) {
            throw new BookException("Book does not exist or not checked out by user");
        }

        book.get().setCopiesAvailable(book.get().getCopiesAvailable() + 1);
        bookRepository.save(book.get());

        long daysLeft = ChronoUnit.DAYS.between(
                LocalDate.now(clock),
                LocalDate.parse(validateCheckout.getReturnDate())
        );

        if (daysLeft < 0) {
            Payment payment = bookFeignClient.findPaymentByUserEmail(userEmail, token);
            payment.setAmount(payment.getAmount() + Math.abs(daysLeft) * LATE_FEE_PER_DAY);
            bookFeignClient.savePayment(payment, token);
        }

        bookFeignClient.deleteCheckoutById(validateCheckout.getId(), token);

        History history = new History(
                userEmail,
                validateCheckout.getCheckoutDate(),
                LocalDate.now(clock).toString(),
                book.get().getTitle(),
                book.get().getAuthor(),
                book.get().getDescription(),
                book.get().getImg()
        );
        if (daysLeft < 0) {
            long daysLate = Math.abs(daysLeft);
            history.setDaysLate((int) daysLate);
            history.setLateFee(daysLate * LATE_FEE_PER_DAY);
            history.setFeeSettled(false);
        }
        bookFeignClient.saveHistory(history, token);
    }

    public void renewLoan(String userEmail, Long bookId, String token) {
        logger.debug("Renew Loan");

        Checkout validateCheckout = bookFeignClient.findByUserEmailAndBookId(userEmail, bookId, token);

        if (validateCheckout == null) {
            throw new BookException("Book does not exist or not checked out by user");
        }

        if (LocalDate.parse(validateCheckout.getReturnDate()).isBefore(LocalDate.now(clock))) {
            throw new BookException("Loan cannot be renewed — book is overdue");
        }

        validateCheckout.setReturnDate(LocalDate.now(clock).plusDays(7).toString());
        bookFeignClient.saveCheckout(validateCheckout, token);
    }

    public void deleteBookById(Long bookId) {
        logger.debug("Deleting book by ID");

        Optional<Book> opt = bookRepository.findById(bookId);
        if (opt.isPresent()) {
            bookRepository.deleteById(bookId);
        }
    }

    public Book createBook(Book book) {
        logger.debug("Creating book");
        return bookRepository.save(book);
    }
}
