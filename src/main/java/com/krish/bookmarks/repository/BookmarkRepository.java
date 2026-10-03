package com.krish.bookmarks.repository;

import java.util.List;
import java.util.Optional;

import com.krish.bookmarks.model.Bookmark;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {

	boolean existsByUrl(String url);

	boolean existsByUrlAndIdNot(String url, Long id);

	@EntityGraph(attributePaths = "tags")
	@Query("select b from Bookmark b order by b.createdAt desc, b.id desc")
	List<Bookmark> findAllByOrderByCreatedAtDescIdDesc();

	@EntityGraph(attributePaths = "tags")
	@Query("select b from Bookmark b where b.id = :id")
	Optional<Bookmark> findByIdWithTags(@Param("id") Long id);

	@Query("select distinct t.value from BookmarkTag t order by t.value asc")
	List<String> findDistinctTagValuesOrderByValueAsc();

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("delete from BookmarkTag t where t.bookmark.id = :bookmarkId")
	void deleteTagsByBookmarkId(@Param("bookmarkId") Long bookmarkId);

	@EntityGraph(attributePaths = "tags")
	@Query("select b from Bookmark b where b.url = :url")
	Optional<Bookmark> findByUrlWithTags(@Param("url") String url);
}

