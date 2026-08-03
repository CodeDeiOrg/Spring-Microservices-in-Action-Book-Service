package com.onlinelibrary.book.controller;

import com.onlinelibrary.book.requestmodels.AdminQuestionRequest;
import com.onlinelibrary.book.requestmodels.MessageRequest;
import com.onlinelibrary.book.service.MessagesService;
import com.onlinelibrary.book.utils.JwtUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/messages")
public class MessagesController {
    private final MessagesService messagesService;

    public MessagesController(MessagesService messagesService) {
        this.messagesService = messagesService;
    }

    @PostMapping("/secure/add/message")
    public void postMessage(@AuthenticationPrincipal Jwt jwt,
                            @RequestBody MessageRequest messageRequest) {
        messagesService.postMessage(messageRequest, JwtUtils.extractEmail(jwt));
    }

    @PreAuthorize("hasAuthority('admin')")
    @PutMapping("/secure/admin/message")
    public void putMessage(@AuthenticationPrincipal Jwt jwt,
                           @RequestBody AdminQuestionRequest adminQuestionRequest) {
        messagesService.putMessage(adminQuestionRequest, JwtUtils.extractEmail(jwt));
    }
}