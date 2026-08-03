package com.onlinelibrary.book.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.onlinelibrary.book.exception.ReviewException;
import com.onlinelibrary.book.requestmodels.ReviewRequest;
import com.onlinelibrary.book.service.ReviewService;
import com.onlinelibrary.book.support.ControllerTestSupport;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = ReviewController.class, excludeAutoConfiguration = OAuth2ResourceServerAutoConfiguration.class)
@Import(ControllerTestSupport.class)
class ReviewControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean ReviewService reviewService;
    @MockitoBean JwtDecoder jwtDecoder;

    @Test
    void postReview_withValidJwt_returns200AndDelegatesToService() throws Exception {
        ReviewRequest request = new ReviewRequest();
        request.setBookId(1L);
        request.setRating(4.5);
        request.setReviewDescription("Great book!");

        mockMvc.perform(post("/api/reviews/secure")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(jwt().jwt(j -> j.claim("email", "user@test.com"))))
                .andExpect(status().isOk());

        ArgumentCaptor<ReviewRequest> requestCaptor = ArgumentCaptor.forClass(ReviewRequest.class);
        verify(reviewService).postReview(eq("user@test.com"), requestCaptor.capture());

        ReviewRequest captured = requestCaptor.getValue();
        assertThat(captured.getBookId()).isEqualTo(1L);
        assertThat(captured.getRating()).isEqualTo(4.5);
        assertThat(captured.getReviewDescription()).contains("Great book!");
    }

    @Test
    void postReview_withNoToken_returns401() throws Exception {
        ReviewRequest request = new ReviewRequest();
        request.setBookId(1L);
        request.setRating(4.5);

        mockMvc.perform(post("/api/reviews/secure")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(reviewService);
    }

    @Test
    void postReview_serviceThrowsReviewException_returns400() throws Exception {
        ReviewRequest request = new ReviewRequest();
        request.setBookId(1L);
        request.setRating(4.5);

        willThrow(new ReviewException("Review already created"))
                .given(reviewService).postReview(anyString(), any(ReviewRequest.class));

        mockMvc.perform(post("/api/reviews/secure")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(jwt().jwt(j -> j.claim("email", "user@test.com"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Review already created"));
    }

    // --- GET /api/reviews/secure/user/book ---

    @Test
    void reviewBookByUser_withValidJwt_returnsBoolean() throws Exception {
        given(reviewService.userReviewListed("user@test.com", 1L)).willReturn(true);

        mockMvc.perform(get("/api/reviews/secure/user/book")
                        .param("bookId", "1")
                        .with(jwt().jwt(j -> j.claim("email", "user@test.com"))))
                .andExpect(status().isOk());

        verify(reviewService).userReviewListed("user@test.com", 1L);
    }

    @Test
    void reviewBookByUser_withNoToken_returns401() throws Exception {
        mockMvc.perform(get("/api/reviews/secure/user/book")
                        .param("bookId", "1"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(reviewService);
    }

    // --- DELETE /api/reviews/secure/{bookId} ---

    @Test
    void deleteBookReview_withAdminToken_returns200() throws Exception {
        mockMvc.perform(delete("/api/reviews/secure/1")
                        .with(jwt().authorities(new SimpleGrantedAuthority("admin"))))
                .andExpect(status().isOk());

        verify(reviewService).deleteReviewByBookId(1L);
    }

    @Test
    void deleteBookReview_withUserToken_returns403() throws Exception {
        mockMvc.perform(delete("/api/reviews/secure/1")
                        .with(jwt()))
                .andExpect(status().isForbidden());

        verifyNoInteractions(reviewService);
    }

    @Test
    void deleteBookReview_withNoToken_returns401() throws Exception {
        mockMvc.perform(delete("/api/reviews/secure/1"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(reviewService);
    }
}