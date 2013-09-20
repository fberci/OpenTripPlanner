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

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import org.opentripplanner.api.ws.oba_rest_api.beans.TransitResponse;
import org.opentripplanner.api.ws.oba_rest_api.beans.TransitResponseBuilder;
import org.opentripplanner.api.ws.oba_rest_api.beans.TransitVehicle;
import org.opentripplanner.updater.vehicle_location.VehicleLocation;
import org.opentripplanner.updater.vehicle_location.VehicleLocationService;

/**
 * Implements the {@code vehicles-for-agency} OneBusAway API method.
 * 
 * @see http://developer.onebusaway.org/modules/onebusaway-application-modules/current/api/where/methods/vehicles-for-agency.html
 */

@Path(OneBusAwayApiMethod.API_BASE_PATH + "vehicles-for-agency/{agencyId}" + OneBusAwayApiMethod.API_CONTENT_TYPE)
public class VehiclesForAgencyMethod extends OneBusAwayApiMethod {

    @PathParam("agencyId") private String agencyId;
    @QueryParam("ifModifiedSince") @DefaultValue("-1") long ifModifiedSince;
    
    @Override
    protected TransitResponse getResponse() {
        
        VehicleLocationService vehicleLocationService = graph.getService(VehicleLocationService.class);
        if(vehicleLocationService == null)
            return TransitResponseBuilder.getFailResponse(TransitResponse.Status.ERROR_VEHICLE_LOCATION_SERVICE);
        
        if(ifModifiedSince >= vehicleLocationService.getLastUpdateTime()) {
            return TransitResponseBuilder.getFailResponse(TransitResponse.Status.NOT_MODIFIED);
        }
                
        Collection<VehicleLocation> vehicles = vehicleLocationService.getForAgency(agencyId);
        List<TransitVehicle> transitVehicles = new LinkedList<TransitVehicle>();
        for(VehicleLocation vehicle : vehicles) {
            if(vehicle.getTripId() != null) {
                responseBuilder.addToReferences(getTrip(vehicle.getTripId(), vehicle.getServiceDate()));
            }
            transitVehicles.add(responseBuilder.getVehicle(vehicle));
        }
        
        return responseBuilder.getResponseForList(transitVehicles);
    }
}
