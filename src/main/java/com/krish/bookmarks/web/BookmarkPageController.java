package com.krish.bookmarks.web;

import java.util.List;
import java.util.TreeSet;

import com.krish.bookmarks.service.BookmarkService;
import com.krish.bookmarks.service.BookmarkValidationException;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponentsBuilder;

import org.springframework.util.StringUtils;

@Controller
public class BookmarkPageController {

	private final BookmarkService bookmarkService;

	public BookmarkPageController(BookmarkService bookmarkService) {
		this.bookmarkService = bookmarkService;
	}

	@GetMapping({"/", "/bookmarks"})
	public String index(@RequestParam(name = "search", defaultValue = "") String searchText,
			@RequestParam(name = "tag", defaultValue = "") String selectedTag, Model model) {
		String normalizedSearchText = trimToEmpty(searchText);
		String normalizedSelectedTag = normalizeTag(selectedTag);
		model.addAttribute("bookmarks", bookmarkService.listBookmarks(normalizedSearchText, normalizedSelectedTag));
		model.addAttribute("availableTags", availableTags(normalizedSelectedTag));
		model.addAttribute("hasBookmarks", bookmarkService.hasBookmarks());
		model.addAttribute("searchText", normalizedSearchText);
		model.addAttribute("selectedTag", normalizedSelectedTag);
		return "index";
	}

	@GetMapping("/bookmarks/new")
	public String newBookmark(@RequestParam(name = "search", defaultValue = "") String searchText,
			@RequestParam(name = "tag", defaultValue = "") String selectedTag,
			Model model) {
		if (!model.containsAttribute("bookmarkForm")) {
			model.addAttribute("bookmarkForm", new BookmarkForm());
		}
		String normalizedSearchText = trimToEmpty(searchText);
		String normalizedSelectedTag = normalizeTag(selectedTag);
		model.addAttribute("searchText", normalizedSearchText);
		model.addAttribute("selectedTag", normalizedSelectedTag);
		model.addAttribute("formAction", bookmarkFormAction(null, normalizedSearchText, normalizedSelectedTag));
		model.addAttribute("cancelUrl", listUrl(normalizedSearchText, normalizedSelectedTag));
		return "bookmark-form";
	}

	@GetMapping("/bookmarks/{id}/edit")
	public String editBookmark(@PathVariable Long id,
			@RequestParam(name = "search", defaultValue = "") String searchText,
			@RequestParam(name = "tag", defaultValue = "") String selectedTag,
			Model model) {
		if (!model.containsAttribute("bookmarkForm")) {
			model.addAttribute("bookmarkForm", toForm(bookmarkService.getBookmarkForEdit(id)));
		}
		String normalizedSearchText = trimToEmpty(searchText);
		String normalizedSelectedTag = normalizeTag(selectedTag);
		model.addAttribute("searchText", normalizedSearchText);
		model.addAttribute("selectedTag", normalizedSelectedTag);
		model.addAttribute("formAction", bookmarkFormAction(id, normalizedSearchText, normalizedSelectedTag));
		model.addAttribute("cancelUrl", listUrl(normalizedSearchText, normalizedSelectedTag));
		return "bookmark-form";
	}

	@GetMapping("/bookmarks/{id}/delete")
	public String deleteBookmark(@PathVariable Long id,
			@RequestParam(name = "search", defaultValue = "") String searchText,
			@RequestParam(name = "tag", defaultValue = "") String selectedTag,
			Model model) {
		if (!model.containsAttribute("bookmark")) {
			model.addAttribute("bookmark", bookmarkService.getBookmarkForEdit(id));
		}
		String normalizedSearchText = trimToEmpty(searchText);
		String normalizedSelectedTag = normalizeTag(selectedTag);
		model.addAttribute("searchText", normalizedSearchText);
		model.addAttribute("selectedTag", normalizedSelectedTag);
		model.addAttribute("deleteAction", deleteAction(id, normalizedSearchText, normalizedSelectedTag));
		model.addAttribute("cancelUrl", listUrl(normalizedSearchText, normalizedSelectedTag));
		return "bookmark-delete";
	}

