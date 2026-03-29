package com.onlinelibrary.book.controller;

import com.onlinelibrary.book.entity.Message;
import com.onlinelibrary.book.exception.AdminControlException;
import com.onlinelibrary.book.requestmodels.AdminQuestionRequest;
import com.onlinelibrary.book.service.MessagesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.onlinelibrary.book.utils.Constants.ROLES_CLAIM;
import static com.onlinelibrary.book.utils.Constants.ADMIN;
import static com.onlinelibrary.book.utils.Constants.ADMINISTRATION_PAGE_ONLY;

@RestController
@RequestMapping("/api/messages")
public class MessagesController {
    private final MessagesService messagesService;

    @Autowired
    public MessagesController(MessagesService messagesService) {
        this.messagesService = messagesService;
    }

    @PostMapping("/secure/add/message")
    public void postMessage(@AuthenticationPrincipal Jwt jwt,
                            @RequestBody Message messageRequest) {
        String userEmail = jwt.getClaim("email");
        messagesService.postMessage(messageRequest, userEmail);
    }

    @PutMapping("/secure/admin/message")
    public void putMessage(@AuthenticationPrincipal Jwt jwt,
                           @RequestBody AdminQuestionRequest adminQuestionRequest) {
        List<String> roles = jwt.getClaim(ROLES_CLAIM);
        if (roles == null || !roles.contains(ADMIN)) {
            throw new AdminControlException(ADMINISTRATION_PAGE_ONLY);
        }
        String userEmail = jwt.getClaim("email");
        messagesService.putMessage(adminQuestionRequest, userEmail);
    }

}