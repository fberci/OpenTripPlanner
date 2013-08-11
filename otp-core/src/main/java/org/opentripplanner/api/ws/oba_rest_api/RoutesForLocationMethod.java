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

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.vividsolutions.jts.geom.Coordinate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Route;
import org.opentripplanner.api.ws.oba_rest_api.beans.TransitResponse;
import org.opentripplanner.routing.services.StreetVertexIndexService;

/**
 * Implements the {@code routes-for-location} OneBusAway API method.
 * 
 * @see http://developer.onebusaway.org/modules/onebusaway-application-modules/current/api/where/methods/routes-for-location.html
 */

@Path(OneBusAwayApiMethod.API_BASE_PATH + "routes-for-location" + OneBusAwayApiMethod.API_CONTENT_TYPE)
public class RoutesForLocationMethod extends OneBusAwayApiMethod {
    
    @QueryParam("lat") private Float lat;
    @QueryParam("lon") private Float lon;
    @QueryParam("latSpan") private Float latSpan;
    @QueryParam("lonSpan") private Float lonSpan;
    @QueryParam("query") private String query;
    @QueryParam("radius") @DefaultValue("100") private int radius;
    
    @Override
    protected TransitResponse getResponse() {
        
        Set<Route> routes = new HashSet<Route>();
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
                for(AgencyAndId routeId : transitIndexService.getRoutesForStop(transitStop.getStopId()))
                    routes.add(transitIndexService.getAllRoutes().get(routeId));
            }
        } else if(query != null && query.length() > 0) {
            for (Route route : transitIndexService.getAllRoutes().values()) {
                routes.add(route);
            }
        }
        
        if(query != null) {
            query = query.toLowerCase();
            Iterable<Route> iterableRoutes = Iterables.filter(routes, new Predicate<Route>() {
                @Override
                public boolean apply(Route route) {
                    boolean match = false;

                    if(route.getShortName() != null)
                        match |= route.getShortName().toLowerCase().contains(query);

                    if(route.getLongName()!= null)
                        match |= route.getLongName().toLowerCase().contains(query);

                    return match;
                }
            });

            routes = Sets.newHashSet(iterableRoutes);
        }

        return responseBuilder.getResponseForRoutes(routes);
    }
}
