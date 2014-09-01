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

package org.opentripplanner.api.ws.oba_rest_api.methods;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Stop;
import org.opentripplanner.api.ws.oba_rest_api.beans.TransitArrivalsAndDepartures;
import org.opentripplanner.api.ws.oba_rest_api.beans.TransitEntryWithReferences;
import org.opentripplanner.api.ws.oba_rest_api.beans.TransitResponse;
import org.opentripplanner.api.ws.oba_rest_api.beans.TransitResponseBuilder;
import org.opentripplanner.api.ws.oba_rest_api.beans.TransitScheduleStopTime;
import org.opentripplanner.api.ws.oba_rest_api.beans.TransitTrip;
import org.opentripplanner.common.model.T2;
import org.opentripplanner.routing.core.RoutingRequest;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 */
@Path(OneBusAwayApiMethod.API_BASE_PATH + "arrivals-and-departures-for-stop" + OneBusAwayApiMethod.API_CONTENT_TYPE)
public class ArrivalsAndDeparturesForStopOTPMethod extends OneBusAwayApiMethod<TransitEntryWithReferences<TransitArrivalsAndDepartures>> {
    
    @QueryParam("minutesBefore") @DefaultValue("2") private int minutesBefore;
    @QueryParam("minutesAfter") @DefaultValue("30") private int minutesAfter;
    @QueryParam("stopId") private List<String> stopIdStrings;
    @QueryParam("time") private Long time;
	@QueryParam("onlyDepartures") @DefaultValue("true") private boolean onlyDepartures;

    @Override
    protected TransitResponse<TransitEntryWithReferences<TransitArrivalsAndDepartures>> getResponse() {

        if(stopIdStrings == null || stopIdStrings.isEmpty())
            return TransitResponseBuilder.getFailResponse(TransitResponse.Status.NOT_FOUND, "Unknown stopId.");

        if(time == null)
            time = System.currentTimeMillis() / 1000;
        
        long startTime = time - minutesBefore * 60;
        long endTime   = time + minutesAfter  * 60;

        RoutingRequest options = makeTraverseOptions(startTime, routerId);

        Stop firstStop = null;
        List<T2<TransitScheduleStopTime, TransitTrip>> stopTimesWithTrips = new ArrayList<T2<TransitScheduleStopTime, TransitTrip>>();
        List<String> alertIds = new ArrayList<String>();
        List<String> nearbyStopIds = new ArrayList<String>();

        for(String stopIdString : stopIdStrings) {
            AgencyAndId stopId = parseAgencyAndId(stopIdString);
            Stop stop = transitIndexService.getAllStops().get(stopId);
            if(stop == null)
                return TransitResponseBuilder.getFailResponse(TransitResponse.Status.NOT_FOUND, "Unknown stopId.");

            if(firstStop == null) {
                firstStop = stop;
                if(!graph.transitFeedCovers(startTime) && graph.transitFeedCovers(endTime)) {
                    return TransitResponseBuilder.getFailResponse(TransitResponse.Status.NO_TRANSIT_TIMES, "Date is outside the dateset's validity.",
                            responseBuilder.entity(responseBuilder.getArrivalsAndDepartures(firstStop, null, null, null, null)));
                }
            }

            stopTimesWithTrips.addAll(getStopTimesForStop(startTime, endTime, stopId, onlyDepartures, stopIdStrings.size() > 1));
            alertIds.addAll(getAlertsForStop(stopId, options, startTime, endTime));
            nearbyStopIds.addAll(getNearbyStops(stop));
        }

        sortStopTimesWithTrips(stopTimesWithTrips);
        
        List<TransitScheduleStopTime> stopTimes = new LinkedList<TransitScheduleStopTime>();
        List<TransitTrip> trips = new LinkedList<TransitTrip>();
        for(T2<TransitScheduleStopTime, TransitTrip> stopTimeWithTrip : stopTimesWithTrips) {
            stopTimes.add(stopTimeWithTrip.getFirst());
            trips.add(stopTimeWithTrip.getSecond());
        }

        return responseBuilder.getResponseForStop(firstStop, stopTimes, alertIds, trips, nearbyStopIds);
    }
}
