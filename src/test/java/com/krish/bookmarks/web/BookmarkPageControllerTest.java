package com.krish.bookmarks.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.krish.bookmarks.model.Bookmark;
import com.krish.bookmarks.model.BookmarkTag;
import com.krish.bookmarks.repository.BookmarkRepository;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
class BookmarkPageControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private BookmarkRepository bookmarkRepository;

	@BeforeEach
	void clearDatabase() {
		bookmarkRepository.deleteAll();
	}

	@Test
	void showsAddForm() throws Exception {
		mockMvc.perform(get("/bookmarks/new"))
				.andExpect(status().isOk())
				.andExpect(view().name("bookmark-form"))
				.andExpect(model().attributeExists("bookmarkForm"));
	}

	@Test
	void showsEmptyListMessageWhenNoBookmarksExist() throws Exception {
		mockMvc.perform(get("/bookmarks"))
				.andExpect(status().isOk())
				.andExpect(view().name("index"))
				.andExpect(model().attributeExists("bookmarks"))
				.andExpect(model().attribute("bookmarks", org.hamcrest.Matchers.empty()))
				.andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string(org.hamcrest.Matchers.containsString("No bookmarks yet. Add your first bookmark.")))
				.andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string(org.hamcrest.Matchers.containsString("Add bookmark")));
	}

	@Test
	void savesValidBookmarkWithoutTags() throws Exception {
		mockMvc.perform(post("/bookmarks")
					.param("url", "  https://example.com/articles/1  ")
					.param("title", "  Example article  ")
					.param("tags", ""))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/bookmarks"))
				.andExpect(flash().attribute("successMessage", "Bookmark saved."));

		Bookmark saved = bookmarkRepository.findByUrlWithTags("https://example.com/articles/1").orElseThrow();
		assertThat(saved.getTitle()).isEqualTo("Example article");
		assertThat(saved.getTags()).isEmpty();
	}

	@Test
	void showsSavedBookmarksWithTitleUrlAndTags() throws Exception {
		saveBookmark("https://example.com/articles/1", "Example article", Instant.parse("2026-09-29T10:00:00Z"), "java", "spring");

		mockMvc.perform(get("/bookmarks"))
				.andExpect(status().isOk())
				.andExpect(view().name("index"))
				.andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string(org.hamcrest.Matchers.containsString("Example article")))
				.andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string(org.hamcrest.Matchers.containsString("https://example.com/articles/1")))
				.andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string(org.hamcrest.Matchers.containsString("java, spring")));
	}

	@Test
	void showsPrefilledEditFormAndPreservesSearchStateInLinks() throws Exception {
		saveBookmark("https://example.com/articles/1", "Example article", Instant.parse("2026-09-29T10:00:00Z"), "java", "spring");
		Bookmark saved = bookmarkRepository.findByUrlWithTags("https://example.com/articles/1").orElseThrow();

		String html = mockMvc.perform(get("/bookmarks/{id}/edit", saved.getId())
					.param("search", "  Docs  ")
					.param("tag", "spring"))
				.andExpect(status().isOk())
				.andExpect(view().name("bookmark-form"))
				.andReturn()
				.getResponse()
				.getContentAsString();

		assertThat(html).contains("Edit bookmark");
		assertThat(html).contains("value=\"https://example.com/articles/1\"");
		assertThat(html).contains("Example article");
		assertThat(html).contains("java, spring");
		assertThat(html).contains("action=\"/bookmarks/" + saved.getId() + "?search=Docs&amp;tag=spring\"");
		assertThat(html).contains("search=Docs");
		assertThat(html).contains("tag=spring");
		assertThat(html).contains("/bookmarks?search=Docs&amp;tag=spring");
	}

	@Test
	void keepsSearchStateOnAddBookmarkLinkAndAddFormCancel() throws Exception {
		saveBookmark("https://docs.example.com/java", "Java Docs", Instant.parse("2026-09-29T11:00:00Z"), "spring");

		String listHtml = mockMvc.perform(get("/bookmarks")
					.param("search", "Docs")
					.param("tag", "spring"))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();

		assertThat(listHtml).contains("/bookmarks/new?search=Docs&amp;tag=spring");

		String addHtml = mockMvc.perform(get("/bookmarks/new")
					.param("search", "Docs")
					.param("tag", "spring"))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();

		assertThat(addHtml).contains("action=\"/bookmarks?search=Docs&amp;tag=spring\"");
		assertThat(addHtml).contains("/bookmarks?search=Docs&amp;tag=spring");
	}

	@Test
	void updatesAllEditableFieldsAndPreservesCreationTimeAndOrdering() throws Exception {
		saveBookmark("https://example.com/older", "Older bookmark", Instant.parse("2026-09-29T09:00:00Z"), "java");
		saveBookmark("https://example.com/newer", "Newer bookmark", Instant.parse("2026-09-29T10:00:00Z"), "spring");
		Bookmark older = bookmarkRepository.findByUrlWithTags("https://example.com/older").orElseThrow();

		mockMvc.perform(post("/bookmarks/{id}", older.getId())
					.param("search", "older")
					.param("tag", "java")
					.param("url", " https://example.com/older-updated ")
					.param("title", " Updated older bookmark ")
					.param("tags", " spring, java, spring boot "))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/bookmarks?search=older&tag=java"))
				.andExpect(flash().attribute("successMessage", "Bookmark updated."));

		Bookmark updated = bookmarkRepository.findByUrlWithTags("https://example.com/older-updated").orElseThrow();
		assertThat(updated.getId()).isEqualTo(older.getId());
		assertThat(updated.getCreatedAt()).isEqualTo(Instant.parse("2026-09-29T09:00:00Z"));
		assertThat(updated.getTitle()).isEqualTo("Updated older bookmark");
		assertThat(updated.getTags()).extracting(BookmarkTag::getValue).containsExactly("spring", "java", "spring boot");

		String html = mockMvc.perform(get("/bookmarks"))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();

		assertThat(html.indexOf("Newer bookmark")).isLessThan(html.indexOf("Updated older bookmark"));
	}

	@Test
	void rejectsUpdatingToAnotherBookmarksTrimmedUrlAndLeavesDataUnchanged() throws Exception {
		saveBookmark("https://example.com/current", "Current bookmark", Instant.parse("2026-09-29T09:00:00Z"), "java");
		saveBookmark("https://example.com/other", "Other bookmark", Instant.parse("2026-09-29T10:00:00Z"), "spring");
		Bookmark current = bookmarkRepository.findByUrlWithTags("https://example.com/current").orElseThrow();

		mockMvc.perform(post("/bookmarks/{id}", current.getId())
					.param("search", "current")
					.param("tag", "java")
					.param("url", "  https://example.com/other  ")
					.param("title", " Current bookmark updated ")
					.param("tags", "java"))
				.andExpect(status().isOk())
				.andExpect(view().name("bookmark-form"))
				.andExpect(model().attributeHasFieldErrors("bookmarkForm", "url"))
				.andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string(org.hamcrest.Matchers.containsString("This bookmark already exists.")));

		Bookmark unchanged = bookmarkRepository.findByUrlWithTags("https://example.com/current").orElseThrow();
		assertThat(unchanged.getTitle()).isEqualTo("Current bookmark");
		assertThat(unchanged.getTags()).extracting(BookmarkTag::getValue).containsExactly("java");
		assertThat(bookmarkRepository.findByUrlWithTags("https://example.com/other")).isPresent();
	}

	@Test
	void rejectsInvalidEditAndKeepsStoredDataUnchanged() throws Exception {
		saveBookmark("https://example.com/edit", "Editable bookmark", Instant.parse("2026-09-29T09:00:00Z"), "java", "spring");
		Bookmark existing = bookmarkRepository.findByUrlWithTags("https://example.com/edit").orElseThrow();

		mockMvc.perform(post("/bookmarks/{id}", existing.getId())
					.param("search", "edit")
					.param("tag", "java")
					.param("url", "ftp://example.com")
					.param("title", "   ")
					.param("tags", "ignored"))
				.andExpect(status().isOk())
				.andExpect(view().name("bookmark-form"))
				.andExpect(model().attributeHasFieldErrors("bookmarkForm", "url", "title"))
				.andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string(org.hamcrest.Matchers.containsString("Enter a valid website URL starting with http:// or https:// and including a domain such as example.com.")));

		Bookmark reloaded = bookmarkRepository.findByUrlWithTags("https://example.com/edit").orElseThrow();
		assertThat(reloaded.getTitle()).isEqualTo("Editable bookmark");
		assertThat(reloaded.getTags()).extracting(BookmarkTag::getValue).containsExactly("java", "spring");
	}

	@Test
	void removesAllOptionalTagsWhenEditingAndPreservesVisibleFilterStateOnCancel() throws Exception {
		saveBookmark("https://example.com/clear-tags", "Clear tags bookmark", Instant.parse("2026-09-29T09:00:00Z"), "java", "spring");
		Bookmark saved = bookmarkRepository.findByUrlWithTags("https://example.com/clear-tags").orElseThrow();

		mockMvc.perform(post("/bookmarks/{id}", saved.getId())
					.param("search", "Clear")
					.param("tag", "spring")
					.param("url", "https://example.com/clear-tags")
					.param("title", "Clear tags bookmark edited")
					.param("tags", "   "))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/bookmarks?search=Clear&tag=spring"));

		Bookmark updated = bookmarkRepository.findByUrlWithTags("https://example.com/clear-tags").orElseThrow();
		assertThat(updated.getTags()).isEmpty();

		String filteredHtml = mockMvc.perform(get("/bookmarks")
					.param("tag", "spring"))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();

		assertThat(filteredHtml).contains("No matching bookmarks found.");
		assertThat(filteredHtml).contains("value=\"spring\" selected=\"selected\"");

		String cancelHtml = mockMvc.perform(get("/bookmarks/{id}/edit", saved.getId())
					.param("search", "Clear")
					.param("tag", "spring"))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();

		assertThat(cancelHtml).contains("/bookmarks?search=Clear&amp;tag=spring");
	}

	@Test
	void showsDeleteConfirmationPageWithEscapedBookmarkDetailsAndPreservedState() throws Exception {
		saveBookmark("https://example.com/delete-me", "Delete <b>me</b>", Instant.parse("2026-09-29T09:00:00Z"), "java");
		Bookmark saved = bookmarkRepository.findByUrlWithTags("https://example.com/delete-me").orElseThrow();

		String html = mockMvc.perform(get("/bookmarks/{id}/delete", saved.getId())
					.param("search", "  Docs  ")
					.param("tag", "spring"))
			.andExpect(status().isOk())
			.andExpect(view().name("bookmark-delete"))
			.andReturn()
			.getResponse()
			.getContentAsString();

		assertThat(html).contains("Delete bookmark");
		assertThat(html).contains("Delete &lt;b&gt;me&lt;/b&gt;");
		assertThat(html).contains("https://example.com/delete-me");
		assertThat(html).contains("action=\"/bookmarks/" + saved.getId() + "/delete?search=Docs&amp;tag=spring\"");
		assertThat(html).contains("/bookmarks?search=Docs&amp;tag=spring");
	}

	@Test
	void cancelingDeleteLeavesStoredDataUnchangedAndKeepsTheCancelLinkSafe() throws Exception {
		saveBookmark("https://example.com/cancel-delete", "Cancel delete", Instant.parse("2026-09-29T09:00:00Z"), "spring");
		Bookmark saved = bookmarkRepository.findByUrlWithTags("https://example.com/cancel-delete").orElseThrow();

		String html = mockMvc.perform(get("/bookmarks/{id}/delete", saved.getId())
					.param("search", "  Cancel  ")
					.param("tag", "spring"))
			.andExpect(status().isOk())
			.andReturn()
			.getResponse()
			.getContentAsString();

		assertThat(html).contains("/bookmarks?search=Cancel&amp;tag=spring");
		assertThat(bookmarkRepository.findByUrlWithTags("https://example.com/cancel-delete")).isPresent();
	}

	@Test
	void confirmsDeleteRemovesBookmarkAndItsTagsWhileLeavingOtherBookmarksUntouched() throws Exception {
		saveBookmark("https://example.com/delete-this", "Delete this", Instant.parse("2026-09-29T10:00:00Z"), "java", "spring");
		saveBookmark("https://example.com/keep-this", "Keep this", Instant.parse("2026-09-29T09:00:00Z"), "spring", "docs");
		Bookmark toDelete = bookmarkRepository.findByUrlWithTags("https://example.com/delete-this").orElseThrow();

		mockMvc.perform(post("/bookmarks/{id}/delete", toDelete.getId())
					.param("search", "delete")
					.param("tag", "java"))
			.andExpect(status().is3xxRedirection())
			.andExpect(redirectedUrl("/bookmarks?search=delete&tag=java"))
			.andExpect(flash().attribute("successMessage", "Bookmark deleted."));

		assertThat(bookmarkRepository.findByUrlWithTags("https://example.com/delete-this")).isEmpty();
		Bookmark kept = bookmarkRepository.findByUrlWithTags("https://example.com/keep-this").orElseThrow();
		assertThat(kept.getTags()).extracting(BookmarkTag::getValue).containsExactly("spring", "docs");

		String filteredHtml = mockMvc.perform(get("/bookmarks")
					.param("search", "delete")
					.param("tag", "java"))
			.andExpect(status().isOk())
			.andReturn()
			.getResponse()
			.getContentAsString();

		assertThat(filteredHtml).contains("No matching bookmarks found.");
		assertThat(filteredHtml).contains("value=\"java\" selected=\"selected\"");
	}

	@Test
	void showsFriendlyNotFoundResponseWhenDeletingMissingOrAlreadyDeletedBookmark() throws Exception {
		mockMvc.perform(get("/bookmarks/999/delete"))
				.andExpect(status().isNotFound())
				.andExpect(view().name("bookmark-not-found"))
				.andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string(org.hamcrest.Matchers.containsString("Bookmark not found")));

		saveBookmark("https://example.com/already-gone", "Already gone", Instant.parse("2026-09-29T09:00:00Z"));
		Bookmark saved = bookmarkRepository.findByUrlWithTags("https://example.com/already-gone").orElseThrow();
		mockMvc.perform(post("/bookmarks/{id}/delete", saved.getId()))
				.andExpect(status().is3xxRedirection());

		mockMvc.perform(post("/bookmarks/{id}/delete", saved.getId()))
				.andExpect(status().isNotFound())
				.andExpect(view().name("bookmark-not-found"));
	}

	@Test
	void showsEmptyStateAfterDeletingTheLastBookmark() throws Exception {
		saveBookmark("https://example.com/last", "Last bookmark", Instant.parse("2026-09-29T09:00:00Z"), "java");
		Bookmark saved = bookmarkRepository.findByUrlWithTags("https://example.com/last").orElseThrow();

		mockMvc.perform(post("/bookmarks/{id}/delete", saved.getId()))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/bookmarks"))
				.andExpect(flash().attribute("successMessage", "Bookmark deleted."));

		String html = mockMvc.perform(get("/bookmarks"))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();

		assertThat(html).contains("No bookmarks yet. Add your first bookmark.");
	}

	@Test
	void showsFriendlyNotFoundResponseForMissingBookmark() throws Exception {
		mockMvc.perform(get("/bookmarks/999/edit"))
				.andExpect(status().isNotFound())
				.andExpect(view().name("bookmark-not-found"))
				.andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string(org.hamcrest.Matchers.containsString("Bookmark not found")));
	}

	@Test
	void showsBookmarksWithoutTagsCorrectly() throws Exception {
		saveBookmark("https://example.com/untagged", "No tags article", Instant.parse("2026-09-29T10:00:00Z"));

		mockMvc.perform(get("/bookmarks"))
				.andExpect(status().isOk())
				.andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string(org.hamcrest.Matchers.containsString("No tags")));
	}

	@Test
	void ordersBookmarksNewestFirstAndUsesIdForTies() throws Exception {
		saveBookmark("https://example.com/older", "Older bookmark", Instant.parse("2026-09-29T10:00:00Z"));
		saveBookmark("https://example.com/newer-a", "Newer bookmark A", Instant.parse("2026-09-29T11:00:00Z"));
		saveBookmark("https://example.com/newer-b", "Newer bookmark B", Instant.parse("2026-09-29T11:00:00Z"));

		String html = mockMvc.perform(get("/bookmarks"))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();

		assertThat(html.indexOf("Newer bookmark B")).isGreaterThanOrEqualTo(0);
		assertThat(html.indexOf("Newer bookmark A")).isGreaterThanOrEqualTo(0);
		assertThat(html.indexOf("Older bookmark")).isGreaterThanOrEqualTo(0);
		assertThat(html.indexOf("Newer bookmark B")).isLessThan(html.indexOf("Newer bookmark A"));
		assertThat(html.indexOf("Newer bookmark A")).isLessThan(html.indexOf("Older bookmark"));
	}

	@Test
	void filtersBookmarksBySearchAndTagAndRetainsTheSubmittedControls() throws Exception {
		saveBookmark("https://docs.example.com/java", "Java Docs", Instant.parse("2026-09-29T11:00:00Z"), "spring", "java");
		saveBookmark("https://example.com/guide", "General Guide", Instant.parse("2026-09-29T10:00:00Z"), "spring");

		String html = mockMvc.perform(get("/bookmarks")
					.param("search", "  DOCS  ")
					.param("tag", "spring"))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();

		assertThat(html).contains("Java Docs");
		assertThat(html).doesNotContain("General Guide");
		assertThat(html).contains("value=\"DOCS\"");
		assertThat(html).contains("value=\"spring\" selected=\"selected\"");
		assertThat(html).contains("Clear search and filters");
	}

	@Test
	void showsAlphabeticalUniqueTagOptionsAcrossAllSavedBookmarks() throws Exception {
		saveBookmark("https://example.com/one", "One", Instant.parse("2026-09-29T11:00:00Z"), "spring", "java");
		saveBookmark("https://example.com/two", "Two", Instant.parse("2026-09-29T10:00:00Z"), "javascript", "java");

		String html = mockMvc.perform(get("/bookmarks"))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();

		assertThat(html).contains("All tags");
		assertThat(countOccurrences(html, "value=\"java\"")).isEqualTo(1);
		assertThat(countOccurrences(html, "value=\"javascript\"")).isEqualTo(1);
		assertThat(countOccurrences(html, "value=\"spring\"")).isEqualTo(1);
		assertThat(html.indexOf("value=\"java\"")).isLessThan(html.indexOf("value=\"javascript\""));
		assertThat(html.indexOf("value=\"javascript\"")).isLessThan(html.indexOf("value=\"spring\""));
	}

	@Test
	void showsNoMatchingMessageAndClearLinkWhenFiltersReturnNoResults() throws Exception {
		saveBookmark("https://example.com/one", "One", Instant.parse("2026-09-29T11:00:00Z"), "java");

		String html = mockMvc.perform(get("/bookmarks")
					.param("search", "missing")
					.param("tag", "spring"))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();

		assertThat(html).contains("No matching bookmarks found.");
		assertThat(html).contains("Clear search and filters");
		assertThat(html).contains("href=\"/bookmarks\"");
		assertThat(html).doesNotContain("No bookmarks yet. Add your first bookmark.");
	}

	@Test
	void escapesUserEnteredContentWhenRenderingBookmarks() throws Exception {
		saveBookmark("https://example.com/escape", "<script>alert(1)</script>", Instant.parse("2026-09-29T10:00:00Z"), "<b>tag</b>");

		String html = mockMvc.perform(get("/bookmarks"))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();

		assertThat(html).doesNotContain("<script>alert(1)</script>");
		assertThat(html).contains("&lt;script&gt;alert(1)&lt;/script&gt;");
		assertThat(html).doesNotContain("<b>tag</b>");
		assertThat(html).contains("&lt;b&gt;tag&lt;/b&gt;");
	}

	@Test
	void savesValidBookmarkWithCleanedTags() throws Exception {
		mockMvc.perform(post("/bookmarks")
					.param("url", "https://example.com/articles/2")
					.param("title", "Example")
					.param("tags", " Java, spring, JAVA, , Spring Boot, spring "))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/bookmarks"));

		Bookmark saved = bookmarkRepository.findByUrlWithTags("https://example.com/articles/2").orElseThrow();
		assertThat(saved.getTags()).extracting(BookmarkTag::getValue).containsExactly("java", "spring", "spring boot");
	}

	@Test
	void rejectsBlankRequiredFieldsAndKeepsFormInput() throws Exception {
		mockMvc.perform(post("/bookmarks")
					.param("url", "   ")
					.param("title", "\t"))
				.andExpect(status().isOk())
				.andExpect(view().name("bookmark-form"))
				.andExpect(model().attributeHasFieldErrors("bookmarkForm", "url", "title"))
				.andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string(org.hamcrest.Matchers.containsString("class=\"form-message form-message--error\"")))
				.andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string(org.hamcrest.Matchers.containsString("Please fix the errors below.")));

		assertThat(bookmarkRepository.count()).isZero();
	}

	@Test
	void rejectsMissingSchemeAndUnsupportedScheme() throws Exception {
		mockMvc.perform(post("/bookmarks")
					.param("url", "example.com")
					.param("title", "Example"))
				.andExpect(status().isOk())
				.andExpect(model().attributeHasFieldErrors("bookmarkForm", "url"))
				.andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string(org.hamcrest.Matchers.containsString("Enter a valid website URL starting with http:// or https:// and including a domain such as example.com.")));

		mockMvc.perform(post("/bookmarks")
					.param("url", "ftp://example.com")
					.param("title", "Example"))
				.andExpect(status().isOk())
				.andExpect(model().attributeHasFieldErrors("bookmarkForm", "url"));

		assertThat(bookmarkRepository.count()).isZero();
	}

	@Test
	void rejectsDuplicateTrimmedUrlWithFriendlyMessage() throws Exception {
		mockMvc.perform(post("/bookmarks")
					.param("url", "https://example.com/dup")
					.param("title", "First"))
				.andExpect(status().is3xxRedirection());

		mockMvc.perform(post("/bookmarks")
					.param("url", "  https://example.com/dup  ")
					.param("title", "Second"))
				.andExpect(status().isOk())
				.andExpect(view().name("bookmark-form"))
				.andExpect(model().attributeHasFieldErrors("bookmarkForm", "url"));

		assertThat(bookmarkRepository.count()).isEqualTo(1);
	}

	private void saveBookmark(String url, String title, Instant createdAt, String... tags) {
		Bookmark bookmark = new Bookmark();
		bookmark.setUrl(url);
		bookmark.setTitle(title);
		bookmark.setCreatedAt(createdAt);
		for (String tag : tags) {
			bookmark.addTag(tag);
		}
		bookmarkRepository.saveAndFlush(bookmark);
	}

	private int countOccurrences(String value, String token) {
		int count = 0;
		int index = 0;
		while ((index = value.indexOf(token, index)) >= 0) {
			count++;
			index += token.length();
		}
		return count;
	}
}

