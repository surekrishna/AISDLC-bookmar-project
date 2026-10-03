package com.krish.bookmarks.service;

import java.util.List;

import org.springframework.validation.BindingResult;

public class BookmarkValidationException extends RuntimeException {

	private final List<FieldViolation> violations;

	public BookmarkValidationException(List<FieldViolation> violations) {
		super("Bookmark validation failed");
		this.violations = List.copyOf(violations);
	}

	public List<FieldViolation> getViolations() {
		return violations;
	}

	public void applyTo(BindingResult bindingResult) {
		for (FieldViolation violation : violations) {
			bindingResult.rejectValue(violation.field(), null, violation.message());
		}
	}

	public record FieldViolation(String field, String message) {
	}
}

