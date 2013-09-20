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

import java.text.ParseException;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import org.onebusaway.gtfs.model.Agency;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Trip;
import org.onebusaway.gtfs.model.calendar.ServiceDate;
import org.onebusaway.gtfs.services.calendar.CalendarService;
import org.opentripplanner.api.ws.oba_rest_api.beans.TransitAlert;
import org.opentripplanner.api.ws.oba_rest_api.beans.TransitMetadata;
import org.opentripplanner.api.ws.oba_rest_api.beans.TransitResponse;
import org.opentripplanner.api.ws.oba_rest_api.beans.TransitResponseBuilder;
import org.opentripplanner.api.ws.oba_rest_api.beans.TransitStopTime;
import org.opentripplanner.api.ws.oba_rest_api.beans.TransitTrip;
import org.opentripplanner.api.ws.oba_rest_api.beans.TransitVehicle;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.ServiceDay;
import org.opentripplanner.routing.edgetype.TableTripPattern;
import org.opentripplanner.routing.edgetype.Timetable;
import org.opentripplanner.routing.services.TransitIndexService;
import org.opentripplanner.routing.transit_index.RouteVariant;
import org.opentripplanner.routing.trippattern.CanceledTripTimes;
import org.opentripplanner.updater.vehicle_location.VehicleLocation;
import org.opentripplanner.updater.vehicle_location.VehicleLocationService;

@Path(OneBusAwayApiMethod.API_BASE_PATH + "trip-details" + OneBusAwayApiMethod.API_CONTENT_TYPE)
public class TripDetailsOTPMethod extends OneBusAwayApiMethod {

    @QueryParam("vehicleId") private String vehicleIdString;
    @QueryParam("tripId") private String tripIdString;
    @QueryParam("date") private String date;
    
    @Override
    protected TransitResponse getResponse() {
        ServiceDate serviceDate;
        AgencyAndId tripId;
        
        if(vehicleIdString != null) {
            VehicleLocationService vehicleLocationService = graph.getService(VehicleLocationService.class);
            AgencyAndId vehicleId = parseAgencyAndId(vehicleIdString);

            if(vehicleLocationService == null)
                return TransitResponseBuilder.getFailResponse(TransitResponse.Status.ERROR_VEHICLE_LOCATION_SERVICE);

            VehicleLocation vehicle = vehicleLocationService.getForVehicle(vehicleId);
            if(vehicle == null)
                return TransitResponseBuilder.getFailResponse(TransitResponse.Status.NOT_FOUND, "A vehicle of the given id doesn't exist.");
            
            serviceDate = vehicle.getServiceDate();
            tripId = vehicle.getTripId();
        } else {
            serviceDate = new ServiceDate();
            if(date != null) {
                try {
                    serviceDate = ServiceDate.parseString(date);
                } catch (ParseException ex) {
                    return TransitResponseBuilder.getFailResponse(TransitResponse.Status.INVALID_VALUE, "Failed to parse service date.");
                }
            }

            tripId = parseAgencyAndId(tripIdString);
            if(transitIndexService.getTripPatternForTrip(tripId) == null)
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
        
        RoutingRequest options = makeTraverseOptions(startTime, routerId);
        
        RouteVariant variant = transitIndexService.getVariantForTrip(tripId);
        
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
        
        List<TransitStopTime> stopTimes = getStopTimesForTrip(routerId, tripId, serviceDate, pattern, timetable);
        List<TransitAlert> alerts = new LinkedList<TransitAlert>(); // TODO
        
        TransitVehicle transitVehicle = null;
        VehicleLocationService vehicleLocationService = graph.getService(VehicleLocationService.class);
        if(vehicleLocationService != null) {
            VehicleLocation vehicle = vehicleLocationService.getForTrip(tripId);
            if(vehicle != null)
                transitVehicle = responseBuilder.getVehicle(vehicle);
        }
        
        TransitTrip transitTrip = responseBuilder.getTrip(trip);
        transitTrip.setWheelchairAccessible(timetable.isWheelchairAccessible(tripIndex));
        
        AgencyAndId routeId = trip.getRoute().getId();
        List<String> alertIds = getAlertsForRoute(routeId, options, startTime, endTime);
                
        return responseBuilder.getResponseForTrip(transitTrip, serviceDate, alertIds, pattern.getStops(), stopTimes, transitVehicle, variant);
    }
}
