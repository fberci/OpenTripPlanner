/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.api.ws.oba_rest_api;

import java.util.Calendar;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Trip;
import org.onebusaway.gtfs.model.calendar.ServiceDate;
import org.onebusaway.gtfs.services.calendar.CalendarService;
import org.opentripplanner.api.ws.oba_rest_api.beans.TransitResponse;
import org.opentripplanner.api.ws.oba_rest_api.beans.TransitResponseBuilder;
import org.opentripplanner.api.ws.oba_rest_api.beans.TransitTripDetails;
import org.opentripplanner.routing.core.ServiceDay;
import org.opentripplanner.routing.edgetype.TableTripPattern;
import org.opentripplanner.routing.edgetype.Timetable;
import org.opentripplanner.routing.trippattern.CanceledTripTimes;

@Path(OneBusAwayApiMethod.API_BASE_PATH + "trip-details/{tripId}" + OneBusAwayApiMethod.API_CONTENT_TYPE)
public class TripDetailsMethod extends OneBusAwayApiMethod {

    @PathParam("tripId") private String tripIdString;
    @QueryParam("serviceDate") private Long serviceDateMS;
    @QueryParam("includeTrip") @DefaultValue("true") private boolean includeTrip; // TODO
    @QueryParam("includeSchedule") @DefaultValue("true") private boolean includeSchedule;
    @QueryParam("includeStatus") @DefaultValue("true") private boolean includeStatus;
    
    @Override
    protected TransitResponse getResponse() {
        
        ServiceDate serviceDate;
        AgencyAndId tripId;
        
        serviceDate = new ServiceDate();
        if(serviceDateMS != null) {
            Calendar calendar = Calendar.getInstance(graph.getTimeZone());
            calendar.setTimeInMillis(serviceDateMS);
            serviceDate = new ServiceDate(calendar);
        }

        tripId = parseAgencyAndId(tripIdString);
        if(transitIndexService.getTripPatternForTrip(tripId) == null) {
            return TransitResponseBuilder.getFailResponse(TransitResponse.Status.NOT_FOUND, "Unknown tripId.");
        }
        
        Trip trip = getTrip(tripId, serviceDate);
        
        CalendarService calendarService = graph.getCalendarService();
        ServiceDay serviceDay = new ServiceDay(graph, serviceDate.getAsDate(graph.getTimeZone()).getTime() / 1000, calendarService, trip.getId().getAgencyId());
        
        long startTime = serviceDate.getAsDate(graph.getTimeZone()).getTime() / 1000;
        long endTime = serviceDate.next().getAsDate(graph.getTimeZone()).getTime() / 1000 - 1;
        
        if(!graph.transitFeedCovers(startTime) && graph.transitFeedCovers(endTime)) {
            return TransitResponseBuilder.getFailResponse(TransitResponse.Status.NO_TRANSIT_TIMES, "Date is outside the dateset's validity.");
        }
        
        TableTripPattern pattern = transitIndexService.getTripPatternForTrip(tripId);
        if(!serviceDay.serviceIdRunning(pattern.getServiceId()))
            return TransitResponseBuilder.getFailResponse(TransitResponse.Status.NOT_OPERATING, "Trip isn't operation on the given service date.");
        
        Timetable timetable;
        if(graph.getTimetableSnapshotSource() != null && graph.getTimetableSnapshotSource().getTimetableSnapshot() != null) {
            timetable = graph.getTimetableSnapshotSource().getTimetableSnapshot().resolve(pattern, serviceDate);
        } else {
            timetable = pattern.getScheduledTimetable();
        }
        
        int tripIndex = timetable.getTripIndex(tripId);
        if(timetable.getTripTimes(tripIndex) instanceof CanceledTripTimes)
            return TransitResponseBuilder.getFailResponse(TransitResponse.Status.NOT_OPERATING, "Trip is canceled on the given service date.");
        
        TransitTripDetails tripDetails = getTripDetails(trip, serviceDate, pattern, timetable);
        
        if(!includeSchedule)
            tripDetails.setSchedule(null);
        if(!includeStatus)
            tripDetails.setStatus(null);
        
        return responseBuilder.getResponseForTrip(tripDetails);
    }
}
