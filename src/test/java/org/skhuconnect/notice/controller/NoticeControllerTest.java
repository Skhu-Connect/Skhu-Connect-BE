package org.skhuconnect.notice.controller;

import org.junit.jupiter.api.Test;
import org.skhuconnect.notice.dto.NoticeResponse;
import org.skhuconnect.notice.entity.NoticeStatus;
import org.skhuconnect.notice.exception.NoticeException;
import org.skhuconnect.notice.exception.NoticeExceptionHandler;
import org.skhuconnect.notice.service.NoticeService;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class NoticeControllerTest {
    @Test
    void returnsBannerAndDismissesNotice() throws Exception {
        NoticeService service = mock(NoticeService.class);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new NoticeController(service))
                .setControllerAdvice(new NoticeExceptionHandler()).build();
        when(service.banner(1L)).thenReturn(Optional.of(new NoticeResponse(
                2L, "공지", "내용", NoticeStatus.PUBLISHED, 7L, null, null, null)));

        mvc.perform(get("/connect/users/me/notices/banner").requestAttr("userId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2));

        mvc.perform(post("/connect/users/me/notices/2/dismiss").requestAttr("userId", 1L))
                .andExpect(status().isNoContent());
    }

    @Test
    void bannerReturnsNoContentWhenNothingVisibleAndMapsMissingNotice() throws Exception {
        NoticeService service = mock(NoticeService.class);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new NoticeController(service))
                .setControllerAdvice(new NoticeExceptionHandler()).build();
        when(service.banner(1L)).thenReturn(Optional.empty());

        mvc.perform(get("/connect/users/me/notices/banner").requestAttr("userId", 1L))
                .andExpect(status().isNoContent());

        doThrow(new NoticeException(NoticeException.Reason.NOTICE_NOT_FOUND))
                .when(service).dismiss(1L, 99L);
        mvc.perform(post("/connect/users/me/notices/99/dismiss").requestAttr("userId", 1L))
                .andExpect(status().isNotFound());
    }
}
