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

import com.google.common.collect.Lists;
import com.vividsolutions.jts.geom.Coordinate;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Route;
import org.opentripplanner.api.common.SearchHintService;
import org.opentripplanner.api.ws.oba_rest_api.beans.TransitListEntryWithReferences;
import org.opentripplanner.api.ws.oba_rest_api.beans.TransitResponse;
import org.opentripplanner.api.ws.oba_rest_api.beans.TransitRoute;
import org.opentripplanner.routing.services.StreetVertexIndexService;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import java.util.ArrayList;
import java.util.List;

/**
 * Implements the <a href="http://developer.onebusaway.org/modules/onebusaway-application-modules/current/api/where/methods/routes-for-location.html">routes-for-location</a> OneBusAway API method.
 */

@Path(OneBusAwayApiMethod.API_BASE_PATH + "routes-for-location" + OneBusAwayApiMethod.API_CONTENT_TYPE)
public class RoutesForLocationMethod extends AbstractSearchMethod<TransitListEntryWithReferences<TransitRoute>> {
    
    @QueryParam("lat") private Float lat;
    @QueryParam("lon") private Float lon;
    @QueryParam("latSpan") private Float latSpan;
    @QueryParam("lonSpan") private Float lonSpan;
    @QueryParam("query") private String query;
    @QueryParam("radius") @DefaultValue("100") private int radius;
    
    @Override
    protected TransitResponse<TransitListEntryWithReferences<TransitRoute>> getResponse() {
        if(query != null && latSpan == null && lonSpan == null) {
            lat = null;
            lon = null;
        }
        
        List<Route> routes = new ArrayList<Route>();
        if(lat != null && lon != null) {
            Coordinate center = new Coordinate(lon, lat);
            StreetVertexIndexService streetVertexIndexService = graph.streetIndex;
            List<org.opentripplanner.routing.vertextype.TransitStop> stopVertices;
            
            if (latSpan != null && lonSpan != null) {
                Coordinate c1 = new Coordinate(lon - lonSpan, lat - latSpan),
                           c2 = new Coordinate(lon + lonSpan, lat + latSpan);
                
                stopVertices = streetVertexIndexService.getNearbyTransitStops(c1, c2);
            } else {
                stopVertices = streetVertexIndexService.getNearbyTransitStops(center, radius);
            }

            for (org.opentripplanner.routing.vertextype.TransitStop transitStop : stopVertices) {
                for(AgencyAndId routeId : getRoutesForStop(transitStop.getStopId())) {
                    routes.add(transitIndexService.getAllRoutes().get(routeId));
                }
            }
        } else if(query != null && query.length() > 0) {
            routes.addAll(transitIndexService.getAllRoutes().values());
        }
        
        if(query != null) {
            SearchHintService searchHintService = graph.getService(SearchHintService.class);
            query = normalize(query);
            
            Iterable<Route> matchingRoutes = getMatchingRoutes(routes, query, searchHintService);
            routes = Lists.newArrayList(matchingRoutes);
        }

	    return responseBuilder.getResponseForRoutes(routes);
    }
}
