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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import org.onebusaway.gtfs.model.Agency;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.model.calendar.ServiceDate;
import org.opentripplanner.api.ws.oba_rest_api.beans.TransitMetadata;
import org.opentripplanner.api.ws.oba_rest_api.beans.TransitResponse;
import org.opentripplanner.api.ws.oba_rest_api.beans.TransitResponseBuilder;
import org.opentripplanner.api.ws.oba_rest_api.beans.TransitRouteSchedule;
import org.opentripplanner.api.ws.oba_rest_api.beans.TransitRouteScheduleForDirection;
import org.opentripplanner.api.ws.oba_rest_api.beans.TransitSchedule;
import org.opentripplanner.api.ws.oba_rest_api.beans.TransitScheduleGroup;
import org.opentripplanner.api.ws.oba_rest_api.beans.TransitScheduleStopTime;
import org.opentripplanner.api.ws.oba_rest_api.beans.TransitTrip;
import org.opentripplanner.common.model.T2;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.transit_index.RouteVariant;
import org.opentripplanner.util.MapUtils;

@Path(OneBusAwayApiMethod.API_BASE_PATH + "schedule-for-stop" + OneBusAwayApiMethod.API_CONTENT_TYPE)
public class ScheduleForStopMethod extends OneBusAwayApiMethod {

    @QueryParam("stopId") private String id;
    @QueryParam("date") private String date;
    
    @Override
    protected TransitResponse getResponse() {
        ServiceDate serviceDate = new ServiceDate();
        if(date != null) {
            try {
                serviceDate = ServiceDate.parseString(date);
            } catch (ParseException ex) {
                return TransitResponseBuilder.getFailResponse(TransitResponse.Status.INVALID_VALUE, "Failed to parse service date.");
            }
        }
        
        AgencyAndId stopId = parseAgencyAndId(id);
        Stop stop = transitIndexService.getAllStops().get(stopId);
        if(stop == null)
            return TransitResponseBuilder.getFailResponse(TransitResponse.Status.NOT_FOUND, "Unknown stopId.");
        
        long startTime = serviceDate.getAsDate(graph.getTimeZone()).getTime() / 1000;
        long endTime = serviceDate.next().getAsDate(graph.getTimeZone()).getTime() / 1000 - 1;
        
        RoutingRequest options = null;
        if(graph.transitFeedCovers(startTime) && graph.transitFeedCovers(endTime))
            options = makeTraverseOptions(startTime, routerId);
        
        
        Map<String, TransitRouteSchedule> scheduleByRoute = new HashMap<String, TransitRouteSchedule>();
        if(options != null) {
            List<T2<TransitScheduleStopTime, TransitTrip>> stopTimesWithTrips = getStopTimesForStop(startTime, endTime, stopId);

            Map<T2<String, String>, List<TransitScheduleStopTime>> stopTimesByRoute = new HashMap<T2<String, String>, List<TransitScheduleStopTime>>();
            for(T2<TransitScheduleStopTime, TransitTrip> stopTimeWithTrip : stopTimesWithTrips) {
                TransitScheduleStopTime stopTime = stopTimeWithTrip.getFirst();
                TransitTrip transitTrip = stopTimeWithTrip.getSecond();
                stopTime.setWheelchairAccessible(transitTrip.isWheelchairAccessible());
                stopTime.setHeadsign(transitTrip.getTripHeadsign());
                //builder.addToReferences(transitTrip);
                MapUtils.addToMapList(stopTimesByRoute, new T2<String, String>(transitTrip.getRouteId(), transitTrip.getDirectionId()), stopTime);
            }

            for(T2<String, String> key : stopTimesByRoute.keySet()) {
                String SrouteId = key.getFirst();
                String direction = key.getSecond();

                TransitRouteSchedule routeSchedule = scheduleByRoute.get(SrouteId);
                if(routeSchedule == null) {
                    routeSchedule = new TransitRouteSchedule();
                    routeSchedule.setRouteId(SrouteId);
                    scheduleByRoute.put(SrouteId, routeSchedule);

                    AgencyAndId routeId = parseAgencyAndId(SrouteId);
                    List<String> alertIds = getAlertsForRoute(routeId, options, startTime, endTime);
                    routeSchedule.setAlertIds(alertIds);
                }

                List<TransitScheduleStopTime> stopTimes = stopTimesByRoute.get(key);
                sortStopTimes(stopTimes);

                Map<String, TransitScheduleGroup> groups = new HashMap<String, TransitScheduleGroup>();
                SortedMap<Integer, Integer> groupCounts = new TreeMap<Integer, Integer>();
                for(TransitScheduleStopTime stopTime : stopTimes) {
                    AgencyAndId tripId = parseAgencyAndId(stopTime.getTripId());
                    RouteVariant variant = transitIndexService.getVariantForTrip(tripId);
                    Integer groupId = variant.getId();

                    if(!groups.containsKey(groupId.toString())) {
                        TransitScheduleGroup group = new TransitScheduleGroup();
                        group.setGroupId(groupId.toString());
                        group.setDescription(variant.getName());
                        group.setHeadsign(stopTime.getHeadsign());
                        groups.put(groupId.toString(), group);
                    }

                    if(groupCounts.containsKey(groupId)) {
                        groupCounts.put(groupId, groupCounts.get(groupId) + 1);
                    } else {
                        groupCounts.put(groupId, 1);
                    }

                    stopTime.setGroupIds(Collections.singletonList(groupId.toString()));
                }

                TransitRouteScheduleForDirection directionSchedule = new TransitRouteScheduleForDirection();
                directionSchedule.setDirectionId(direction);
                directionSchedule.setStopTimes(stopTimes);
                directionSchedule.setGroups(groups);
                routeSchedule.getDirections().add(directionSchedule);
            }
        }
        
        List<String> alertIds = null;
        if(options != null)
            alertIds = getAlertsForStop(stopId, options, startTime, endTime);
        
        TransitSchedule schedule = new TransitSchedule();
        schedule.setStopId(id);
        schedule.setAlertIds(alertIds);
        schedule.setServiceDate(serviceDate.getAsString());
        schedule.setSchedules(new ArrayList<TransitRouteSchedule>(scheduleByRoute.values()));
        
        if(options == null) {
            responseBuilder.addToReferences(stop);
            return TransitResponseBuilder.getFailResponse(TransitResponse.Status.NO_TRANSIT_TIMES, "Date is outside the dateset's validity.",
                    responseBuilder.entity(schedule));
        }
        
        return responseBuilder.getResponseForStopSchedule(stop, schedule);
    }
}
