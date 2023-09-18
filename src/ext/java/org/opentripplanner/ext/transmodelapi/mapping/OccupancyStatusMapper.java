package org.opentripplanner.ext.transmodelapi.mapping;

import org.opentripplanner.transit.model.timetable.OccupancyStatus;

/**
 * Transmodel API supports a subset of {@link OccupancyStatus} and this mapper can be used to map
 * any value to a value supported by the transmodel API.
 */
public class OccupancyStatusMapper {

  /**
   * @return {@link OccupancyStatus} supported by the Transmodel API that is the closes match to the
   * original.
   */
  public static OccupancyStatus mapStatus(OccupancyStatus occupancyStatus) {
    return switch (occupancyStatus) {
      case NO_DATA_AVAILABLE -> OccupancyStatus.NO_DATA_AVAILABLE;
      case MANY_SEATS_AVAILABLE, EMPTY -> OccupancyStatus.MANY_SEATS_AVAILABLE;
      case FEW_SEATS_AVAILABLE -> OccupancyStatus.FEW_SEATS_AVAILABLE;
      case STANDING_ROOM_ONLY, CRUSHED_STANDING_ROOM_ONLY -> OccupancyStatus.STANDING_ROOM_ONLY;
      case FULL -> OccupancyStatus.FULL;
      case NOT_ACCEPTING_PASSENGERS, NOT_BOARDABLE -> OccupancyStatus.NOT_ACCEPTING_PASSENGERS;
    };
  }
}
