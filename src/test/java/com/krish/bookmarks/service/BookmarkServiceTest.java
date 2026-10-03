package com.krish.bookmarks.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import com.krish.bookmarks.model.Bookmark;
import com.krish.bookmarks.repository.BookmarkRepository;
import com.krish.bookmarks.web.BookmarkForm;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MockitoExtension.class)
class BookmarkServiceTest {

	@Mock
	private BookmarkRepository bookmarkRepository;

	private BookmarkService bookmarkService;

	@BeforeEach
	void setUp() {
		bookmarkService = new BookmarkService(bookmarkRepository, Clock.fixed(Instant.parse("2026-09-29T10:15:30Z"), ZoneOffset.UTC));
	}

	@ParameterizedTest
	@ValueSource(strings = { "https://chatgpt.com", "https://docs.example.com/path?q=java" })
	void savesBookmarkWithAcceptedDottedHttpUrls(String url) {
		BookmarkForm form = form("  " + url + "  ", "  Example title  ", null);
		when(bookmarkRepository.existsByUrl(url)).thenReturn(false);
		when(bookmarkRepository.saveAndFlush(any(Bookmark.class))).thenAnswer(invocation -> invocation.getArgument(0));

		Bookmark saved = bookmarkService.save(form);

		ArgumentCaptor<Bookmark> captor = ArgumentCaptor.forClass(Bookmark.class);
		verify(bookmarkRepository).saveAndFlush(captor.capture());
		assertThat(saved.getUrl()).isEqualTo(url);
		assertThat(captor.getValue().getUrl()).isEqualTo(url);
		assertThat(captor.getValue().getTitle()).isEqualTo("Example title");
	}

	@Test
	void savesBookmarkWithoutTags() {
		BookmarkForm form = form("  https://example.com/path  ", "  Example title  ", null);
		when(bookmarkRepository.existsByUrl("https://example.com/path")).thenReturn(false);
		when(bookmarkRepository.saveAndFlush(any(Bookmark.class))).thenAnswer(invocation -> invocation.getArgument(0));

		Bookmark saved = bookmarkService.save(form);

		ArgumentCaptor<Bookmark> captor = ArgumentCaptor.forClass(Bookmark.class);
		verify(bookmarkRepository).saveAndFlush(captor.capture());
		assertThat(saved.getUrl()).isEqualTo("https://example.com/path");
		assertThat(saved.getTitle()).isEqualTo("Example title");
		assertThat(saved.getCreatedAt()).isEqualTo(Instant.parse("2026-09-29T10:15:30Z"));
		assertThat(captor.getValue().getTags()).isEmpty();
	}

	@Test
	void savesBookmarkWithNormalizedTagsAndDeduplicatesThem() {
		BookmarkForm form = form("https://example.com", "Example", " Java, spring, JAVA, , Spring Boot, spring ");
		when(bookmarkRepository.existsByUrl("https://example.com")).thenReturn(false);
		when(bookmarkRepository.saveAndFlush(any(Bookmark.class))).thenAnswer(invocation -> invocation.getArgument(0));

		Bookmark saved = bookmarkService.save(form);

		assertThat(saved.getTags())
				.extracting(com.krish.bookmarks.model.BookmarkTag::getValue)
				.containsExactly("java", "spring", "spring boot");
	}

