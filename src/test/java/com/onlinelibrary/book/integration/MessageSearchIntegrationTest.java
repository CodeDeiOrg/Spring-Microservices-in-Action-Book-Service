package com.onlinelibrary.book.integration;

import com.onlinelibrary.book.entity.Message;
import com.onlinelibrary.book.repository.MessageRepository;
import com.onlinelibrary.book.utils.Constants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class MessageSearchIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    MessageRepository messageRepository;

    @BeforeEach
    void setUp() {
        messageRepository.deleteAll();
        Message own = new Message("My issue", "My question");
        own.setUserEmail("user@test.com");
        own.setClosed(false);
        Message other = new Message("Someone else's issue", "Someone else's question");
        other.setUserEmail("someoneelse@test.com");
        other.setClosed(false);
        messageRepository.save(own);
        messageRepository.save(other);
    }

    @Test
    void findByUserEmail_ownEmail_returnsOnlyThatUsersMessages() throws Exception {
        mockMvc.perform(get("/messages/search/findByUserEmail?userEmail=user@test.com&page=0&size=5")
                        .with(jwt().jwt(j -> j.claim("email", "user@test.com"))))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("My issue"),
                        not(containsString("Someone else's issue")))));
    }

    @Test
    void findByUserEmail_anotherUsersEmail_isForbidden() throws Exception {
        mockMvc.perform(get("/messages/search/findByUserEmail?userEmail=someoneelse@test.com&page=0&size=5")
                        .with(jwt().jwt(j -> j.claim("email", "user@test.com"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void findByUserEmail_noToken_isUnauthorized() throws Exception {
        mockMvc.perform(get("/messages/search/findByUserEmail?userEmail=user@test.com&page=0&size=5"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void findByUserEmail_adminQueryingAnotherUser_isAllowed() throws Exception {
        mockMvc.perform(get("/messages/search/findByUserEmail?userEmail=someoneelse@test.com&page=0&size=5")
                        .with(jwt().authorities(new SimpleGrantedAuthority("admin"))
                                .jwt(j -> j.claim("email", "admin@test.com")
                                        .claim(Constants.ROLES_CLAIM, List.of("admin")))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Someone else's issue")));
    }

    @Test
    void findByClosed_asAdmin_isAllowed() throws Exception {
        mockMvc.perform(get("/messages/search/findByClosed?closed=false&page=0&size=5")
                        .with(jwt().authorities(new SimpleGrantedAuthority("admin"))
                                .jwt(j -> j.claim(Constants.ROLES_CLAIM, List.of("admin")))))
                .andExpect(status().isOk());
    }

    @Test
    void findByClosed_nonAdmin_isForbidden() throws Exception {
        mockMvc.perform(get("/messages/search/findByClosed?closed=false&page=0&size=5")
                        .with(jwt()))
                .andExpect(status().isForbidden());
    }

    @Test
    void findByClosed_noToken_isUnauthorized() throws Exception {
        mockMvc.perform(get("/messages/search/findByClosed?closed=false&page=0&size=5"))
                .andExpect(status().isUnauthorized());
    }
}
