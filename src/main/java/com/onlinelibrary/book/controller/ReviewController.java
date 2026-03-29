package com.onlinelibrary.book.controller;

import com.onlinelibrary.book.exception.AdminControlException;
import com.onlinelibrary.book.exception.EmailMissingException;
import com.onlinelibrary.book.requestmodels.ReviewRequest;
import com.onlinelibrary.book.service.ReviewService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.onlinelibrary.book.utils.Constants.ROLES_CLAIM;
import static com.onlinelibrary.book.utils.Constants.ADMIN;
import static com.onlinelibrary.book.utils.Constants.ADMINISTRATION_PAGE_ONLY;
import static com.onlinelibrary.book.utils.Constants.EMAIL_MISSING;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {
    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @DeleteMapping("/secure/{bookId}")
    public void deleteBookReview(@AuthenticationPrincipal Jwt jwt,
                                 @PathVariable Long bookId) {
        List<String> roles = jwt.getClaim(ROLES_CLAIM);
        if (roles == null || !roles.contains(ADMIN)) {
            throw new AdminControlException(ADMINISTRATION_PAGE_ONLY);
        }
        reviewService.deleteReviewByBookId(bookId);
    }

    @GetMapping("/secure/user/book")
    public Boolean reviewBookByUser(@AuthenticationPrincipal Jwt jwt,
                                    @RequestParam Long bookId) {
        String userEmail = jwt.getClaim("email");
        if (userEmail == null) {
            throw new EmailMissingException(EMAIL_MISSING);
        }
        return reviewService.userReviewListed(userEmail, bookId);
    }

    @PostMapping("/secure")
    public void postReview(@AuthenticationPrincipal Jwt jwt,
                           @RequestBody ReviewRequest reviewRequest) {
        String userEmail = jwt.getClaim("email");
        if (userEmail == null) {
            throw new EmailMissingException(EMAIL_MISSING);
        }
        reviewService.postReview(userEmail, reviewRequest);
    }

}