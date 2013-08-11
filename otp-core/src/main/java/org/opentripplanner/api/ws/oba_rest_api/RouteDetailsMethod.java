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
import java.util.List;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import org.onebusaway.gtfs.model.Agency;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Route;
import org.onebusaway.gtfs.model.calendar.ServiceDate;
import org.opentripplanner.api.ws.oba_rest_api.beans.TransitMetadata;
import org.opentripplanner.api.ws.oba_rest_api.beans.TransitResponse;
import org.opentripplanner.api.ws.oba_rest_api.beans.TransitResponseBuilder;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.transit_index.RouteVariant;

@Path(OneBusAwayApiMethod.API_BASE_PATH + "route-details" + OneBusAwayApiMethod.API_CONTENT_TYPE)
public class RouteDetailsMethod extends OneBusAwayApiMethod {

    @QueryParam("routeId") private String id;
    @QueryParam("date") private String date;
    
    @Override
    protected TransitResponse getResponse() {
        AgencyAndId routeId = parseAgencyAndId(id);
        Route route = transitIndexService.getAllRoutes().get(routeId);
        if(route == null) {
            return TransitResponseBuilder.getFailResponse(TransitResponse.Status.NOT_FOUND, "Unknown routeId.");
        }
        
        ServiceDate serviceDate = new ServiceDate();
        if(date != null) {
            try {
                serviceDate = ServiceDate.parseString(date);
            } catch (ParseException ex) {
                return TransitResponseBuilder.getFailResponse(TransitResponse.Status.INVALID_VALUE, "Failed to parse service date.");
            }
        }
        
        long startTime = serviceDate.getAsDate(graph.getTimeZone()).getTime() / 1000;
        long endTime = serviceDate.next().getAsDate(graph.getTimeZone()).getTime() / 1000 - 1;
        
        if(!graph.transitFeedCovers(startTime) && graph.transitFeedCovers(endTime)) {
            return TransitResponseBuilder.getFailResponse(TransitResponse.Status.NO_TRANSIT_TIMES, "Date is outside the dateset's validity.");
        }
        
        RoutingRequest options = makeTraverseOptions(startTime, routerId);
        
        List<String> alertIds = getAlertsForRoute(routeId, options, startTime, endTime);
        List<RouteVariant> routeVariants = getReferenceVariantsForRoute(routeId);
        
        return responseBuilder.getResponseForRoute(route, routeVariants, alertIds);
    }
}
