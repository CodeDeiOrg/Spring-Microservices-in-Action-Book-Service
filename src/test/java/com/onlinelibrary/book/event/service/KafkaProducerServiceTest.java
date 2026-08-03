package com.onlinelibrary.book.event.service;

import com.onlinelibrary.book.entity.Book;
import com.onlinelibrary.book.event.model.BookChangeModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class KafkaProducerServiceTest {

    @Mock
    KafkaTemplate<String, BookChangeModel> kafkaTemplate;

    @InjectMocks
    KafkaProducerService kafkaProducerService;

    @Test
    void publishBookChange_update_sendsToCorrectTopicWithFullBookData() {
        Book book = new Book();
        book.setId(1L);
        book.setTitle("Java Programming");
        book.setAuthor("James Gosling");
        book.setCopiesAvailable(3);

        kafkaProducerService.publishBookChange("UPDATE", book);

        ArgumentCaptor<BookChangeModel> captor = ArgumentCaptor.forClass(BookChangeModel.class);
        verify(kafkaTemplate).send(eq("bookTopic"), captor.capture());

        BookChangeModel sent = captor.getValue();
        assertThat(sent.getAction()).isEqualTo("UPDATE");
        assertThat(sent.getType()).isEqualTo(BookChangeModel.class.getTypeName());
        assertThat(sent.getBook().getId()).isEqualTo(1L);
        assertThat(sent.getBook().getTitle()).isEqualTo("Java Programming");
    }

    @Test
    void publishBookChange_delete_sendsDeleteAction() {
        Book book = new Book();
        book.setId(2L);
        book.setTitle("Spring Boot in Action");

        kafkaProducerService.publishBookChange("DELETE", book);

        ArgumentCaptor<BookChangeModel> captor = ArgumentCaptor.forClass(BookChangeModel.class);
        verify(kafkaTemplate).send(eq("bookTopic"), captor.capture());

        assertThat(captor.getValue().getAction()).isEqualTo("DELETE");
        assertThat(captor.getValue().getBook().getId()).isEqualTo(2L);
    }
}