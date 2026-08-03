package com.onlinelibrary.book.utils;

import com.onlinelibrary.book.exception.EmailMissingException;
import lombok.experimental.UtilityClass;
import org.springframework.security.oauth2.jwt.Jwt;

import static com.onlinelibrary.book.utils.Constants.EMAIL;
import static com.onlinelibrary.book.utils.Constants.EMAIL_MISSING;

@UtilityClass
public class JwtUtils {

    public static String extractEmail(Jwt jwt) {
        String email = jwt.getClaim(EMAIL);
        if (email == null) {
            throw new EmailMissingException(EMAIL_MISSING);
        }
        return email;
    }
}