package com.onlinelibrary.book.service;

import com.onlinelibrary.book.entity.Review;
import com.onlinelibrary.book.exception.ReviewException;
import com.onlinelibrary.book.repository.ReviewRepository;
import com.onlinelibrary.book.requestmodels.ReviewRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    ReviewRepository reviewRepository;

    @InjectMocks
    ReviewService reviewService;

    @Test
    void postReview_newReview_savesWithCorrectFields() {
        // -- arrange --
        ReviewRequest request = new ReviewRequest();
        request.setBookId(1L);
        request.setRating(4.5);
        request.setReviewDescription("Great book!");

        given(reviewRepository.findByUserEmailAndBookId("user@test.com", 1L))
                .willReturn(null); // no existing review

        // -- act --
        reviewService.postReview("user@test.com", request);

        // -- assert --
        ArgumentCaptor<Review> reviewCaptor = ArgumentCaptor.forClass(Review.class);
        verify(reviewRepository).save(reviewCaptor.capture());

        Review saved = reviewCaptor.getValue();
        assertThat(saved.getUserEmail()).isEqualTo("user@test.com");
        assertThat(saved.getBookId()).isEqualTo(1L);
        assertThat(saved.getRating()).isEqualTo(4.5);
        assertThat(saved.getReviewDescription()).isEqualTo("Great book!");
        assertThat(saved.getDate()).isNotNull();
    }

    @Test
    void postReview_alreadyReviewed_throwsAndNeverSaves() {
        // -- arrange --
        ReviewRequest request = new ReviewRequest();
        request.setBookId(1L);
        request.setRating(3.0);

        given(reviewRepository.findByUserEmailAndBookId("user@test.com", 1L))
                .willReturn(new Review()); // existing review found

        // -- act + assert --
        assertThatThrownBy(() -> reviewService.postReview("user@test.com", request))
                .isInstanceOf(ReviewException.class)
                .hasMessageContaining("already");

        verify(reviewRepository, never()).save(any());
    }

    @Test
    void userReviewListed_reviewExists_returnsTrue() {
        given(reviewRepository.findByUserEmailAndBookId("user@test.com", 1L))
                .willReturn(new Review());

        assertThat(reviewService.userReviewListed("user@test.com", 1L)).isTrue();
    }

    @Test
    void userReviewListed_noReview_returnsFalse() {
        given(reviewRepository.findByUserEmailAndBookId("user@test.com", 1L))
                .willReturn(null);

        assertThat(reviewService.userReviewListed("user@test.com", 1L)).isFalse();
    }

    @Test
    void deleteReviewByBookId_delegatesToRepository() {
        reviewService.deleteReviewByBookId(42L);

        verify(reviewRepository).deleteAllByBookId(42L);
        verifyNoMoreInteractions(reviewRepository);
    }
}