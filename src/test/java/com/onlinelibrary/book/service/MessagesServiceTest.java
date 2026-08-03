package com.onlinelibrary.book.service;

import com.onlinelibrary.book.entity.Message;
import com.onlinelibrary.book.exception.MessageException;
import com.onlinelibrary.book.repository.MessageRepository;
import com.onlinelibrary.book.requestmodels.AdminQuestionRequest;
import com.onlinelibrary.book.requestmodels.MessageRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MessagesServiceTest {

    @Mock
    MessageRepository messageRepository;

    @InjectMocks
    MessagesService messagesService;

    @Test
    void postMessage_savesWithCorrectFields() {
        MessageRequest request = new MessageRequest();
        request.setTitle("Help needed");
        request.setQuestion("Where is my book?");

        messagesService.postMessage(request, "user@test.com");

        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(messageRepository).save(captor.capture());

        Message saved = captor.getValue();
        assertThat(saved.getTitle()).isEqualTo("Help needed");
        assertThat(saved.getQuestion()).isEqualTo("Where is my book?");
        assertThat(saved.getUserEmail()).isEqualTo("user@test.com");
    }

    @Test
    void putMessage_success_closesMessageWithAdminResponse() {
        Message openMessage = new Message("Help needed", "Where is my book?");
        openMessage.setUserEmail("user@test.com");
        openMessage.setClosed(false);

        given(messageRepository.findById(1L)).willReturn(Optional.of(openMessage));

        AdminQuestionRequest request = new AdminQuestionRequest();
        request.setId(1L);
        request.setResponse("Your book is on shelf 3.");

        messagesService.putMessage(request, "admin@test.com");

        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(messageRepository).save(captor.capture());

        Message saved = captor.getValue();
        assertThat(saved.getAdminEmail()).isEqualTo("admin@test.com");
        assertThat(saved.getResponse()).isEqualTo("Your book is on shelf 3.");
        assertThat(saved.isClosed()).isTrue();
    }

    @Test
    void putMessage_notFound_throwsAndNeverSaves() {
        given(messageRepository.findById(99L)).willReturn(Optional.empty());

        AdminQuestionRequest request = new AdminQuestionRequest();
        request.setId(99L);
        request.setResponse("Some answer");

        assertThatThrownBy(() -> messagesService.putMessage(request, "admin@test.com"))
                .isInstanceOf(MessageException.class)
                .hasMessageContaining("not found");

        verify(messageRepository, never()).save(any());
    }

    @Test
    void putMessage_alreadyClosed_throwsAndNeverSaves() {
        Message closedMessage = new Message("Old question", "Old text");
        closedMessage.setClosed(true);

        given(messageRepository.findById(2L)).willReturn(Optional.of(closedMessage));

        AdminQuestionRequest request = new AdminQuestionRequest();
        request.setId(2L);
        request.setResponse("Duplicate answer");

        assertThatThrownBy(() -> messagesService.putMessage(request, "admin@test.com"))
                .isInstanceOf(MessageException.class)
                .hasMessageContaining("already closed");

        verify(messageRepository, never()).save(any());
    }
}