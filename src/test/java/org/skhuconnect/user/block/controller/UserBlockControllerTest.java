package org.skhuconnect.user.block.controller;

import org.junit.jupiter.api.Test;
import org.skhuconnect.user.block.dto.UserBlockResponse;
import org.skhuconnect.user.block.exception.UserBlockException;
import org.skhuconnect.user.block.exception.UserBlockExceptionHandler;
import org.skhuconnect.user.block.service.UserBlockService;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserBlockControllerTest {
    @Test void createsPermanentBlockAndMapsFailures() throws Exception {
        UserBlockService service = mock(UserBlockService.class);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new UserBlockController(service))
                .setControllerAdvice(new UserBlockExceptionHandler()).build();
        when(service.block(eq(1L), any())).thenReturn(new UserBlockResponse(null));
        mvc.perform(post("/connect/users/me/blocks").requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"targetType\":\"COMMENT\",\"contentId\":2}"))
                .andExpect(status().isCreated());

        doThrow(new UserBlockException(UserBlockException.Reason.ALREADY_BLOCKED)).when(service).block(eq(1L), any());
        mvc.perform(post("/connect/users/me/blocks").requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"targetType\":\"COMMENT\",\"contentId\":2}"))
                .andExpect(status().isConflict());
    }
}
