package org.skhuconnect.comment.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skhuconnect.comment.dto.request.CommentCreateRequest;
import org.skhuconnect.comment.dto.response.CommentLikeResponse;
import org.skhuconnect.comment.dto.response.CommentPageResponse;
import org.skhuconnect.comment.dto.response.CommentResponse;
import org.skhuconnect.comment.exception.CommentException;
import org.skhuconnect.comment.exception.CommentExceptionHandler;
import org.skhuconnect.comment.service.CommentLikeService;
import org.skhuconnect.comment.service.CommentService;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CommentControllerTest {

    private CommentService commentService;
    private CommentLikeService likeService;
    private MockMvc mockMvc;
    private CommentResponse response;

    @BeforeEach
    void setUp() {
        commentService = mock(CommentService.class);
        likeService = mock(CommentLikeService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new CommentController(commentService, likeService))
                .setControllerAdvice(new CommentExceptionHandler())
                .build();
        LocalDateTime now = LocalDateTime.of(2026, 8, 6, 12, 0);
        response = new CommentResponse(
                5L, "content", 2, 0, true, false, false, now, now);
    }

    @Test
    void createReturnsCreatedWithoutIdentityInputFields() throws Exception {
        when(commentService.create(any(Long.class), any(Long.class),
                any(CommentCreateRequest.class))).thenReturn(response);

        mockMvc.perform(post("/connect/petitions/10/comments")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"content\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.anonymousNumber").value(2))
                .andExpect(jsonPath("$.myComment").value(true))
                .andExpect(jsonPath("$.writerId").doesNotExist())
                .andExpect(jsonPath("$.userId").doesNotExist());
    }

    @Test
    void publicListWorksWithoutUserAttribute() throws Exception {
        when(commentService.findAll(null, 10L, 0, 20)).thenReturn(
                new CommentPageResponse(List.of(), 0, 20, 0, 0, true, true));

        mockMvc.perform(get("/connect/petitions/10/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty());

        verify(commentService).findAll(null, 10L, 0, 20);
    }

    @Test
    void updateDeleteLikeAndCancelDelegateAuthenticatedUser() throws Exception {
        when(commentService.update(any(), any(), any(), any())).thenReturn(response);
        when(likeService.like(1L, 10L, 5L))
                .thenReturn(new CommentLikeResponse(5L, 1, true));
        when(likeService.cancel(1L, 10L, 5L))
                .thenReturn(new CommentLikeResponse(5L, 0, false));

        mockMvc.perform(put("/connect/petitions/10/comments/5")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"updated\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/connect/petitions/10/comments/5")
                        .requestAttr("userId", 1L))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
        mockMvc.perform(post("/connect/petitions/10/comments/5/likes")
                        .requestAttr("userId", 1L))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.liked").value(true));
        mockMvc.perform(delete("/connect/petitions/10/comments/5/likes")
                        .requestAttr("userId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.liked").value(false));
    }

    @Test
    void duplicateLikeAndAnonymousConflictReturnConflict() throws Exception {
        doThrow(new CommentException(CommentException.Reason.COMMENT_LIKE_DUPLICATE))
                .when(likeService).like(1L, 10L, 5L);
        mockMvc.perform(post("/connect/petitions/10/comments/5/likes")
                        .requestAttr("userId", 1L))
                .andExpect(status().isConflict());

        doThrow(new CommentException(CommentException.Reason.ANONYMOUS_NUMBER_CONFLICT))
                .when(commentService).create(any(), any(), any());
        mockMvc.perform(post("/connect/petitions/10/comments")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"content\"}"))
                .andExpect(status().isConflict());
    }
}
