package com.onlinelibrary.book.aop;

import com.onlinelibrary.book.entity.Book;
import com.onlinelibrary.book.event.service.KafkaProducerService;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class LoggingAspect {
    private final KafkaProducerService kafkaProducerService;

    public LoggingAspect(KafkaProducerService kafkaProducerService) {
        this.kafkaProducerService = kafkaProducerService;
    }

    @AfterReturning(value = "execution(* com.onlinelibrary.book.repository.BookRepository.save(..))", returning = "book")
    public void logAfterUpdate(Book book) {
        // Determine if it's a save or update based on some logic or flag inside Book
        kafkaProducerService.publishBookChange("UPDATE", book);
    }

    @Before("execution(* com.onlinelibrary.book.repository.BookRepository.deleteById(..)) && args(bookId)")
    public void logBeforeDelete(Long bookId) {
        // Create a dummy book or find another way to construct necessary information
        Book dummyBook = new Book();
        dummyBook.setId(bookId);
        kafkaProducerService.publishBookChange("DELETE", dummyBook);
    }
}
