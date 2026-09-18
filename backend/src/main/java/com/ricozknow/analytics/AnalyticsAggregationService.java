package com.ricozknow.analytics;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;

/**
 * Computes one day's worth of rollups from raw analytics_events (spec section
 * 16: "Raw events are retained so aggregates can be recalculated"). Each run
 * for a given tenant+date deletes and re-inserts that date's rows, so calling
 * this twice for the same date is safe and produces identical results —
 * that's the "recalculated" guarantee, exercised for real rather than just
 * asserted.
 *
 * Two heuristics worth calling out explicitly, since they're judgment calls
 * the spec leaves open:
 *
 * - "Unsuccessful search" (spec section 17's gap-score formula): a search is
 *   successful if the same session clicked a result for that query
 *   (SEARCH_RESULT_CLICKED, wired up in Phase 8 — see PortalController's
 *   /search/click) OR viewed some article within the following 10 minutes.
 *   The click signal is the precise one; the view-proxy is kept as a fallback
 *   for visitors whose click never reaches the backend (JS disabled,
 *   middle-click-into-new-tab, ad blockers interfering with the beacon call)
 *   rather than dropped outright — belt-and-suspenders, not redundancy for
 *   its own sake. Zero-result searches are always unsuccessful regardless.
 *
 * - "Estimated deflection" (spec section 18): a session that viewed an
 *   article with no SUPPORT_CONTACT_STARTED event following it. No support
 *   widget/integration exists in this MVP to ever emit that event, so today
 *   this number is mechanically equal to qualifying article views — it's
 *   real infrastructure waiting on a support-side integration (spec's "Should
 *   Have: Deflection integrations"), not a fake metric.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsAggregationService {

    private static final long UNSUCCESSFUL_SEARCH_WINDOW_MINUTES = 10;
    private static final long DEFLECTION_WINDOW_HOURS = 24;

    private final AnalyticsEventRepository eventRepository;
    private final DailyArticleStatRepository articleStatRepository;
    private final DailySearchStatRepository searchStatRepository;
    private final DailyDeflectionStatRepository deflectionStatRepository;

    @Transactional
    public void aggregateForDate(UUID tenantId, LocalDate date) {
        Instant dayStart = date.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant dayEnd = date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        // Deflection/unsuccessful-search detection needs to look slightly past
        // the day's end to know whether a search/view was followed by an
        // engagement/contact within its window.
        Instant lookahead = date.plusDays(2).atStartOfDay(ZoneOffset.UTC).toInstant();

        aggregateArticleStats(tenantId, date, dayStart, dayEnd);
        aggregateSearchStats(tenantId, date, dayStart, dayEnd, lookahead);
        aggregateDeflectionStats(tenantId, date, dayStart, dayEnd, lookahead);
    }

    private void aggregateArticleStats(UUID tenantId, LocalDate date, Instant dayStart, Instant dayEnd) {
        Map<UUID, int[]> perArticle = new HashMap<>(); // [views, helpful, unhelpful]

        tally(eventRepository.findByTenantIdAndEventTypeAndOccurredAtBetween(
                tenantId, AnalyticsEventType.ARTICLE_VIEWED, dayStart, dayEnd), perArticle, 0);
        tally(eventRepository.findByTenantIdAndEventTypeAndOccurredAtBetween(
                tenantId, AnalyticsEventType.ARTICLE_HELPFUL, dayStart, dayEnd), perArticle, 1);
        tally(eventRepository.findByTenantIdAndEventTypeAndOccurredAtBetween(
                tenantId, AnalyticsEventType.ARTICLE_UNHELPFUL, dayStart, dayEnd), perArticle, 2);

        articleStatRepository.deleteByTenantIdAndStatDate(tenantId, date);
        perArticle.forEach((articleId, counts) -> {
            DailyArticleStat stat = new DailyArticleStat();
            stat.setTenantId(tenantId);
            stat.setArticleId(articleId);
            stat.setStatDate(date);
            stat.setViews(counts[0]);
            stat.setHelpfulCount(counts[1]);
            stat.setUnhelpfulCount(counts[2]);
            articleStatRepository.save(stat);
        });
    }

    private void tally(List<AnalyticsEvent> events, Map<UUID, int[]> perArticle, int slot) {
        for (AnalyticsEvent event : events) {
            if (event.getArticleId() == null) continue;
            int[] counts = perArticle.computeIfAbsent(event.getArticleId(), k -> new int[3]);
            counts[slot]++;
        }
    }

    private void aggregateSearchStats(UUID tenantId, LocalDate date, Instant dayStart, Instant dayEnd, Instant lookahead) {
        List<AnalyticsEvent> searches = eventRepository.findByTenantIdAndEventTypeAndOccurredAtBetween(
                tenantId, AnalyticsEventType.SEARCH_PERFORMED, dayStart, dayEnd);
        List<AnalyticsEvent> views = eventRepository.findByTenantIdAndEventTypeAndOccurredAtBetween(
                tenantId, AnalyticsEventType.ARTICLE_VIEWED, dayStart, lookahead);
        List<AnalyticsEvent> clicks = eventRepository.findByTenantIdAndEventTypeAndOccurredAtBetween(
                tenantId, AnalyticsEventType.SEARCH_RESULT_CLICKED, dayStart, lookahead);

        Map<String, List<Instant>> viewsBySession = groupBySession(views);
        Set<String> clickedSessionQueryPairs = clickedSessionQueryPairs(clicks);
        Map<String, int[]> perQuery = new HashMap<>(); // [volume, zeroResult, unsuccessful]

        for (AnalyticsEvent search : searches) {
            SearchEventMetadata meta = SearchEventMetadata.parse(search.getMetadata());
            String query = meta.query() != null ? meta.query().toLowerCase().trim() : "(unknown)";
            int[] counts = perQuery.computeIfAbsent(query, k -> new int[3]);
            counts[0]++; // volume
            if (meta.zeroResult()) {
                counts[1]++; // zero-result
                counts[2]++; // zero-result implies unsuccessful
            } else if (!wasClicked(search, query, clickedSessionQueryPairs)
                    && !hasFollowingView(search, viewsBySession)) {
                counts[2]++; // neither a click nor a view followed -> unsuccessful
            }
        }

        searchStatRepository.deleteByTenantIdAndStatDate(tenantId, date);
        perQuery.forEach((query, counts) -> {
            DailySearchStat stat = new DailySearchStat();
            stat.setTenantId(tenantId);
            stat.setQueryText(query);
            stat.setStatDate(date);
            stat.setVolumeCount(counts[0]);
            stat.setZeroResultCount(counts[1]);
            stat.setUnsuccessfulCount(counts[2]);
            searchStatRepository.save(stat);
        });
    }

    /** session|normalized-query pairs that had at least one click, for O(1) lookup per search event. */
    private Set<String> clickedSessionQueryPairs(List<AnalyticsEvent> clicks) {
        Set<String> pairs = new HashSet<>();
        for (AnalyticsEvent click : clicks) {
            if (click.getAnonymousSessionId() == null) continue;
            SearchEventMetadata meta = SearchEventMetadata.parse(click.getMetadata());
            String query = meta.query() != null ? meta.query().toLowerCase().trim() : "(unknown)";
            pairs.add(click.getAnonymousSessionId() + "|" + query);
        }
        return pairs;
    }

    private boolean wasClicked(AnalyticsEvent search, String query, Set<String> clickedSessionQueryPairs) {
        if (search.getAnonymousSessionId() == null) {
            return false;
        }
        return clickedSessionQueryPairs.contains(search.getAnonymousSessionId() + "|" + query);
    }

    private boolean hasFollowingView(AnalyticsEvent search, Map<String, List<Instant>> viewsBySession) {
        if (search.getAnonymousSessionId() == null) {
            return false; // can't correlate; treat conservatively as unsuccessful
        }
        List<Instant> sessionViews = viewsBySession.get(search.getAnonymousSessionId());
        if (sessionViews == null) {
            return false;
        }
        Instant windowEnd = search.getOccurredAt().plusSeconds(UNSUCCESSFUL_SEARCH_WINDOW_MINUTES * 60);
        return sessionViews.stream().anyMatch(viewedAt ->
                !viewedAt.isBefore(search.getOccurredAt()) && !viewedAt.isAfter(windowEnd));
    }

    private void aggregateDeflectionStats(UUID tenantId, LocalDate date, Instant dayStart, Instant dayEnd, Instant lookahead) {
        List<AnalyticsEvent> views = eventRepository.findByTenantIdAndEventTypeAndOccurredAtBetween(
                tenantId, AnalyticsEventType.ARTICLE_VIEWED, dayStart, dayEnd);
        List<AnalyticsEvent> contacts = eventRepository.findByTenantIdAndEventTypeAndOccurredAtBetween(
                tenantId, AnalyticsEventType.SUPPORT_CONTACT_STARTED, dayStart, lookahead);
        List<AnalyticsEvent> confirmed = eventRepository.findByTenantIdAndEventTypeAndOccurredAtBetween(
                tenantId, AnalyticsEventType.DEFLECTION_RECORDED, dayStart, dayEnd);

        Map<String, List<Instant>> contactsBySession = groupBySession(contacts);

        int estimated = 0;
        for (AnalyticsEvent view : views) {
            if (view.getAnonymousSessionId() == null) continue;
            List<Instant> sessionContacts = contactsBySession.get(view.getAnonymousSessionId());
            Instant windowEnd = view.getOccurredAt().plusSeconds(DEFLECTION_WINDOW_HOURS * 3600);
            boolean contactedAfterward = sessionContacts != null && sessionContacts.stream()
                    .anyMatch(contactedAt -> !contactedAt.isBefore(view.getOccurredAt()) && !contactedAt.isAfter(windowEnd));
            if (!contactedAfterward) {
                estimated++;
            }
        }

        deflectionStatRepository.deleteByTenantIdAndStatDate(tenantId, date);
        DailyDeflectionStat stat = new DailyDeflectionStat();
        stat.setTenantId(tenantId);
        stat.setStatDate(date);
        stat.setEstimatedDeflectionCount(estimated);
        stat.setConfirmedDeflectionCount(confirmed.size());
        stat.setSupportContactCount((int) contacts.stream()
                .filter(c -> !c.getOccurredAt().isBefore(dayStart) && c.getOccurredAt().isBefore(dayEnd))
                .count());
        deflectionStatRepository.save(stat);
    }

    private Map<String, List<Instant>> groupBySession(List<AnalyticsEvent> events) {
        Map<String, List<Instant>> bySession = new HashMap<>();
        for (AnalyticsEvent event : events) {
            if (event.getAnonymousSessionId() == null) continue;
            bySession.computeIfAbsent(event.getAnonymousSessionId(), k -> new ArrayList<>()).add(event.getOccurredAt());
        }
        return bySession;
    }
}