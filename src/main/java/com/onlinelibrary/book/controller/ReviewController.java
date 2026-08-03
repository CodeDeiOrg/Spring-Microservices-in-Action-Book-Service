package com.onlinelibrary.book.controller;

import com.onlinelibrary.book.requestmodels.ReviewRequest;
import com.onlinelibrary.book.service.ReviewService;
import com.onlinelibrary.book.utils.JwtUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {
    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PreAuthorize("hasAuthority('admin')")
    @DeleteMapping("/secure/{bookId}")
    public void deleteBookReview(@PathVariable Long bookId) {
        reviewService.deleteReviewByBookId(bookId);
    }

    @GetMapping("/secure/user/book")
    public Boolean reviewBookByUser(@AuthenticationPrincipal Jwt jwt,
                                    @RequestParam Long bookId) {
        return reviewService.userReviewListed(JwtUtils.extractEmail(jwt), bookId);
    }

    @PostMapping("/secure")
    public void postReview(@AuthenticationPrincipal Jwt jwt,
                           @RequestBody ReviewRequest reviewRequest) {
        reviewService.postReview(JwtUtils.extractEmail(jwt), reviewRequest);
    }
}