	@PostMapping("/bookmarks")
	public String saveBookmark(@ModelAttribute("bookmarkForm") BookmarkForm bookmarkForm,
			BindingResult bindingResult,
			@RequestParam(name = "search", defaultValue = "") String searchText,
			@RequestParam(name = "tag", defaultValue = "") String selectedTag,
			Model model,
			RedirectAttributes redirectAttributes) {
		try {
			bookmarkService.save(bookmarkForm);
		} catch (BookmarkValidationException ex) {
			ex.applyTo(bindingResult);
		}

		if (bindingResult.hasErrors()) {
			String normalizedSearchText = trimToEmpty(searchText);
			String normalizedSelectedTag = normalizeTag(selectedTag);
			model.addAttribute("searchText", normalizedSearchText);
			model.addAttribute("selectedTag", normalizedSelectedTag);
			model.addAttribute("formAction", bookmarkFormAction(null, normalizedSearchText, normalizedSelectedTag));
			model.addAttribute("cancelUrl", listUrl(normalizedSearchText, normalizedSelectedTag));
			return "bookmark-form";
		}

		redirectAttributes.addFlashAttribute("successMessage", "Bookmark saved.");
		return redirectToList(searchText, selectedTag);
	}

	@PostMapping("/bookmarks/{id}")
	public String updateBookmark(@PathVariable Long id,
			@ModelAttribute("bookmarkForm") BookmarkForm bookmarkForm,
			BindingResult bindingResult,
			@RequestParam(name = "search", defaultValue = "") String searchText,
			@RequestParam(name = "tag", defaultValue = "") String selectedTag,
			Model model,
			RedirectAttributes redirectAttributes) {
		try {
			bookmarkService.update(id, bookmarkForm);
		} catch (BookmarkValidationException ex) {
			ex.applyTo(bindingResult);
		}

		if (bindingResult.hasErrors()) {
			String normalizedSearchText = trimToEmpty(searchText);
			String normalizedSelectedTag = normalizeTag(selectedTag);
			model.addAttribute("searchText", normalizedSearchText);
			model.addAttribute("selectedTag", normalizedSelectedTag);
			model.addAttribute("formAction", bookmarkFormAction(id, normalizedSearchText, normalizedSelectedTag));
			model.addAttribute("cancelUrl", listUrl(normalizedSearchText, normalizedSelectedTag));
			return "bookmark-form";
		}

		redirectAttributes.addFlashAttribute("successMessage", "Bookmark updated.");
		return redirectToList(searchText, selectedTag);
	}

	@PostMapping("/bookmarks/{id}/delete")
	public String confirmDeleteBookmark(@PathVariable Long id,
			@RequestParam(name = "search", defaultValue = "") String searchText,
			@RequestParam(name = "tag", defaultValue = "") String selectedTag,
			RedirectAttributes redirectAttributes) {
		bookmarkService.delete(id);
		redirectAttributes.addFlashAttribute("successMessage", "Bookmark deleted.");
		return redirectToList(searchText, selectedTag);
	}

	private BookmarkForm toForm(com.krish.bookmarks.model.Bookmark bookmark) {
		BookmarkForm form = new BookmarkForm();
		form.setId(bookmark.getId());
		form.setUrl(bookmark.getUrl());
		form.setTitle(bookmark.getTitle());
		form.setTags(bookmark.getTags().stream().map(com.krish.bookmarks.model.BookmarkTag::getValue).reduce((left, right) -> left + ", " + right).orElse(""));
		return form;
	}

	private List<String> availableTags(String selectedTag) {
		TreeSet<String> tagOptions = new TreeSet<>(bookmarkService.listTagOptions());
		if (StringUtils.hasText(selectedTag)) {
			tagOptions.add(selectedTag);
		}
		return List.copyOf(tagOptions);
	}

	private String redirectToList(String searchText, String selectedTag) {
		return "redirect:" + listUrl(trimToEmpty(searchText), normalizeTag(selectedTag));
	}

	private String bookmarkFormAction(Long id, String searchText, String selectedTag) {
		if (id == null) {
			return listUrl(searchText, selectedTag);
		}
		UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/bookmarks/{id}").queryParam("search", searchText).queryParam("tag", selectedTag);
		return builder.buildAndExpand(id).encode().toUriString();
	}

	private String deleteAction(Long id, String searchText, String selectedTag) {
		UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/bookmarks/{id}/delete").queryParam("search", searchText).queryParam("tag", selectedTag);
		return builder.buildAndExpand(id).encode().toUriString();
	}

	private String listUrl(String searchText, String selectedTag) {
		UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/bookmarks");
		if (StringUtils.hasText(searchText)) {
			builder.queryParam("search", searchText);
		}
		if (StringUtils.hasText(selectedTag)) {
			builder.queryParam("tag", selectedTag);
		}
		return builder.build().encode().toUriString();
	}

	private String trimToEmpty(String value) {
		return value == null ? "" : value.trim();
	}

	private String normalizeTag(String value) {
		String trimmed = trimToEmpty(value);
		return trimmed.isEmpty() ? "" : trimmed.toLowerCase(java.util.Locale.ROOT);
	}
}

