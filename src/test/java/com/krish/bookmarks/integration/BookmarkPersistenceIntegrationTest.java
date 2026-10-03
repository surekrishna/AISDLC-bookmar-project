package com.krish.bookmarks.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.nio.file.Path;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

import com.krish.bookmarks.BookmarkManagerApplication;
import com.krish.bookmarks.model.Bookmark;
import com.krish.bookmarks.repository.BookmarkRepository;
import com.krish.bookmarks.service.BookmarkService;
import com.krish.bookmarks.web.BookmarkForm;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

class BookmarkPersistenceIntegrationTest {

	private static final List<String> OVERRIDE_KEYS = List.of(
			"spring.datasource.url",
			"spring.datasource.username",
			"spring.datasource.password",
			"spring.datasource.driver-class-name",
			"spring.jpa.hibernate.ddl-auto",
			"spring.jpa.open-in-view",
			"spring.flyway.locations",
			"spring.flyway.enabled",
			"spring.main.web-application-type");

	@Test
	void persistsBookmarkAndTagsAcrossRestart(@TempDir Path tempDir) {
		Path dbFile = tempDir.resolve("bookmarks-persistence-test");
		Instant[] createdAtBeforeRestart = new Instant[1];
		Map<String, String> properties = Map.of(
				"spring.datasource.url", "jdbc:h2:file:" + dbFile.toAbsolutePath().toString().replace('\\', '/') + ";DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
				"spring.datasource.username", "sa",
				"spring.datasource.password", "",
				"spring.datasource.driver-class-name", "org.h2.Driver",
				"spring.jpa.hibernate.ddl-auto", "validate",
				"spring.jpa.open-in-view", "false",
				"spring.flyway.locations", "classpath:db/migration",
				"spring.flyway.enabled", "true",
				"spring.main.web-application-type", "none");
		Map<String, String> previousValues = new HashMap<>();
		properties.forEach((key, value) -> {
			previousValues.put(key, System.getProperty(key));
			System.setProperty(key, value);
		});

		try {
			try (ConfigurableApplicationContext firstContext = new SpringApplicationBuilder(BookmarkManagerApplication.class)
					.web(WebApplicationType.NONE)
					.run()) {
				BookmarkService bookmarkService = firstContext.getBean(BookmarkService.class);
				BookmarkRepository bookmarkRepository = firstContext.getBean(BookmarkRepository.class);
				BookmarkForm form = new BookmarkForm();
				form.setUrl("https://example.com/restart");
				form.setTitle("Restart example");
				form.setTags(" Java, spring, JAVA, ");
				Bookmark saved = bookmarkService.save(form);
				Bookmark persistedAfterSave = bookmarkRepository.findByUrlWithTags("https://example.com/restart").orElseThrow();

				BookmarkForm updatedForm = new BookmarkForm();
				updatedForm.setUrl("https://example.com/restart-updated");
				updatedForm.setTitle("Restart example updated");
				updatedForm.setTags(" spring, edit ");
				Bookmark updated = bookmarkService.update(saved.getId(), updatedForm);
				Bookmark persistedAfterUpdate = bookmarkRepository.findByUrlWithTags("https://example.com/restart-updated").orElseThrow();
				assertThat(updated.getId()).isEqualTo(saved.getId());
				assertThat(persistedAfterUpdate.getCreatedAt()).isEqualTo(persistedAfterSave.getCreatedAt());
				createdAtBeforeRestart[0] = persistedAfterUpdate.getCreatedAt();
			}

			try (ConfigurableApplicationContext secondContext = new SpringApplicationBuilder(BookmarkManagerApplication.class)
					.web(WebApplicationType.NONE)
					.run()) {
				BookmarkRepository bookmarkRepository = secondContext.getBean(BookmarkRepository.class);
				Bookmark restored = bookmarkRepository.findByUrlWithTags("https://example.com/restart-updated").orElseThrow();
				assertThat(restored.getTitle()).isEqualTo("Restart example updated");
				assertThat(restored.getTags()).extracting(com.krish.bookmarks.model.BookmarkTag::getValue).containsExactly("spring", "edit");
				assertThat(restored.getCreatedAt()).isEqualTo(createdAtBeforeRestart[0]);
			}
		} finally {
			OVERRIDE_KEYS.forEach(key -> {
				String previousValue = previousValues.get(key);
				if (previousValue == null) {
					System.clearProperty(key);
				} else {
					System.setProperty(key, previousValue);
				}
			});
		}
	}

	@Test
	void deletedBookmarkRemainsDeletedAcrossRestart(@TempDir Path tempDir) {
		Path dbFile = tempDir.resolve("bookmarks-delete-test");
		Map<String, String> properties = Map.of(
				"spring.datasource.url", "jdbc:h2:file:" + dbFile.toAbsolutePath().toString().replace('\\', '/') + ";DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
				"spring.datasource.username", "sa",
				"spring.datasource.password", "",
				"spring.datasource.driver-class-name", "org.h2.Driver",
				"spring.jpa.hibernate.ddl-auto", "validate",
				"spring.jpa.open-in-view", "false",
				"spring.flyway.locations", "classpath:db/migration",
				"spring.flyway.enabled", "true",
				"spring.main.web-application-type", "none");
		Map<String, String> previousValues = new HashMap<>();
		properties.forEach((key, value) -> {
			previousValues.put(key, System.getProperty(key));
			System.setProperty(key, value);
		});

		try {
			Long deletedId;
			try (ConfigurableApplicationContext firstContext = new SpringApplicationBuilder(BookmarkManagerApplication.class)
					.web(WebApplicationType.NONE)
					.run()) {
				BookmarkService bookmarkService = firstContext.getBean(BookmarkService.class);
				BookmarkForm form = new BookmarkForm();
				form.setUrl("https://example.com/delete-restart");
				form.setTitle("Delete restart example");
				form.setTags(" java, spring ");
				Bookmark saved = bookmarkService.save(form);
				deletedId = saved.getId();
				bookmarkService.delete(saved.getId());
			}

			try (ConfigurableApplicationContext secondContext = new SpringApplicationBuilder(BookmarkManagerApplication.class)
					.web(WebApplicationType.NONE)
					.run()) {
				BookmarkRepository bookmarkRepository = secondContext.getBean(BookmarkRepository.class);
				assertThat(bookmarkRepository.findByIdWithTags(deletedId)).isEmpty();
				assertThat(bookmarkRepository.findByUrlWithTags("https://example.com/delete-restart")).isEmpty();
			}
		} finally {
			OVERRIDE_KEYS.forEach(key -> {
				String previousValue = previousValues.get(key);
				if (previousValue == null) {
					System.clearProperty(key);
				} else {
					System.setProperty(key, previousValue);
				}
			});
		}
	}
}



