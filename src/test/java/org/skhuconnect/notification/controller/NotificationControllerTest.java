package org.skhuconnect.notification.controller;

import org.junit.jupiter.api.Test;
import org.skhuconnect.notification.dto.*;
import org.skhuconnect.notification.exception.NotificationExceptionHandler;
import org.skhuconnect.notification.service.NotificationService;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class NotificationControllerTest {
    @Test void supportsListCountAndReadApis() throws Exception {
        NotificationService service=mock(NotificationService.class);
        when(service.findAll(1L,0,20)).thenReturn(new NotificationPageResponse(java.util.List.of(),0,20,0,0,true,true));
        when(service.unreadCount(1L)).thenReturn(new UnreadNotificationCountResponse(2));
        MockMvc mvc=standaloneSetup(new NotificationController(service))
                .setControllerAdvice(new NotificationExceptionHandler()).build();
        mvc.perform(get("/connect/notifications").requestAttr("userId",1L))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content").isArray());
        mvc.perform(get("/connect/notifications/unread-count").requestAttr("userId",1L))
                .andExpect(status().isOk()).andExpect(jsonPath("$.unreadCount").value(2));
        mvc.perform(patch("/connect/notifications/read-all").requestAttr("userId",1L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());
    }
}
