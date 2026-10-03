package com.krish.bookmarks.web;

import com.krish.bookmarks.service.BookmarkNotFoundException;

import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice
public class BookmarkNotFoundAdvice {

	@ExceptionHandler(BookmarkNotFoundException.class)
	@ResponseStatus(HttpStatus.NOT_FOUND)
	public String handleBookmarkNotFound(BookmarkNotFoundException ex, Model model) {
		model.addAttribute("message", ex.getMessage());
		return "bookmark-not-found";
	}
}

