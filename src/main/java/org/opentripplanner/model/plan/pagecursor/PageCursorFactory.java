package org.opentripplanner.model.plan.pagecursor;

import static org.opentripplanner.model.plan.pagecursor.PageType.NEXT_PAGE;
import static org.opentripplanner.model.plan.pagecursor.PageType.PREVIOUS_PAGE;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import javax.annotation.Nullable;
import org.opentripplanner.model.base.ToStringBuilder;
import org.opentripplanner.model.plan.SortOrder;

public class PageCursorFactory {
    private final SortOrder sortOrder;
    private final Duration newSearchWindow;
    private PageType currentPageType;
    private SearchTime current = null;
    private Duration currentSearchWindow = null;
    private boolean holeSwUsed = true;
    private Instant removedItineraryStartTime = null;
    private Instant removedItineraryEndTime = null;

    private PageCursor nextCursor = null;
    private PageCursor prevCursor = null;

    public PageCursorFactory(SortOrder sortOrder, Duration newSearchWindow) {
        this.sortOrder = sortOrder;
        this.newSearchWindow = newSearchWindow;
    }

    /**
     * Set the original search earliest-departure-time({@code edt}), latest-arrival-time
     * ({@code lat}, optional) and the search-window used.
     */
    public PageCursorFactory withOriginalSearch(
            @Nullable PageType pageType,
            Instant edt,
            Instant lat,
            Duration searchWindow
    ) {
        // For the original/first search we set the page type equals to NEXT, since
        // the NEXT and first search is equivalent when creating new cursors.
        this.currentPageType = pageType == null ? pageTypeFromSortOrder(sortOrder) : pageType;

        this.current = new SearchTime(edt, lat);
        this.currentSearchWindow = searchWindow;
        return this;
    }

    public static PageType pageTypeFromSortOrder(SortOrder sortOrder) {
        return sortOrder.isSortedByArrivalTimeAcceding() ? NEXT_PAGE : PREVIOUS_PAGE;
    }

    /**
     * Set the start and end time for removed itineraries. The current implementation uses the
     * FIRST removed itinerary, but this can in some cases lead to missed itineraries in the
     * next search. So, we will document here what should be done.
     * <p>
     * For case {@code depart-after-crop-sw} and {@code arrive-by-crop-sw-reversed-filter} the
     * {@code startTime} should be the EARLIEST departure time for all removed itineraries.
     * <p>
     * For case {@code depart-after-crop-sw-reversed-filter} and {@code arrive-by-crop-sw} the
     * {@code startTime} should be the LATEST departure time for all removed itineraries.
     * <p>
     * The {@code endTime} should be replaced by removing duplicates between the to pages.
     * This can for example be done by including a hash for each potential itinerary in the
     * token, and make a filter to remove those in the following page response.
     *
     * @param startTime is rounded down to the closest minute.
     * @param endTime is round up to the closest minute.
     */
    public PageCursorFactory withRemovedItineraries(
            Instant startTime,
            Instant endTime
    ) {
        this.holeSwUsed = false;
        this.removedItineraryStartTime = startTime.truncatedTo(ChronoUnit.MINUTES);
        this.removedItineraryEndTime = endTime.plusSeconds(59).truncatedTo(ChronoUnit.MINUTES);
        return this;
    }

    @Nullable
    public PageCursor previousPageCursor() {
        createPageCursors();
        return prevCursor;
    }

    @Nullable
    public  PageCursor nextPageCursor() {
        createPageCursors();
        return nextCursor;
    }

    /** Create page cursor pair (next and previous) */
    private void createPageCursors() {
        if(current == null || nextCursor != null || prevCursor != null) { return; }

        SearchTime prev = new SearchTime(null, null);
        SearchTime next = new SearchTime(null, null);

        // Depart after, sort on arrival time with the earliest first
        if (sortOrder.isSortedByArrivalTimeAcceding()) {
            if (currentPageType == NEXT_PAGE) {
                prev.edt = current.edt.minus(newSearchWindow);
                next.edt = holeSwUsed
                        ? current.edt.plus(currentSearchWindow)
                        : removedItineraryStartTime;
            }
            // current page type == PREV_PAGE
            else {
                if (holeSwUsed) {
                    prev.edt = current.edt.minus(currentSearchWindow);
                }
                else {
                    //TODO: The start time for the removed itinerary is not the best thing to use
                    //      here. We should take the LATEST start time of all removed itineraries
                    //      instead.
                    prev.edt = calcStartOfSearchWindow(removedItineraryStartTime);
                    prev.lat = removedItineraryEndTime;
                }
                next.edt = current.edt.plus(currentSearchWindow);
            }
        }
        // Arrive-by, sort on departure time with the latest first
        else {
            if (currentPageType == PREVIOUS_PAGE) {
                if(holeSwUsed) {
                    prev.edt = current.edt.minus(newSearchWindow);
                    prev.lat = current.lat;
                }
                else {
                    prev.edt = calcStartOfSearchWindow(removedItineraryStartTime);
                    // TODO: Replace this by hashing removed itineraries
                    prev.lat = removedItineraryEndTime;
                }
                next.edt = current.edt.plus(currentSearchWindow);
            }
            // Use normal sort and removal in ItineraryFilterChain
            else {
                prev.edt = current.edt.minus(newSearchWindow);
                prev.lat = current.lat;

                if (holeSwUsed) {
                    next.edt = current.edt.plus(currentSearchWindow);
                }
                else {
                    next.edt = removedItineraryStartTime;
                }
            }
        }
        prevCursor = new PageCursor(PREVIOUS_PAGE, sortOrder, prev.edt, prev.lat, newSearchWindow);
        nextCursor = new PageCursor(NEXT_PAGE, sortOrder, next.edt, next.lat, newSearchWindow);
    }

    @Override
    public String toString() {
        return ToStringBuilder.of(PageCursorFactory.class)
                .addEnum("sortOrder", sortOrder)
                .addEnum("currentPageType", currentPageType)
                .addObj("current", current)
                .addDuration("currentSearchWindow", currentSearchWindow)
                .addDuration("newSearchWindow", newSearchWindow)
                .addBoolIfTrue("searchWindowCropped", !holeSwUsed)
                .addTime("removedItineraryStartTime", removedItineraryStartTime)
                .addTime("removedItineraryEndTime", removedItineraryEndTime)
                .addObj("nextCursor", nextCursor)
                .addObj("prevCursor", prevCursor)
                .toString();
    }

    /**
     * The search-window start and end is [inclusive, exclusive], so to calculate the start of the
     * search-window from the last time included in the search window we need to include one extra
     * minute at the end.
     */
    private Instant calcStartOfSearchWindow(Instant lastMinuteInSearchWindow) {
        return lastMinuteInSearchWindow.minus(currentSearchWindow).plusSeconds(60);
    }

    /** Temporary data class used to hold a pair of edt and lat */
    private static class SearchTime {
        Instant edt;
        Instant lat;

        private SearchTime(Instant edt, Instant lat) {
            this.edt = edt;
            this.lat = lat;
        }

        @Override
        public String toString() {
            return ToStringBuilder.of(SearchTime.class)
                .addTime("edt", edt)
                .addTime("lat", lat)
                .toString();
        }
    }
}
