package com.onlinelibrary.book.repository;

import com.onlinelibrary.book.entity.Book;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class BookRepositoryTest {

    @Container
    @ServiceConnection
    static MySQLContainer mysql = new MySQLContainer("mysql:8");

    @Autowired
    BookRepository bookRepository;

    private Long javaBookId;
    private Long scienceBookId;

    @BeforeEach
    void setUp() {
        bookRepository.deleteAll();

        Book java = new Book();
        java.setTitle("Java Programming");
        java.setAuthor("James Gosling");
        java.setCategory("Technology");
        java.setCopies(5);
        java.setCopiesAvailable(5);
        javaBookId = bookRepository.save(java).getId();

        Book spring = new Book();
        spring.setTitle("Spring Boot in Action");
        spring.setAuthor("Craig Walls");
        spring.setCategory("Technology");
        spring.setCopies(3);
        spring.setCopiesAvailable(3);
        bookRepository.save(spring);

        Book science = new Book();
        science.setTitle("A Brief History of Time");
        science.setAuthor("Stephen Hawking");
        science.setCategory("Science");
        science.setCopies(2);
        science.setCopiesAvailable(2);
        scienceBookId = bookRepository.save(science).getId();
    }

    // --- findByTitleContaining ---

    @Test
    void findByTitleContaining_matchingBooks_returnsCorrectPage() {
        Page<Book> result = bookRepository.findByTitleContaining("Java", PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().getFirst().getTitle()).isEqualTo("Java Programming");
    }

    @Test
    void findByTitleContaining_partialMatch_returnsBothMatches() {
        Page<Book> result = bookRepository.findByTitleContaining("a", PageRequest.of(0, 10));

        // "Java Programming" and "Spring Boot in Action" and "A Brief History of Time" all contain "a"
        assertThat(result.getTotalElements()).isEqualTo(3);
    }

    @Test
    void findByTitleContaining_noMatch_returnsEmpty() {
        Page<Book> result = bookRepository.findByTitleContaining("nonexistent", PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isZero();
    }

    // --- findByCategory ---

    @Test
    void findByCategory_returnsOnlyBooksInThatCategory() {
        Page<Book> result = bookRepository.findByCategory("Technology", PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent())
                .extracting(Book::getCategory)
                .containsOnly("Technology");
    }

    @Test
    void findByCategory_noMatch_returnsEmpty() {
        Page<Book> result = bookRepository.findByCategory("Fiction", PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isZero();
    }

    // --- findBooksByBookIds ---

    @Test
    void findBooksByBookIds_returnsExactlyRequestedBooks() {
        List<Book> result = bookRepository.findBooksByBookIds(List.of(javaBookId, scienceBookId));

        assertThat(result).hasSize(2);
        assertThat(result)
                .extracting(Book::getId)
                .containsExactlyInAnyOrder(javaBookId, scienceBookId);
    }

    @Test
    void findBooksByBookIds_emptyList_returnsEmpty() {
        List<Book> result = bookRepository.findBooksByBookIds(List.of());

        assertThat(result).isEmpty();
    }
}