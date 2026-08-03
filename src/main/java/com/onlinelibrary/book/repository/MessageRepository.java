package com.onlinelibrary.book.repository;

import com.onlinelibrary.book.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
public interface MessageRepository extends JpaRepository<Message, Long> {

    Page<Message> findByUserEmail(String userEmail, Pageable pageable);

    Page<Message> findByClosed(boolean closed, Pageable pageable);

}
