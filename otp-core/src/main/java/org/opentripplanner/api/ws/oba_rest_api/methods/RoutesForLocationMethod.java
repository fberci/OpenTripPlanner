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

import org.onebusaway.gtfs.model.Route;
import org.opentripplanner.api.ws.oba_rest_api.beans.TransitListEntryWithReferences;
import org.opentripplanner.api.ws.oba_rest_api.beans.TransitResponse;
import org.opentripplanner.api.ws.oba_rest_api.beans.TransitRoute;

import javax.ws.rs.Path;
import java.util.List;

/**
 * Implements the <a href="http://developer.onebusaway.org/modules/onebusaway-application-modules/current/api/where/methods/routes-for-location.html">routes-for-location</a> OneBusAway API method.
 */

@Path(OneBusAwayApiMethod.API_BASE_PATH + "routes-for-location" + OneBusAwayApiMethod.API_CONTENT_TYPE)
public class RoutesForLocationMethod extends AbstractLocationSearchMethod<TransitListEntryWithReferences<TransitRoute>> {
    
    @Override
    protected TransitResponse<TransitListEntryWithReferences<TransitRoute>> getResponse() {
        List<Route> routes = queryRoutes();
	    return responseBuilder.getResponseForRoutes(routes);
    }
}
