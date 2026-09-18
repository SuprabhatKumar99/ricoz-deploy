package com.ricozknow.agent;

import com.ricozknow.agent.dto.AgentArticleResponse;
import com.ricozknow.article.Visibility;
import com.ricozknow.common.RateLimiter;
import com.ricozknow.common.TooManyRequestsException;
import com.ricozknow.search.SearchService;
import com.ricozknow.search.dto.SearchResultResponse;
import com.ricozknow.user.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.EnumSet;
import java.util.UUID;

/**
 * Stable integration boundary for external support systems (spec section 19).
 * Authorization is enforced in SecurityConfig: only ADMIN or AGENT_VIEWER
 * principals may call these endpoints — see UserService for how a CRM/support
 * tool obtains an AGENT_VIEWER credential (a service-account user created by
 * a tenant ADMIN, authenticated the normal way via /api/v1/auth/login).
 * OpenSearch is never exposed directly to these callers, same as the portal.
 */
@RestController
@RequestMapping("/api/v1/agent")
@RequiredArgsConstructor
@Tag(name = "Agent API", description = "Read-only knowledge access for external support/CRM integrations")
@SecurityRequirement(name = "bearerAuth")
public class AgentController {

    private static final int PAGE_SIZE = 10;

    private final SearchService searchService;
    private final AgentArticleService agentArticleService;
    private final RateLimiter rateLimiter;

    @Value("${ricozknow.security.rate-limits.search.max-requests}")
    private int searchMaxRequests;
    @Value("${ricozknow.security.rate-limits.search.window-seconds}")
    private long searchWindowSeconds;

    @Operation(summary = "Search published knowledge visible to agents (PUBLIC + AUTHENTICATED articles)")
    @GetMapping("/search")
    public SearchResultResponse search(@RequestParam String q,
                                        @RequestParam(required = false) UUID category,
                                        @RequestParam(defaultValue = "0") int page) {
        // Keyed by user id, not IP: a service account behind a shared CRM
        // integration is one caller for rate-limiting purposes regardless of
        // which of the CRM's own servers happens to make the call.
        String key = "agent-search:" + currentUserId();
        if (!rateLimiter.allow(key, searchMaxRequests, Duration.ofSeconds(searchWindowSeconds))) {
            throw new TooManyRequestsException("Too many searches. Please slow down.");
        }
        return searchService.search(q, category, page, PAGE_SIZE, null,
                EnumSet.of(Visibility.PUBLIC, Visibility.AUTHENTICATED));
    }

    @Operation(summary = "Fetch a single published article by id")
    @GetMapping("/articles/{id}")
    public AgentArticleResponse getArticle(@PathVariable UUID id) {
        return agentArticleService.getById(id);
    }

    private UUID currentUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AuthenticatedUser principal) {
            return principal.userId();
        }
        throw new IllegalStateException("No authenticated user in context");
    }
}