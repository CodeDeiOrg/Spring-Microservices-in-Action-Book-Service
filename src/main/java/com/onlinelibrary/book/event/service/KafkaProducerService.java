package com.onlinelibrary.book.event.service;

import com.onlinelibrary.book.entity.Book;
import com.onlinelibrary.book.event.model.BookChangeModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class KafkaProducerService {
    private static final Logger logger = LoggerFactory.getLogger(KafkaProducerService.class);
    private static final String TOPIC_NAME = "bookTopic";

    private final KafkaTemplate<String, BookChangeModel> kafkaTemplate;

    public KafkaProducerService(KafkaTemplate<String, BookChangeModel> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishBookChange(String action, Book book) {
        logger.debug("Sending Kafka message {} for Book Id: {}", action, book.getId());
        BookChangeModel change = new BookChangeModel(
                BookChangeModel.class.getTypeName(),
                action,
                book);

        kafkaTemplate.send(TOPIC_NAME, change);
    }
}
