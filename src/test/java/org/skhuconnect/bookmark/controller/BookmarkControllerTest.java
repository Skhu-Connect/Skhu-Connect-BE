package org.skhuconnect.bookmark.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skhuconnect.bookmark.dto.response.BookmarkPageResponse;
import org.skhuconnect.bookmark.dto.response.BookmarkResponse;
import org.skhuconnect.bookmark.exception.BookmarkException;
import org.skhuconnect.bookmark.exception.BookmarkExceptionHandler;
import org.skhuconnect.bookmark.service.BookmarkService;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BookmarkControllerTest {

    private BookmarkService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = mock(BookmarkService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new BookmarkController(service))
                .setControllerAdvice(new BookmarkExceptionHandler())
                .build();
    }

    @Test
    void createReturnsCreated() throws Exception {
        when(service.create(1L, 10L)).thenReturn(new BookmarkResponse(10L, true));

        mockMvc.perform(post("/connect/petitions/10/bookmarks")
                        .requestAttr("userId", 1L))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.petitionId").value(10))
                .andExpect(jsonPath("$.bookmarked").value(true));
    }

    @Test
    void duplicateReturnsConflict() throws Exception {
        doThrow(new BookmarkException(BookmarkException.Reason.BOOKMARK_DUPLICATE))
                .when(service).create(1L, 10L);

        mockMvc.perform(post("/connect/petitions/10/bookmarks")
                        .requestAttr("userId", 1L))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title")
                        .value("Petition bookmark already exists"));
    }

    @Test
    void cancelReturnsNoContentAndMissingReturnsNotFound() throws Exception {
        mockMvc.perform(delete("/connect/petitions/10/bookmarks")
                        .requestAttr("userId", 1L))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
        verify(service).cancel(1L, 10L);

        doThrow(new BookmarkException(BookmarkException.Reason.BOOKMARK_NOT_FOUND))
                .when(service).cancel(1L, 11L);
        mockMvc.perform(delete("/connect/petitions/11/bookmarks")
                        .requestAttr("userId", 1L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Petition bookmark not found"));
    }

    @Test
    void findMineUsesDefaultsAndReturnsPage() throws Exception {
        when(service.findMine(1L, 0, 20)).thenReturn(new BookmarkPageResponse(
                List.of(), 0, 20, 0, 0, true, true));

        mockMvc.perform(get("/connect/petitions/bookmarks")
                        .requestAttr("userId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20));
    }

    @Test
    void invalidPageReturnsBadRequest() throws Exception {
        doThrow(new BookmarkException(BookmarkException.Reason.INVALID_PAGE))
                .when(service).findMine(1L, -1, 20);

        mockMvc.perform(get("/connect/petitions/bookmarks?page=-1")
                        .requestAttr("userId", 1L))
                .andExpect(status().isBadRequest());
    }
}
