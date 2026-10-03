package com.krish.bookmarks.service;

public class BookmarkNotFoundException extends RuntimeException {

	private final Long bookmarkId;

	public BookmarkNotFoundException(Long bookmarkId) {
		super("Bookmark not found.");
		this.bookmarkId = bookmarkId;
	}
}


