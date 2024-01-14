package com.onlinelibrary.book;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.kafka.annotation.EnableKafka;
import reactor.core.publisher.Hooks;


@SpringBootApplication
@EnableFeignClients
@EnableKafka
@RefreshScope
@EnableDiscoveryClient
public class BookServiceApplication {

	public static void main(String[] args) {
		Hooks.enableAutomaticContextPropagation();
		SpringApplication.run(BookServiceApplication.class, args);
	}

}
