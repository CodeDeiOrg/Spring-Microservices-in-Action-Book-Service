package com.onlinelibrary.book.repository;

import com.onlinelibrary.book.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    Page<Review> findByBookId(Long bookId, Pageable pageable);

    Review findByUserEmailAndBookId(String userEmail, Long bookId);

    @Modifying
    @Query("delete from Review where bookId = :book_id")
    void deleteAllByBookId(@Param("book_id") Long bookId);
}
