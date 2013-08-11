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

import java.util.LinkedList;
import java.util.List;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import org.onebusaway.gtfs.model.Agency;
import org.onebusaway.gtfs.model.Route;
import org.opentripplanner.api.ws.oba_rest_api.beans.TransitResponse;
import org.opentripplanner.api.ws.oba_rest_api.beans.TransitResponseBuilder;

/**
 * Implements the {@code routes-for-agency} OneBusAway API method.
 * 
 * @see http://developer.onebusaway.org/modules/onebusaway-application-modules/current/api/where/methods/routes-for-agency.html
 */

@Path(OneBusAwayApiMethod.API_BASE_PATH + "routes-for-agency/{agencyId}" + OneBusAwayApiMethod.API_CONTENT_TYPE)
public class RoutesForAgencyMethod extends OneBusAwayApiMethod {

    @PathParam("agencyId") private String agencyId;
    
    @Override
    protected TransitResponse getResponse() {
        Agency agency = transitIndexService.getAgency(agencyId);
        if(agency == null)
            return TransitResponseBuilder.getFailResponse(TransitResponse.Status.NOT_FOUND);
        
        List<Route> routesForAgency = new LinkedList<Route>();
        
        for(Route route : transitIndexService.getAllRoutes().values()) {
            if(route.getId().getAgencyId().equals(agencyId)) {
                routesForAgency.add(route);
            }
        }
        
        return responseBuilder.getResponseForRoutes(routesForAgency);
    }
}
