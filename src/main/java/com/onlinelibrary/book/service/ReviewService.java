package com.onlinelibrary.book.service;

import com.onlinelibrary.book.entity.Review;
import com.onlinelibrary.book.exception.ReviewException;
import com.onlinelibrary.book.repository.ReviewRepository;
import com.onlinelibrary.book.requestmodels.ReviewRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
@Transactional
public class ReviewService {
    private static final Logger logger = LoggerFactory.getLogger(ReviewService.class);
    private final ReviewRepository reviewRepository;
    private final Clock clock;

    public ReviewService(ReviewRepository reviewRepository, Clock clock) {
        this.reviewRepository = reviewRepository;
        this.clock = clock;
    }

    public void postReview(String userEmail, ReviewRequest reviewRequest) {
        logger.debug("Posting review");

        if (reviewRepository.findByUserEmailAndBookId(userEmail, reviewRequest.getBookId()) != null) {
            throw new ReviewException("Review already created");
        }

        Review review = new Review();
        review.setBookId(reviewRequest.getBookId());
        review.setRating(reviewRequest.getRating());
        review.setUserEmail(userEmail);
        review.setReviewDescription(reviewRequest.getReviewDescription());
        review.setDate(LocalDateTime.now(clock));
        reviewRepository.save(review);
    }

    public Boolean userReviewListed(String userEmail, Long bookId) {
        return reviewRepository.findByUserEmailAndBookId(userEmail, bookId) != null;
    }

    public void deleteReviewByBookId(Long bookId) {
        logger.debug("Deleting reviews for bookId={}", bookId);
        reviewRepository.deleteAllByBookId(bookId);
    }
}