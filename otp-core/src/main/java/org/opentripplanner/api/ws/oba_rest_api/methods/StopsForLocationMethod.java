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
import org.onebusaway.gtfs.model.Stop;
import org.opentripplanner.api.ws.oba_rest_api.beans.TransitListEntryWithReferences;
import org.opentripplanner.api.ws.oba_rest_api.beans.TransitResponse;
import org.opentripplanner.api.ws.oba_rest_api.beans.TransitResponseBuilder;
import org.opentripplanner.api.ws.oba_rest_api.beans.TransitStop;
import org.opentripplanner.routing.services.PatchService;
import org.opentripplanner.routing.services.StreetVertexIndexService;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implements the <a href="http://developer.onebusaway.org/modules/onebusaway-application-modules/current/api/where/methods/stops-for-location.html">stops-for-location</a> OneBusAway API method.
 */

@Path(OneBusAwayApiMethod.API_BASE_PATH + "stops-for-location" + OneBusAwayApiMethod.API_CONTENT_TYPE)
public class StopsForLocationMethod extends AbstractSearchMethod<TransitListEntryWithReferences<TransitStop>> {

	/**
	 * The center <code>latitude</code> of the requested area.
	 */
    @QueryParam("lat") private Float lat;

	/**
	 * The center <code>longitude</code> of the requested area.
	 */
    @QueryParam("lon") private Float lon;

	/**
	 * The latitudinal span of the area. Optional. Override the {@code latSpan} parameter.
	 */
    @QueryParam("latSpan") private Float latSpan;
	/**
	 * The longitudinal span of the area. Optional.
	 */
    @QueryParam("lonSpan") private Float lonSpan;

	/**
	 * The radius (in meters) of the requested area. Optional.
	 */
	@QueryParam("radius") @DefaultValue("100") private int radius;

	/**
	 * Search query with which to filter the returned stops.
	 */
    @QueryParam("query") private String query;

    @Override
    protected TransitResponse<TransitListEntryWithReferences<TransitStop>> getResponse() {
        if(query != null && latSpan == null && lonSpan == null) {
            lat = null;
            lon = null;
        }
        
        List<Stop> stops = new ArrayList<Stop>();
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
        } else {
            stops.addAll(transitIndexService.getAllStops().values());
        }

        if(query != null && query.length() >= 3) {
            query = normalize(query);
            
            List<Stop> matchingRoutes = getMatchingStops(stops, query);
            stops = Lists.newArrayList(matchingRoutes);
        }

        List<Stop> filteredStops = new ArrayList<Stop>();
        for(Stop stop : stops) {
            if(TransitResponseBuilder.isStopPrivate(stop))
                continue;

            filteredStops.add(stop);
        }
        stops = filteredStops;

        if(dialect.getDialect() == TransitResponseBuilder.Dialect.OBA) {
            List<Stop> filteredForStopsOnly = new ArrayList<Stop>();
            for(Stop stop : stops) {
                if(stop.getLocationType() == 0) {
                    filteredForStopsOnly.add(stop);
                }
            }
            stops = filteredForStopsOnly;
        }

	    Map<String, List<String>> alertIds = new HashMap<String, List<String>>();
	    if(dialect.getDialect() == TransitResponseBuilder.Dialect.MOBILE) {
			for(Stop stop : stops) {
				List<String> alerts = getAlertsForStop(graph.getService(PatchService.class), stop.getId(), true, false);
				alertIds.put(stop.getId().toString(), alerts);
			}
	    }

        return responseBuilder.getResponseForStops(stops, alertIds);
    }
}
