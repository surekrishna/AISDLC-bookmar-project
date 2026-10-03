package com.krish.bookmarks;

import java.time.Clock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class BookmarkManagerApplication {

	@Bean
	Clock clock() {
		return Clock.systemUTC();
	}

	public static void main(String[] args) {
		SpringApplication.run(BookmarkManagerApplication.class, args);
		System.out.println("Bookmark Manager Application started successfully...");
	}
}

