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
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import org.onebusaway.gtfs.model.Agency;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Route;
import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.model.calendar.ServiceDate;
import org.opentripplanner.api.ws.oba_rest_api.beans.TransitMetadata;
import org.opentripplanner.api.ws.oba_rest_api.beans.TransitResponse;
import org.opentripplanner.api.ws.oba_rest_api.beans.TransitResponseBuilder;
import org.opentripplanner.api.ws.oba_rest_api.beans.TransitRoute;
import org.opentripplanner.routing.transit_index.RouteVariant;

@Path(OneBusAwayApiMethod.API_BASE_PATH + "route-details-for-stop" + OneBusAwayApiMethod.API_CONTENT_TYPE)
public class RouteDetailsForStopMethod extends OneBusAwayApiMethod {

    @QueryParam("stopId") private String id;
    
    @Override
    protected TransitResponse getResponse() {
        AgencyAndId stopId = parseAgencyAndId(id);
        Stop stop =  transitIndexService.getAllStops().get(stopId);
        if(stop == null) {
            return TransitResponseBuilder.getFailResponse(TransitResponse.Status.NOT_FOUND, "Unknown stopId.");
        }

        List<TransitRoute> routes = new LinkedList<TransitRoute>();
        for(AgencyAndId routeId : transitIndexService.getRoutesForStop(stopId)) {
            Route route = transitIndexService.getAllRoutes().get(routeId);
            List<RouteVariant> routeVariants = getReferenceVariantsForRoute(routeId);
            TransitRoute transitRoute = responseBuilder.getRoute(route, routeVariants, Collections.<String> emptyList()); // TODO: alerts?
            routes.add(transitRoute);
        }
        
        responseBuilder.addToReferences(stop);
        return responseBuilder.getResponseForRoutes(routes);
    }
}
