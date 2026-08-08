package com.onlinelibrary.book.repository;

import com.onlinelibrary.book.entity.Book;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;

import java.util.List;
import java.util.Optional;

@RepositoryRestResource
public interface BookRepository extends JpaRepository<Book, Long> {

    @Override
    @RestResource
    @NonNull
    Page<Book> findAll(@NonNull Pageable pageable);

    @Override
    @RestResource
    @NonNull
    Optional<Book> findById(@NonNull Long id);

    @RestResource
    Page<Book> findByTitleContaining(String title, Pageable pageable);

    @RestResource
    Page<Book> findByCategory(String category, Pageable pageable);

    @Query("select o from Book o where o.id in :book_ids")
    List<Book> findBooksByBookIds (@Param("book_ids") List<Long> bookId);
}
