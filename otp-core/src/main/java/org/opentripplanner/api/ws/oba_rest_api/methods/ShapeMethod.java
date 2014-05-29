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
import org.opentripplanner.api.ws.oba_rest_api.beans.TransitEntryWithReferences;
import org.opentripplanner.api.ws.oba_rest_api.beans.TransitPolyline;
import org.opentripplanner.api.ws.oba_rest_api.beans.TransitResponse;
import org.opentripplanner.api.ws.oba_rest_api.beans.TransitResponseBuilder;
import org.opentripplanner.routing.transit_index.RouteVariant;

import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

/**
 * Implements the <a href="http://developer.onebusaway.org/modules/onebusaway-application-modules/current/api/where/methods/shape.html">shape</a> OneBusAway API method. Since OTP doesn't keep track of shapes, the method
 * expects a tripId instead of an shapeId.
 */

@Path(OneBusAwayApiMethod.API_BASE_PATH + "shape/{shapeId}" + OneBusAwayApiMethod.API_CONTENT_TYPE)
public class ShapeMethod extends OneBusAwayApiMethod<TransitEntryWithReferences<TransitPolyline>> {

    @PathParam("shapeId") private String tripIdString;
            
    @Override
    protected TransitResponse<TransitEntryWithReferences<TransitPolyline>> getResponse() {
        
        AgencyAndId tripId = parseAgencyAndId(tripIdString);
        RouteVariant variant = transitIndexService.getVariantForTrip(tripId);
        if(variant == null) {
            return TransitResponseBuilder.getFailResponse(TransitResponse.Status.NOT_FOUND, "A RouteVariant was not found for the tripId.");
        }
        
        return responseBuilder.getResponseForLineString(variant.getGeometry());
    }
}