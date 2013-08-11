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
import com.google.common.collect.Lists;
import com.vividsolutions.jts.geom.Coordinate;
import java.util.LinkedList;
import java.util.List;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import org.onebusaway.gtfs.model.Stop;
import org.opentripplanner.api.ws.oba_rest_api.beans.TransitResponse;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.services.StreetVertexIndexService;

/**
 * Implements the {@code stops-for-location} OneBusAway API method.
 * 
 * @see http://developer.onebusaway.org/modules/onebusaway-application-modules/current/api/where/methods/stops-for-location.html
 */

@Path(OneBusAwayApiMethod.API_BASE_PATH + "stops-for-location" + OneBusAwayApiMethod.API_CONTENT_TYPE)
public class StopsForLocationMethod extends OneBusAwayApiMethod {
    
    @QueryParam("lat") private Float lat;
    @QueryParam("lon") private Float lon;
    @QueryParam("latSpan") private Float latSpan;
    @QueryParam("lonSpan") private Float lonSpan;
    @QueryParam("query") private String query;
    @QueryParam("radius") @DefaultValue("100") private int radius;
    
    @Override
    protected TransitResponse getResponse() {
        
        List<Stop> stops = new LinkedList<Stop>();
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
                stops.add(transitStop.getStop());
            }
        } else if(query != null && query.length() >= 4) {
            for (Vertex gv : graph.getVertices()) {
                if (gv instanceof org.opentripplanner.routing.vertextype.TransitStop) {
                    stops.add(((org.opentripplanner.routing.vertextype.TransitStop) gv).getStop());
                }
            }
        }
        
        if(query != null) {
            query = query.toLowerCase();
            Iterable<Stop> iterableStops = Iterables.filter(stops, new Predicate<Stop>() {
                @Override
                public boolean apply(Stop stop) {
                    boolean match = false;

                    if(stop.getCode() != null)
                        match |= stop.getCode().toLowerCase().contains(query);

                    return match;
                }
            });

            stops = Lists.newArrayList(iterableStops);
        }

        return responseBuilder.getResponseForStops(stops);
    }
}
