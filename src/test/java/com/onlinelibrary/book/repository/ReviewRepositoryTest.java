package com.onlinelibrary.book.repository;

import com.onlinelibrary.book.entity.Review;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class ReviewRepositoryTest {

    @Container
    @ServiceConnection
    static MySQLContainer mysql = new MySQLContainer("mysql:8");

    @Autowired
    ReviewRepository reviewRepository;

    @BeforeEach
    void setUp() {
        reviewRepository.deleteAll();

        Review r1 = new Review();
        r1.setUserEmail("alice@test.com");
        r1.setBookId(1L);
        r1.setRating(4.0);

        Review r2 = new Review();
        r2.setUserEmail("bob@test.com");
        r2.setBookId(1L);
        r2.setRating(3.0);

        Review r3 = new Review();
        r3.setUserEmail("alice@test.com");
        r3.setBookId(2L);
        r3.setRating(5.0);

        reviewRepository.save(r1);
        reviewRepository.save(r2);
        reviewRepository.save(r3);
    }

    @Test
    void findByBookId_returnsOnlyReviewsForThatBook() {
        Page<Review> result = reviewRepository.findByBookId(1L, PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent())
                .extracting(Review::getUserEmail)
                .containsExactlyInAnyOrder("alice@test.com", "bob@test.com");
    }

    @Test
    void findByBookId_differentBook_returnsOnlyItsReviews() {
        Page<Review> result = reviewRepository.findByBookId(2L, PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().getFirst().getUserEmail()).isEqualTo("alice@test.com");
    }

    @Test
    void findByUserEmailAndBookId_returnsCorrectReview() {
        Review result = reviewRepository.findByUserEmailAndBookId("alice@test.com", 1L);

        assertThat(result).isNotNull();
        assertThat(result.getRating()).isEqualTo(4.0);
    }

    @Test
    void findByUserEmailAndBookId_noMatch_returnsNull() {
        Review result = reviewRepository.findByUserEmailAndBookId("nobody@test.com", 1L);

        assertThat(result).isNull();
    }

    @Test
    void deleteAllByBookId_removesOnlyReviewsForThatBook() {
        reviewRepository.deleteAllByBookId(1L);

        assertThat(reviewRepository.findByBookId(1L, PageRequest.of(0, 10)).getTotalElements()).isZero();
        assertThat(reviewRepository.findByBookId(2L, PageRequest.of(0, 10)).getTotalElements()).isEqualTo(1);
    }
}