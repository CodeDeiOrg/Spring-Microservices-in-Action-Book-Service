package com.onlinelibrary.book.controller;

import com.onlinelibrary.book.exception.AdminControlException;
import com.onlinelibrary.book.exception.EmailMissingException;
import com.onlinelibrary.book.requestmodels.ReviewRequest;
import com.onlinelibrary.book.service.ReviewService;
import com.onlinelibrary.book.utils.ExtractJWT;
import org.springframework.web.bind.annotation.*;

import static com.onlinelibrary.book.utils.Constants.USER_TYPE;
import static com.onlinelibrary.book.utils.Constants.ADMIN;
import static com.onlinelibrary.book.utils.Constants.ADMINISTRATION_PAGE_ONLY;
import static com.onlinelibrary.book.utils.Constants.SUB;
import static com.onlinelibrary.book.utils.Constants.EMAIL_MISSING;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {
    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @DeleteMapping("/secure/{bookId}")
    public void deleteBookReview(@RequestHeader(value = "Authorization") String token,
                                 @PathVariable Long bookId) {
        String admin = ExtractJWT.payloadJWTExtraction(token, USER_TYPE);
        if (admin == null || !admin.equals(ADMIN)) {
            throw new AdminControlException(ADMINISTRATION_PAGE_ONLY);
        }
        reviewService.deleteReviewByBookId(bookId);
    }

    @GetMapping("/secure/user/book")
    public Boolean reviewBookByUser(@RequestHeader(value = "Authorization") String token,
                                    @RequestParam Long bookId) {
        String userEmail = ExtractJWT.payloadJWTExtraction(token, SUB);
        if (userEmail == null) {
            throw new EmailMissingException(EMAIL_MISSING);
        }
        return reviewService.userReviewListed(userEmail, bookId);
    }

    @PostMapping("/secure")
    public void postReview(@RequestHeader(value = "Authorization") String token,
                           @RequestBody ReviewRequest reviewRequest) {
        String userEmail = ExtractJWT.payloadJWTExtraction(token, SUB);
        if (userEmail == null) {
            throw new EmailMissingException(EMAIL_MISSING);
        }
        reviewService.postReview(userEmail, reviewRequest);
    }

}
