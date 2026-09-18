package com.ricozknow.search;

import com.ricozknow.search.dto.SearchResultResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * GET /api/v1/search?q={query}&category={category}&page={page} — spec section 14.
 * OpenSearch itself is never exposed directly to external consumers; this is
 * the only door in.
 */
@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
public class SearchController {

    private static final int PAGE_SIZE = 10;
    private static final String SESSION_COOKIE = "rk_session";

    private final SearchService searchService;

    @GetMapping
    public SearchResultResponse search(@RequestParam String q,
                                        @RequestParam(required = false) UUID category,
                                        @RequestParam(defaultValue = "0") int page,
                                        HttpServletRequest request) {
        return searchService.search(q, category, page, PAGE_SIZE, sessionId(request));
    }

    private String sessionId(HttpServletRequest request) {
        if (request.getCookies() != null) {
            for (var cookie : request.getCookies()) {
                if (SESSION_COOKIE.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}
