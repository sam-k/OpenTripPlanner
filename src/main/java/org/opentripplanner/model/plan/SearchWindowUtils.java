package org.opentripplanner.model.plan;

import java.time.Duration;
import java.time.Instant;


public class SearchWindowUtils {
    /**
     * Extra time is added to the search-window for the next request if the current
     * result have few itineraries:
     * <pre>
     *  0: 6h
     *  1: 4h
     *  2: 2h
     *  3: 1h
     *  4: 30m
     *  5: 20m
     *  6: 10m
     * </pre>
     * Note!
     *
     * Unit: minutes
     */
    private static final int[] EXTRA_TIME_ITINERARIES = new int[]{
            //0,   1,   2,  3,  4,  5,  6
            360, 240, 120, 60, 30, 20, 10
    };

    /**
     * We look at the search data after the trip search to see if we should adjust the search-
     * window. This is done to avoid short-search-windows in low frequency areas, where the client
     * would need to request the
     */
    public static Duration calculateNewSearchWindow(
            Duration usedSearchWindow,
            Instant earliestDepartureTime,
            Instant latestDepartureTime,
            int nItinerariesInSearchWindow
    ) {
        int sw = (int) usedSearchWindow.toMinutes();

        // The search result was reduced using the max itineraries limit
        if (latestDepartureTime != null) {
            long actualSW = Duration.between(earliestDepartureTime, latestDepartureTime).toSeconds();
            long diffSW = usedSearchWindow.toSeconds() - actualSW;

            // Remove 7/8 (87.5%) of the unused part of the search-window. We leave a little slack
            // to be sure we get enough results to fill up the num-of-itineraries in the next
            // result as well.
            int newSearchWindow = (int) (usedSearchWindow.toSeconds() - 7 * diffSW / 8);

            // Round down to minutes
            return normalizeSearchWindow(newSearchWindow / 60);
        }
        if (nItinerariesInSearchWindow < EXTRA_TIME_ITINERARIES.length) {
            return normalizeSearchWindow(
                    sw + EXTRA_TIME_ITINERARIES[nItinerariesInSearchWindow]
            );
        }
        // No change
        return usedSearchWindow;
    }


    /**
     * Round search-window({@code sw}) up:
     * <ul>
     *     <li>if {@code sw < 10m } then search-window is set to 10 minutes
     *     <li>if {@code sw <= 4h} then round search-window up to closest 10 minutes
     *     <li>if {@code sw > 4h} then round search-window up to closest 30 minutes
     *     <li>if {@code sw > 24h} then return 24 hours(max search window allowed is 24 hours)
     * </ul>
     */
    static Duration normalizeSearchWindow(int minutes) {
        if (minutes < 10) {
            return Duration.ofMinutes(10);
        }
        else if (minutes <= 240) {
            return Duration.ofMinutes(ceiling(minutes, 10));
        }
        else if (minutes <= 24 * 60) {
            return Duration.ofMinutes(ceiling(minutes, 30));
        }
        else {
            // Max search window is one day
            return Duration.ofDays(1);
        }
    }

    /**
     * Round value to the closest increment of given {@code step}. This is used to round of a time
     * or duration to the closest "step" of like 10 minutes.
     */
    static int ceiling(int value, int step) {
        if (value < 0) {
            return (value / step) * step;
        }
        else {
            return ((value + step - 1) / step) * step;
        }
    }
}
