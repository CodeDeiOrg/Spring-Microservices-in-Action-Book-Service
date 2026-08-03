package com.onlinelibrary.book.config;

import com.onlinelibrary.book.entity.Book;
import com.onlinelibrary.book.entity.Message;
import com.onlinelibrary.book.entity.Review;
import org.jspecify.annotations.NonNull;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurer;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

@Configuration
public class MyDataRestConfig implements RepositoryRestConfigurer {

    @Override
    public void configureRepositoryRestConfiguration(RepositoryRestConfiguration config, @NonNull CorsRegistry cors) {
        config.exposeIdsFor(Book.class);
        config.exposeIdsFor(Review.class);
        config.exposeIdsFor(Message.class);
    }
}
