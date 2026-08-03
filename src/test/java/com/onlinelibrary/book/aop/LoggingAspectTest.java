package com.onlinelibrary.book.aop;

import com.onlinelibrary.book.entity.Book;
import com.onlinelibrary.book.event.service.KafkaProducerService;
import com.onlinelibrary.book.repository.BookRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@SpringBootTest
@Transactional
class LoggingAspectTest {

    @Autowired
    BookRepository bookRepository;

    @MockitoBean
    KafkaProducerService kafkaProducerService;

    @Test
    void save_publishesUpdateEventWithFullBookData() {
        Book book = new Book();
        book.setTitle("Test Book");
        book.setAuthor("Test Author");
        book.setCopies(5);
        book.setCopiesAvailable(5);

        Book saved = bookRepository.save(book);

        verify(kafkaProducerService).publishBookChange("UPDATE", saved);
    }

    @Test
    void deleteById_publishesDeleteEventWithFullBookData() {
        Book book = new Book();
        book.setTitle("To Delete");
        book.setAuthor("Author");
        book.setCopies(1);
        book.setCopiesAvailable(1);
        Book saved = bookRepository.save(book);

        // Clear the UPDATE event triggered by the save above
        Mockito.clearInvocations(kafkaProducerService);

        bookRepository.deleteById(saved.getId());

        ArgumentCaptor<Book> captor = ArgumentCaptor.forClass(Book.class);
        verify(kafkaProducerService).publishBookChange(eq("DELETE"), captor.capture());

        assertThat(captor.getValue().getId()).isEqualTo(saved.getId());
        assertThat(captor.getValue().getTitle()).isEqualTo("To Delete");
    }

    @Test
    void deleteById_nonExistentBook_doesNotPublishEvent() {
        bookRepository.deleteById(999L);

        verifyNoInteractions(kafkaProducerService);
    }
}