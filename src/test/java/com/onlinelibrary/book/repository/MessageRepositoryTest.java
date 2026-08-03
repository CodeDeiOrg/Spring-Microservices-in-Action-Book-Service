package com.onlinelibrary.book.repository;

import com.onlinelibrary.book.entity.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class MessageRepositoryTest {

    @Container
    @ServiceConnection
    static MySQLContainer mysql = new MySQLContainer("mysql:8");

    @Autowired
    MessageRepository messageRepository;

    @BeforeEach
    void setUp() {
        messageRepository.deleteAll();

        Message m1 = new Message("Issue 1", "Question 1");
        m1.setUserEmail("alice@test.com");
        m1.setClosed(false);

        Message m2 = new Message("Issue 2", "Question 2");
        m2.setUserEmail("alice@test.com");
        m2.setClosed(true);

        Message m3 = new Message("Issue 3", "Question 3");
        m3.setUserEmail("bob@test.com");
        m3.setClosed(true);

        messageRepository.save(m1);
        messageRepository.save(m2);
        messageRepository.save(m3);
    }

    @Test
    void findByUserEmail_returnsOnlyThatUsersMessages() {
        Page<Message> result = messageRepository.findByUserEmail("alice@test.com", PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent())
                .extracting(Message::getUserEmail)
                .containsOnly("alice@test.com");
    }

    @Test
    void findByUserEmail_unknownUser_returnsEmpty() {
        Page<Message> result = messageRepository.findByUserEmail("nobody@test.com", PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isZero();
    }

    @Test
    void findByClosed_true_returnsOnlyClosedMessages() {
        Page<Message> result = messageRepository.findByClosed(true, PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).allMatch(Message::isClosed);
    }

    @Test
    void findByClosed_false_returnsOnlyOpenMessages() {
        Page<Message> result = messageRepository.findByClosed(false, PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).noneMatch(Message::isClosed);
    }
}