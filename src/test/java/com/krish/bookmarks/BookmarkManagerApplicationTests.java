package com.krish.bookmarks;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BookmarkManagerApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void rendersLandingPageWithAddLink() throws Exception {
		mockMvc.perform(get("/bookmarks"))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Personal Bookmark Manager")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("/bookmarks/new")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("No bookmarks yet. Add your first bookmark.")));
	}

	@Test
	void rendersAddForm() throws Exception {
		mockMvc.perform(get("/bookmarks/new"))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Add bookmark")));
	}
}

