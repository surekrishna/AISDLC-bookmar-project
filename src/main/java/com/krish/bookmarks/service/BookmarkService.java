package com.krish.bookmarks.service;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

import com.krish.bookmarks.model.Bookmark;
import com.krish.bookmarks.model.BookmarkTag;
import com.krish.bookmarks.repository.BookmarkRepository;
import com.krish.bookmarks.web.BookmarkForm;

import jakarta.transaction.Transactional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class BookmarkService {

	private static final int MAX_URL_LENGTH = 2048;
	private static final int MAX_TITLE_LENGTH = 200;
	private static final Pattern IPV4_PATTERN = Pattern.compile("^(?:25[0-5]|2[0-4]\\d|1?\\d?\\d)(?:\\.(?:25[0-5]|2[0-4]\\d|1?\\d?\\d)){3}$");

	private final BookmarkRepository bookmarkRepository;
	private final java.time.Clock clock;

	public BookmarkService(BookmarkRepository bookmarkRepository, java.time.Clock clock) {
		this.bookmarkRepository = bookmarkRepository;
		this.clock = clock;
	}

	public Bookmark save(BookmarkForm form) {
		Bookmark bookmark = new Bookmark();
		bookmark.setUrl(trimToNull(form.getUrl()));
		bookmark.setTitle(trimToNull(form.getTitle()));
		bookmark.setCreatedAt(java.time.Instant.now(clock));
		applyValidatedFields(bookmark, form, null, false);
		return persist(bookmark);
	}

	public Bookmark update(Long id, BookmarkForm form) {
		Bookmark existing = bookmarkRepository.findByIdWithTags(id).orElseThrow(() -> new com.krish.bookmarks.service.BookmarkNotFoundException(id));
		Bookmark bookmark = new Bookmark();
		bookmark.setId(existing.getId());
		bookmark.setCreatedAt(existing.getCreatedAt());
		applyValidatedFields(bookmark, form, id, true);
		return persist(bookmark);
	}

	@SuppressWarnings("unused")
	public void delete(Long id) {
		Bookmark bookmark = bookmarkRepository.findByIdWithTags(id).orElseThrow(() -> new com.krish.bookmarks.service.BookmarkNotFoundException(id));
		bookmarkRepository.delete(bookmark);
	}

	public Bookmark getBookmarkForEdit(Long id) {
		return bookmarkRepository.findByIdWithTags(id).orElseThrow(() -> new com.krish.bookmarks.service.BookmarkNotFoundException(id));
	}

	private void applyValidatedFields(Bookmark bookmark, BookmarkForm form, Long existingId, boolean flushAfterTagClear) {
		String trimmedUrl = trimToNull(form.getUrl());
		String trimmedTitle = trimToNull(form.getTitle());
		List<BookmarkValidationException.FieldViolation> violations = validate(trimmedUrl, trimmedTitle, existingId);
		if (!violations.isEmpty()) {
			throw new BookmarkValidationException(violations);
		}

		bookmark.setUrl(trimmedUrl);
		bookmark.setTitle(trimmedTitle);
		if (bookmark.getCreatedAt() == null) {
			bookmark.setCreatedAt(java.time.Instant.now(clock));
		}
		if (flushAfterTagClear) {
			bookmarkRepository.deleteTagsByBookmarkId(bookmark.getId());
			bookmark.setTags(new java.util.ArrayList<>());
		} else {
			bookmark.getTags().clear();
		}
		normalizeTags(form.getTags()).forEach(bookmark::addTag);
	}

	private List<BookmarkValidationException.FieldViolation> validate(String trimmedUrl, String trimmedTitle, Long existingId) {
		List<BookmarkValidationException.FieldViolation> violations = new ArrayList<>();

		if (trimmedUrl == null) {
			violations.add(new BookmarkValidationException.FieldViolation("url", "URL is required."));
		} else {
			if (trimmedUrl.length() > MAX_URL_LENGTH) {
				violations.add(new BookmarkValidationException.FieldViolation("url", "URL must be 2048 characters or fewer."));
			} else if (!isValidHttpUrl(trimmedUrl)) {
				violations.add(new BookmarkValidationException.FieldViolation("url", "Enter a valid website URL starting with http:// or https:// and including a domain such as example.com."));
			} else if (existingId == null ? bookmarkRepository.existsByUrl(trimmedUrl) : bookmarkRepository.existsByUrlAndIdNot(trimmedUrl, existingId)) {
				violations.add(new BookmarkValidationException.FieldViolation("url", "This bookmark already exists."));
			}
		}

		if (trimmedTitle == null) {
			violations.add(new BookmarkValidationException.FieldViolation("title", "Title is required."));
		} else if (trimmedTitle.length() > MAX_TITLE_LENGTH) {
			violations.add(new BookmarkValidationException.FieldViolation("title", "Title must be 200 characters or fewer."));
		}

		return violations;
	}

	private Bookmark persist(Bookmark bookmark) {

		try {
			return bookmarkRepository.saveAndFlush(bookmark);
		} catch (DataIntegrityViolationException ex) {
			throw new BookmarkValidationException(List.of(new BookmarkValidationException.FieldViolation("url", "This bookmark already exists.")));
		}
	}

	public List<Bookmark> listBookmarks() {
		return bookmarkRepository.findAllByOrderByCreatedAtDescIdDesc();
	}

	public List<Bookmark> listBookmarks(String searchText, String selectedTag) {
		String normalizedSearchText = normalizeSearchText(searchText);
		String normalizedTag = normalizeSelectedTag(selectedTag);
		return listBookmarks().stream()
				.filter(bookmark -> matchesSearch(bookmark, normalizedSearchText))
				.filter(bookmark -> matchesTag(bookmark, normalizedTag))
				.toList();
	}

	public List<String> listTagOptions() {
		return bookmarkRepository.findDistinctTagValuesOrderByValueAsc();
	}

	public boolean hasBookmarks() {
		return bookmarkRepository.count() > 0;
	}

	List<String> normalizeTags(String rawTags) {
		if (!StringUtils.hasText(rawTags)) {
			return List.of();
		}

		Set<String> normalizedTags = new LinkedHashSet<>();
		for (String candidate : rawTags.split(",")) {
			String cleaned = candidate.trim();
			if (!cleaned.isEmpty()) {
				normalizedTags.add(cleaned.toLowerCase(Locale.ROOT));
			}
		}
		return List.copyOf(normalizedTags);
	}

	private String normalizeSearchText(String searchText) {
		String trimmed = trimToNull(searchText);
		return trimmed == null ? null : trimmed.toLowerCase(Locale.ROOT);
	}

	private String normalizeSelectedTag(String selectedTag) {
		String trimmed = trimToNull(selectedTag);
		return trimmed == null ? null : trimmed.toLowerCase(Locale.ROOT);
	}

	private boolean matchesSearch(Bookmark bookmark, String normalizedSearchText) {
		if (normalizedSearchText == null) {
			return true;
		}
		return containsIgnoreCase(bookmark.getTitle(), normalizedSearchText) || containsIgnoreCase(bookmark.getUrl(), normalizedSearchText);
	}

	private boolean containsIgnoreCase(String value, String normalizedSearchText) {
		return value != null && value.toLowerCase(Locale.ROOT).contains(normalizedSearchText);
	}

	private boolean matchesTag(Bookmark bookmark, String normalizedTag) {
		if (normalizedTag == null) {
			return true;
		}
		for (BookmarkTag tag : bookmark.getTags()) {
			if (normalizedTag.equals(tag.getValue())) {
				return true;
			}
		}
		return false;
	}

	private String trimToNull(String value) {
		if (!StringUtils.hasText(value)) {
			return null;
		}
		return value.trim();
	}

	private boolean isValidHttpUrl(String value) {
		URI uri;
		try {
			uri = URI.create(value);
		} catch (IllegalArgumentException ex) {
			return false;
		}

		String scheme = uri.getScheme();
		if (!("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))) {
			return false;
		}

		String host = uri.getHost();
		return host != null && !host.isBlank() && isValidDottedHostname(host);
	}

	private boolean isValidDottedHostname(String host) {
		if (!host.contains(".")) {
			return false;
		}
		if (IPV4_PATTERN.matcher(host).matches()) {
			return false;
		}
		String[] labels = host.split("\\.");
		if (labels.length < 2) {
			return false;
		}
		for (String label : labels) {
			if (label.isBlank() || label.length() > 63 || label.startsWith("-") || label.endsWith("-")) {
				return false;
			}
			for (int i = 0; i < label.length(); i++) {
				char ch = label.charAt(i);
				if (!(Character.isLetterOrDigit(ch) || ch == '-')) {
					return false;
				}
			}
		}
		return host.length() <= 253;
	}
}

