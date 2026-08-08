package com.onlinelibrary.book.repository;

import com.onlinelibrary.book.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;
import org.springframework.security.access.prepost.PreAuthorize;

@RepositoryRestResource
public interface MessageRepository extends JpaRepository<Message, Long> {

    @RestResource
    @PreAuthorize("isAuthenticated() && (hasAuthority('admin') "
            + "|| #userEmail == authentication.principal.getClaimAsString('email'))")
    Page<Message> findByUserEmail(@Param("userEmail") String userEmail, Pageable pageable);

    @RestResource
    @PreAuthorize("hasAuthority('admin')")
    Page<Message> findByClosed(boolean closed, Pageable pageable);

}
