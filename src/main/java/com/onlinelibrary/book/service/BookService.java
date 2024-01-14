package com.onlinelibrary.book.service;

import com.onlinelibrary.book.entity.Book;
import com.onlinelibrary.book.entity.Checkout;
import com.onlinelibrary.book.entity.History;
import com.onlinelibrary.book.entity.Payment;
import com.onlinelibrary.book.event.service.KafkaProducerService;
import com.onlinelibrary.book.exception.BookException;
import com.onlinelibrary.book.exception.PaymentException;
import com.onlinelibrary.book.repository.BookRepository;
import com.onlinelibrary.book.responsemodels.ShelfCurrentLoansResponse;
import com.onlinelibrary.book.service.client.BookFeignClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
@Transactional
public class BookService {
    private static final Logger logger = LoggerFactory.getLogger(BookService.class);
    public static final String YYYY_MM_DD = "yyyy-MM-dd";
    private final BookRepository bookRepository;
    private final BookFeignClient bookFeignClient;
    private final KafkaProducerService kafkaProducerService;


    public BookService(BookRepository bookRepository,
                       BookFeignClient bookFeignClient, KafkaProducerService kafkaProducerService) {
        this.bookRepository = bookRepository;
        this.bookFeignClient = bookFeignClient;
        this.kafkaProducerService = kafkaProducerService;
    }

    public Book checkoutBook(String token, String userEmail, Long bookId) throws Exception {
        logger.debug("Checkout Book");

        Optional<Book> book = bookRepository.findById(bookId);

        Checkout validateCheckout = bookFeignClient.findByUserEmailAndBookId(token, userEmail, bookId);

        if (book.isEmpty() || validateCheckout != null || book.get().getCopiesAvailable() <= 0) {
            throw new BookException("Book doesn't exist or already checked out by user");
        }

        List<Checkout> currentBooksCheckedOut = bookFeignClient.findBooksByUserEmail(token, userEmail);

        SimpleDateFormat sdf = new SimpleDateFormat(YYYY_MM_DD);

        boolean bookNeedsReturned = false;

        for (Checkout checkout : currentBooksCheckedOut) {
            Date d1 = sdf.parse(checkout.getReturnDate());
            Date d2 = sdf.parse(LocalDate.now().toString());

            TimeUnit time = TimeUnit.DAYS;

            double differenceInTime = time.convert(d1.getTime() - d2.getTime(), TimeUnit.MILLISECONDS);

            if (differenceInTime < 0) {
                bookNeedsReturned = true;
                break;
            }
        }

        Payment userPayment = bookFeignClient.findPaymentByUserEmail(token, userEmail);

        if ((userPayment != null && userPayment.getAmount() > 0) || (userPayment != null && bookNeedsReturned)) {
            throw new PaymentException("Outstanding fees");
        }

        if (userPayment == null) {
            Payment payment = new Payment();
            payment.setAmount(00.00);
            payment.setUserEmail(userEmail);
            bookFeignClient.savePayment(token, payment);
        }

        book.get().setCopiesAvailable(book.get().getCopiesAvailable() - 1);
        bookRepository.save(book.get());

        Checkout checkout = new Checkout(
                userEmail,
                LocalDate.now().toString(),
                LocalDate.now().plusDays(7).toString(),
                book.get().getId()
        );

        bookFeignClient.saveCheckout(token, checkout);

        return book.get();
    }

    public Boolean checkoutBookByUser(String token, String userEmail, Long bookId) {
        logger.debug("Checkout Book by User");
        Checkout validateCheckout = bookFeignClient.findByUserEmailAndBookId(token, userEmail, bookId);
        return validateCheckout != null;
    }

    public int currentLoansCount(String token, String userEmail) {
        logger.debug("Getting current Loans Count");
        return bookFeignClient.findBooksByUserEmail(token, userEmail).size();
    }

