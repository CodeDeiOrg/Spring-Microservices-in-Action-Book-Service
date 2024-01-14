package com.onlinelibrary.book.controller;

import com.onlinelibrary.book.entity.Message;
import com.onlinelibrary.book.exception.AdminControlException;
import com.onlinelibrary.book.requestmodels.AdminQuestionRequest;
import com.onlinelibrary.book.service.MessagesService;
import com.onlinelibrary.book.utils.ExtractJWT;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import static com.onlinelibrary.book.utils.Constants.USER_TYPE;
import static com.onlinelibrary.book.utils.Constants.ADMIN;
import static com.onlinelibrary.book.utils.Constants.ADMINISTRATION_PAGE_ONLY;
import static com.onlinelibrary.book.utils.Constants.SUB;

@RestController
@RequestMapping("/api/messages")
public class MessagesController {
    private final MessagesService messagesService;

    @Autowired
    public MessagesController(MessagesService messagesService) {
        this.messagesService = messagesService;
    }

    @PostMapping("/secure/add/message")
    public void postMessage(@RequestHeader(value = "Authorization") String token,
                            @RequestBody Message messageRequest) {
        String userEmail = ExtractJWT.payloadJWTExtraction(token, SUB);
        messagesService.postMessage(messageRequest, userEmail);
    }

    @PutMapping("/secure/admin/message")
    public void putMessage(@RequestHeader(value = "Authorization") String token,
                           @RequestBody AdminQuestionRequest adminQuestionRequest) {
        String userEmail = ExtractJWT.payloadJWTExtraction(token, SUB);
        String admin = ExtractJWT.payloadJWTExtraction(token, USER_TYPE);
        if (admin == null || !admin.equals(ADMIN)) {
            throw new AdminControlException(ADMINISTRATION_PAGE_ONLY);
        }
        messagesService.putMessage(adminQuestionRequest, userEmail);
    }

}