	@Test
	void rejectsBlankRequiredFieldsWithoutCallingRepository() {
		BookmarkForm form = form("   ", "\t", "ignored");

		assertThatThrownBy(() -> bookmarkService.save(form))
				.isInstanceOf(BookmarkValidationException.class)
				.hasMessageContaining("Bookmark validation failed");

		verifyNoInteractions(bookmarkRepository);
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"http://chatgpt",
			"http://localhost",
			"http://127.0.0.1",
			"https://example..com",
			"https://.com",
			"chatgpt.com",
			"ftp://example.com" })
	void rejectsUrlsThatAreNotDottedHttpWebsites(String url) {
		BookmarkForm form = form(url, "Example", null);

		Throwable thrown = catchThrowable(() -> bookmarkService.save(form));
		assertThat(thrown).isInstanceOf(BookmarkValidationException.class);
		assertThat(((BookmarkValidationException) thrown).getViolations())
				.extracting(BookmarkValidationException.FieldViolation::message)
				.contains("Enter a valid website URL starting with http:// or https:// and including a domain such as example.com.");

		verify(bookmarkRepository, never()).saveAndFlush(any());
	}

	@Test
	void rejectsDuplicateTrimmedUrl() {
		BookmarkForm form = form("  https://example.com  ", "Example", null);
		when(bookmarkRepository.existsByUrl("https://example.com")).thenReturn(true);

		assertThatThrownBy(() -> bookmarkService.save(form))
				.isInstanceOf(BookmarkValidationException.class);

		verify(bookmarkRepository).existsByUrl("https://example.com");
		verify(bookmarkRepository, never()).saveAndFlush(any());
	}

	@Test
	void updatesBookmarkPreservingIdAndCreationTimeAndReplacingAllFields() {
		Bookmark existing = bookmark("https://example.com/original", "Original title", Instant.parse("2026-09-29T09:00:00Z"), "java");
		existing.setId(42L);
		when(bookmarkRepository.findByIdWithTags(42L)).thenReturn(java.util.Optional.of(existing));
		when(bookmarkRepository.existsByUrlAndIdNot("https://example.com/updated", 42L)).thenReturn(false);
		doNothing().when(bookmarkRepository).deleteTagsByBookmarkId(42L);
		when(bookmarkRepository.saveAndFlush(any(Bookmark.class))).thenAnswer(invocation -> invocation.getArgument(0));

		Bookmark updated = bookmarkService.update(42L, form(" https://example.com/updated ", " Updated title ", " spring, java, spring "));

		assertThat(updated.getId()).isEqualTo(42L);
		assertThat(updated.getCreatedAt()).isEqualTo(Instant.parse("2026-09-29T09:00:00Z"));
		assertThat(updated.getUrl()).isEqualTo("https://example.com/updated");
		assertThat(updated.getTitle()).isEqualTo("Updated title");
		assertThat(updated.getTags()).extracting(com.krish.bookmarks.model.BookmarkTag::getValue).containsExactly("spring", "java");
	}

	@Test
	void allowsKeepingCurrentUrlWhileEditingButRejectsAnotherBookmarksUrl() {
		Bookmark existing = bookmark("https://example.com/current", "Current title", Instant.parse("2026-09-29T09:00:00Z"), "java");
		existing.setId(7L);
		when(bookmarkRepository.findByIdWithTags(7L)).thenReturn(java.util.Optional.of(existing));
		when(bookmarkRepository.existsByUrlAndIdNot("https://example.com/current", 7L)).thenReturn(false);
		doNothing().when(bookmarkRepository).deleteTagsByBookmarkId(7L);
		when(bookmarkRepository.saveAndFlush(any(Bookmark.class))).thenAnswer(invocation -> invocation.getArgument(0));

		Bookmark updated = bookmarkService.update(7L, form("https://example.com/current", "Current title updated", null));
		assertThat(updated.getUrl()).isEqualTo("https://example.com/current");
		assertThat(updated.getTitle()).isEqualTo("Current title updated");

		clearInvocations(bookmarkRepository);
		BookmarkForm duplicate = form("https://example.com/current", "Other title", null);
		when(bookmarkRepository.findByIdWithTags(8L)).thenReturn(java.util.Optional.of(bookmark("https://example.com/other", "Other", Instant.parse("2026-09-29T08:00:00Z"))));
		when(bookmarkRepository.existsByUrlAndIdNot("https://example.com/current", 8L)).thenReturn(true);

		assertThatThrownBy(() -> bookmarkService.update(8L, duplicate))
				.isInstanceOf(BookmarkValidationException.class)
				.hasMessageContaining("Bookmark validation failed");

		verify(bookmarkRepository, never()).saveAndFlush(any());
	}

	@Test
	void rejectsInvalidEditedDataAndLeavesStoredBookmarkUnchanged() {
		Bookmark existing = bookmark("https://example.com/original", "Original title", Instant.parse("2026-09-29T09:00:00Z"), "java", "spring");
		existing.setId(55L);
		when(bookmarkRepository.findByIdWithTags(55L)).thenReturn(java.util.Optional.of(existing));

		assertThatThrownBy(() -> bookmarkService.update(55L, form("ftp://example.com", "   ", "ignored")))
				.isInstanceOf(BookmarkValidationException.class);

		assertThat(existing.getUrl()).isEqualTo("https://example.com/original");
		assertThat(existing.getTitle()).isEqualTo("Original title");
		assertThat(existing.getTags()).extracting(com.krish.bookmarks.model.BookmarkTag::getValue).containsExactly("java", "spring");
		verify(bookmarkRepository, never()).saveAndFlush(any());
	}

	@Test
	void removesAllOptionalTagsWhenEditing() {
		Bookmark existing = bookmark("https://example.com/original", "Original title", Instant.parse("2026-09-29T09:00:00Z"), "java", "spring");
		existing.setId(61L);
		when(bookmarkRepository.findByIdWithTags(61L)).thenReturn(java.util.Optional.of(existing));
		when(bookmarkRepository.existsByUrlAndIdNot("https://example.com/original", 61L)).thenReturn(false);
		doNothing().when(bookmarkRepository).deleteTagsByBookmarkId(61L);
		when(bookmarkRepository.saveAndFlush(any(Bookmark.class))).thenAnswer(invocation -> invocation.getArgument(0));

		Bookmark updated = bookmarkService.update(61L, form("https://example.com/original", "Original title updated", "   "));

		assertThat(updated.getTags()).isEmpty();
	}

	@Test
	void deletesBookmarkAndItsLoadedTagsAtomically() {
		Bookmark existing = bookmark("https://example.com/delete", "Delete me", Instant.parse("2026-09-29T09:00:00Z"), "java", "spring");
		existing.setId(71L);
		when(bookmarkRepository.findByIdWithTags(71L)).thenReturn(java.util.Optional.of(existing));
		doNothing().when(bookmarkRepository).delete(existing);

		bookmarkService.delete(71L);

		verify(bookmarkRepository).delete(existing);
		assertThat(existing.getTags()).extracting(com.krish.bookmarks.model.BookmarkTag::getValue).containsExactly("java", "spring");
	}

	@Test
	void listsBookmarksNewestFirst() {
		when(bookmarkRepository.findAllByOrderByCreatedAtDescIdDesc()).thenReturn(List.of());

		assertThat(bookmarkService.listBookmarks()).isEmpty();

		verify(bookmarkRepository).findAllByOrderByCreatedAtDescIdDesc();
	}

	@Test
	void filtersBookmarksByPartialCaseInsensitiveSearchAcrossTitleAndUrl() {
		Bookmark titleMatch = bookmark("https://example.com/path", "Java tips", Instant.parse("2026-09-29T10:15:30Z"));
		Bookmark urlMatch = bookmark("https://docs.example.com/java", "Spring guide", Instant.parse("2026-09-29T10:10:30Z"));
		when(bookmarkRepository.findAllByOrderByCreatedAtDescIdDesc()).thenReturn(List.of(titleMatch, urlMatch));

		assertThat(bookmarkService.listBookmarks("  JAVA  ", null))
				.extracting(Bookmark::getTitle)
				.containsExactly("Java tips", "Spring guide");
	}

	@Test
	void trimsBlankSearchAndLeavesResultsUnrestricted() {
		Bookmark first = bookmark("https://example.com/first", "First", Instant.parse("2026-09-29T10:15:30Z"));
		Bookmark second = bookmark("https://example.com/second", "Second", Instant.parse("2026-09-29T10:10:30Z"));
		when(bookmarkRepository.findAllByOrderByCreatedAtDescIdDesc()).thenReturn(List.of(first, second));

		assertThat(bookmarkService.listBookmarks("   ", null))
				.extracting(Bookmark::getTitle)
				.containsExactly("First", "Second");
	}

	@Test
	void filtersBookmarksByExactSelectedTag() {
		Bookmark javaBookmark = bookmark("https://example.com/java", "Java bookmark", Instant.parse("2026-09-29T10:15:30Z"), "java");
		Bookmark javascriptBookmark = bookmark("https://example.com/javascript", "JavaScript bookmark", Instant.parse("2026-09-29T10:10:30Z"), "javascript");
		when(bookmarkRepository.findAllByOrderByCreatedAtDescIdDesc()).thenReturn(List.of(javaBookmark, javascriptBookmark));

		assertThat(bookmarkService.listBookmarks(null, "java"))
				.extracting(Bookmark::getTitle)
				.containsExactly("Java bookmark");
	}

	@Test
	void combinesSearchTextAndTagFilters() {
		Bookmark matching = bookmark("https://docs.example.com/java", "Docs bookmark", Instant.parse("2026-09-29T10:15:30Z"), "spring");
		Bookmark searchOnly = bookmark("https://docs.example.com/other", "Docs bookmark", Instant.parse("2026-09-29T10:10:30Z"), "java");
		Bookmark tagOnly = bookmark("https://example.com/spring", "Spring bookmark", Instant.parse("2026-09-29T10:05:30Z"), "spring");
		when(bookmarkRepository.findAllByOrderByCreatedAtDescIdDesc()).thenReturn(List.of(matching, searchOnly, tagOnly));

		assertThat(bookmarkService.listBookmarks("docs", "spring"))
				.extracting(Bookmark::getTitle)
				.containsExactly("Docs bookmark");
	}

	@Test
	void acceptsMaximumLengthUrlAndTitleAtTrimmedBoundary() {
		String url = "https://example.com/" + "a".repeat(2048 - "https://example.com/".length());
		String title = "T".repeat(200);
		BookmarkForm form = form("  " + url + "  ", "  " + title + "  ", null);
		when(bookmarkRepository.existsByUrl(url)).thenReturn(false);
		when(bookmarkRepository.saveAndFlush(any(Bookmark.class))).thenAnswer(invocation -> invocation.getArgument(0));

		Bookmark saved = bookmarkService.save(form);

		assertThat(saved.getUrl()).isEqualTo(url);
		assertThat(saved.getTitle()).isEqualTo(title);
		verify(bookmarkRepository).saveAndFlush(any(Bookmark.class));
	}

	@Test
	void rejectsOverLimitUrlAndTitleWithoutTruncating() {
		String url = "https://example.com/" + "a".repeat(2048 - "https://example.com/".length() + 1);
		String title = "T".repeat(201);
		BookmarkForm form = form(url, title, null);

		assertThatThrownBy(() -> bookmarkService.save(form))
				.isInstanceOf(BookmarkValidationException.class)
				.hasMessageContaining("Bookmark validation failed");

		verifyNoInteractions(bookmarkRepository);
	}

	private Bookmark bookmark(String url, String title, Instant createdAt, String... tags) {
		Bookmark bookmark = new Bookmark();
		bookmark.setUrl(url);
		bookmark.setTitle(title);
		bookmark.setCreatedAt(createdAt);
		for (String tag : tags) {
			bookmark.addTag(tag);
		}
		return bookmark;
	}

	private BookmarkForm form(String url, String title, String tags) {
		BookmarkForm form = new BookmarkForm();
		form.setUrl(url);
		form.setTitle(title);
		form.setTags(tags);
		return form;
	}
}

