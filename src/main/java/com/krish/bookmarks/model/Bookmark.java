package com.krish.bookmarks.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "bookmarks", uniqueConstraints = @UniqueConstraint(name = "uk_bookmarks_url", columnNames = "url"))
public class Bookmark {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "url", nullable = false, length = 2048)
	private String url;

	@Column(name = "title", nullable = false, length = 200)
	private String title;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@OneToMany(mappedBy = "bookmark", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
	@jakarta.persistence.OrderBy("id ASC")
	private List<BookmarkTag> tags = new ArrayList<>();

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}

	public List<BookmarkTag> getTags() {
		return tags;
	}

	public void setTags(List<BookmarkTag> tags) {
		this.tags = tags;
	}

	public void addTag(String value) {
		BookmarkTag tag = new BookmarkTag();
		tag.setBookmark(this);
		tag.setValue(value);
		tags.add(tag);
	}
}

