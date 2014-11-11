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

import org.onebusaway.gtfs.model.Stop;
import org.opentripplanner.api.ws.oba_rest_api.beans.TransitListEntryWithReferences;
import org.opentripplanner.api.ws.oba_rest_api.beans.TransitResponse;
import org.opentripplanner.api.ws.oba_rest_api.beans.TransitResponseBuilder;
import org.opentripplanner.api.ws.oba_rest_api.beans.TransitStop;
import org.opentripplanner.routing.services.PatchService;

import javax.ws.rs.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implements the <a href="http://developer.onebusaway.org/modules/onebusaway-application-modules/current/api/where/methods/stops-for-location.html">stops-for-location</a> OneBusAway API method.
 */

@Path(OneBusAwayApiMethod.API_BASE_PATH + "stops-for-location" + OneBusAwayApiMethod.API_CONTENT_TYPE)
public class StopsForLocationMethod extends AbstractLocationSearchMethod<TransitListEntryWithReferences<TransitStop>> {

    @Override
    protected TransitResponse<TransitListEntryWithReferences<TransitStop>> getResponse() {
        List<Stop> stops = queryStops();

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
