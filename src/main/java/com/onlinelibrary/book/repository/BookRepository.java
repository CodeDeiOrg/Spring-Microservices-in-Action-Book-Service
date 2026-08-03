package com.onlinelibrary.book.repository;

import com.onlinelibrary.book.entity.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BookRepository extends JpaRepository<Book, Long> {

    Page<Book> findByTitleContaining(String title, Pageable pageable);

    Page<Book> findByCategory(String category, Pageable pageable);

    @Query("select o from Book o where o.id in :book_ids")
    List<Book> findBooksByBookIds (@Param("book_ids") List<Long> bookId);
}
