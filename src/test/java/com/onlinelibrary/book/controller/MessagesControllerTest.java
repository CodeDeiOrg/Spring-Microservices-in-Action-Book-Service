package com.onlinelibrary.book.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.onlinelibrary.book.exception.MessageException;
import com.onlinelibrary.book.requestmodels.AdminQuestionRequest;
import com.onlinelibrary.book.requestmodels.MessageRequest;
import com.onlinelibrary.book.service.MessagesService;
import com.onlinelibrary.book.support.ControllerTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = MessagesController.class, excludeAutoConfiguration = OAuth2ResourceServerAutoConfiguration.class)
@Import(ControllerTestSupport.class)
class MessagesControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean MessagesService messagesService;
    @MockitoBean JwtDecoder jwtDecoder;

    // --- POST /api/messages/secure/add/message ---

    @Test
    void postMessage_withValidJwt_returns200AndDelegatesToService() throws Exception {
        MessageRequest request = new MessageRequest();
        request.setTitle("Help needed");
        request.setQuestion("Where is my book?");

        mockMvc.perform(post("/api/messages/secure/add/message")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(jwt().jwt(j -> j.claim("email", "user@test.com"))))
                .andExpect(status().isOk());

        verify(messagesService).postMessage(any(MessageRequest.class), anyString());
    }

    @Test
    void postMessage_withNoToken_returns401() throws Exception {
        MessageRequest request = new MessageRequest();
        request.setTitle("Help needed");
        request.setQuestion("Where is my book?");

        mockMvc.perform(post("/api/messages/secure/add/message")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(messagesService);
    }

    // --- PUT /api/messages/secure/admin/message ---

    @Test
    void putMessage_withAdminToken_returns200AndDelegatesToService() throws Exception {
        AdminQuestionRequest request = new AdminQuestionRequest();
        request.setId(1L);
        request.setResponse("Your book is on shelf 3.");

        mockMvc.perform(put("/api/messages/secure/admin/message")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(jwt().authorities(new SimpleGrantedAuthority("admin"))
                                   .jwt(j -> j.claim("email", "admin@test.com"))))
                .andExpect(status().isOk());

        verify(messagesService).putMessage(any(AdminQuestionRequest.class), anyString());
    }

    @Test
    void putMessage_withUserToken_returns403() throws Exception {
        AdminQuestionRequest request = new AdminQuestionRequest();
        request.setId(1L);
        request.setResponse("Some answer");

        mockMvc.perform(put("/api/messages/secure/admin/message")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(jwt()))
                .andExpect(status().isForbidden());

        verifyNoInteractions(messagesService);
    }

    @Test
    void putMessage_withNoToken_returns401() throws Exception {
        AdminQuestionRequest request = new AdminQuestionRequest();
        request.setId(1L);
        request.setResponse("Some answer");

        mockMvc.perform(put("/api/messages/secure/admin/message")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(messagesService);
    }

    @Test
    void putMessage_serviceThrowsMessageException_returns400() throws Exception {
        AdminQuestionRequest request = new AdminQuestionRequest();
        request.setId(99L);
        request.setResponse("Some answer");

        willThrow(new MessageException("Message not found"))
                .given(messagesService).putMessage(any(AdminQuestionRequest.class), anyString());

        mockMvc.perform(put("/api/messages/secure/admin/message")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(jwt().authorities(new SimpleGrantedAuthority("admin"))
                                   .jwt(j -> j.claim("email", "admin@test.com"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Message not found"));
    }
}