    public List<ShelfCurrentLoansResponse> currentLoans(String token, String userEmail) throws ParseException {
        logger.debug("Getting Shelf");

        List<ShelfCurrentLoansResponse> shelfCurrentLoansResponses = new ArrayList<>();

        List<Checkout> checkoutList = bookFeignClient.findBooksByUserEmail(token, userEmail);
        List<Long> bookIdList = new ArrayList<>();

        for (Checkout i : checkoutList) {
            bookIdList.add(i.getBookId());
        }

        List<Book> books = bookRepository.findBooksByBookIds(bookIdList);

        SimpleDateFormat sdf = new SimpleDateFormat(YYYY_MM_DD);

        for (Book book : books) {
            Optional<Checkout> checkout = checkoutList.stream()
                    .filter(x -> x.getBookId().equals(book.getId())).findFirst();

            if (checkout.isPresent()) {

                Date d1 = sdf.parse(checkout.get().getReturnDate());
                Date d2 = sdf.parse(LocalDate.now().toString());

                TimeUnit time = TimeUnit.DAYS;

                long differenceInTime = time.convert(d1.getTime() - d2.getTime(),
                        TimeUnit.MILLISECONDS);

                shelfCurrentLoansResponses.add(new ShelfCurrentLoansResponse(book, (int) differenceInTime));
            }
        }
        return shelfCurrentLoansResponses;
    }

    public void returnBook(String token, String userEmail, Long bookId) throws Exception {
        logger.debug("Returning book");

        Optional<Book> book = bookRepository.findById(bookId);

        Checkout validateCheckout = bookFeignClient.findByUserEmailAndBookId(token, userEmail, bookId);

        if (book.isEmpty() || validateCheckout == null) {
            throw new BookException("Book does not exist or not checked out by user");
        }

        book.get().setCopiesAvailable(book.get().getCopiesAvailable() + 1);

        bookRepository.save(book.get());

        SimpleDateFormat sdf = new SimpleDateFormat(YYYY_MM_DD);

        Date d1 = sdf.parse(validateCheckout.getReturnDate());
        Date d2 = sdf.parse(LocalDate.now().toString());

        TimeUnit time = TimeUnit.DAYS;

        double differenceInTime = time.convert(d1.getTime() - d2.getTime(), TimeUnit.MILLISECONDS);

        if (differenceInTime < 0) {
            Payment payment = bookFeignClient.findPaymentByUserEmail(token, userEmail);

            payment.setAmount(payment.getAmount() + (differenceInTime * -1));
            bookFeignClient.savePayment(token, payment);
        }

        bookFeignClient.deleteCheckoutById(token, validateCheckout.getId());

        History history = new History(
                userEmail,
                validateCheckout.getCheckoutDate(),
                LocalDate.now().toString(),
                book.get().getTitle(),
                book.get().getAuthor(),
                book.get().getDescription(),
                book.get().getImg()
        );

        bookFeignClient.saveHistory(token, history);
    }

    public void renewLoan(String token, String userEmail, Long bookId) throws Exception {
        logger.debug("Renew Load");

        Checkout validateCheckout = bookFeignClient.findByUserEmailAndBookId(token, userEmail, bookId);

        if (validateCheckout == null) {
            throw new BookException("Book does not exist or not checked out by user");
        }

        SimpleDateFormat sdFormat = new SimpleDateFormat(YYYY_MM_DD);

        Date d1 = sdFormat.parse(validateCheckout.getReturnDate());
        Date d2 = sdFormat.parse(LocalDate.now().toString());

        if (d1.compareTo(d2) > 0 || d1.compareTo(d2) == 0) {
            validateCheckout.setReturnDate(LocalDate.now().plusDays(7).toString());
            bookFeignClient.saveCheckout(token, validateCheckout);
        }
    }

    public void deleteBookById(Long bookId) {
        logger.debug("Deleting book by ID");

        Optional<Book> opt = bookRepository.findById(bookId);
        if (opt.isPresent()) {
            bookRepository.deleteById(bookId);
            kafkaProducerService.publishBookChange("DELETE", opt.get());
        }

    }
}
