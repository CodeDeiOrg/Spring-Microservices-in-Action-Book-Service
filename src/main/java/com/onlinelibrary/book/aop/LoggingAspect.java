package com.onlinelibrary.book.aop;

import com.onlinelibrary.book.entity.Book;
import com.onlinelibrary.book.event.service.KafkaProducerService;
import com.onlinelibrary.book.repository.BookRepository;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Aspect
@Component
public class LoggingAspect {
    private static final Logger logger = LoggerFactory.getLogger(LoggingAspect.class);

    private final KafkaProducerService kafkaProducerService;
    private final BookRepository bookRepository;

    public LoggingAspect(KafkaProducerService kafkaProducerService, BookRepository bookRepository) {
        this.kafkaProducerService = kafkaProducerService;
        this.bookRepository = bookRepository;
    }

    @AfterReturning(value = "execution(* com.onlinelibrary.book.repository.BookRepository.save(..))", returning = "book")
    public void afterSave(Book book) {
        logger.info("Book saved: id={}, title={}, copiesAvailable={}", book.getId(), book.getTitle(), book.getCopiesAvailable());
        kafkaProducerService.publishBookChange("UPDATE", book);
    }

    @Around("execution(* com.onlinelibrary.book.repository.BookRepository.deleteById(..)) && args(bookId)")
    public Object aroundDelete(ProceedingJoinPoint pjp, Long bookId) throws Throwable {
        Optional<Book> book = bookRepository.findById(bookId);
        Object result = pjp.proceed();
        book.ifPresentOrElse(
                b -> {
                    logger.info("Book deleted: id={}, title={}", b.getId(), b.getTitle());
                    kafkaProducerService.publishBookChange("DELETE", b);
                },
                () -> logger.warn("deleteById called for non-existent book id={}", bookId)
        );
        return result;
    }
}
