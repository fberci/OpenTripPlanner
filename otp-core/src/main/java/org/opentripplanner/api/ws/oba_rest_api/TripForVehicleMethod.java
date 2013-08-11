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

import javax.ws.rs.DefaultValue;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.opentripplanner.api.ws.oba_rest_api.beans.TransitResponse;
import org.opentripplanner.api.ws.oba_rest_api.beans.TransitResponseBuilder;
import org.opentripplanner.api.ws.oba_rest_api.beans.TransitTripDetails;
import org.opentripplanner.updater.vehicle_location.VehicleLocation;
import org.opentripplanner.updater.vehicle_location.VehicleLocationService;

/**
 * Implements the {@code current-time} OneBusAway API method. Also returns the validity of the currently
 * used schedules.
 * 
 * @see http://developer.onebusaway.org/modules/onebusaway-application-modules/current/api/where/methods/trip-for-vehicle.html
 */

@Path(OneBusAwayApiMethod.API_BASE_PATH + "trip-for-vehicle/{vehicleId}" + OneBusAwayApiMethod.API_CONTENT_TYPE)
public class TripForVehicleMethod extends OneBusAwayApiMethod {

    @PathParam("vehicleId") private String vehicleIdString;
    @QueryParam("includeTrip") @DefaultValue("true") private boolean includeTrip; // TODO
    @QueryParam("includeSchedule") @DefaultValue("true") private boolean includeSchedule;
    @QueryParam("includeStatus") @DefaultValue("true") private boolean includeStatus;
    
    @Override
    protected TransitResponse getResponse() {
        
        VehicleLocationService vehicleLocationService = graph.getService(VehicleLocationService.class);
        if(vehicleLocationService == null)
            return TransitResponseBuilder.getFailResponse(TransitResponse.Status.ERROR_VEHICLE_LOCATION_SERVICE);
        
        AgencyAndId vehicleId = parseAgencyAndId(vehicleIdString);
        VehicleLocation vehicleLocation = vehicleLocationService.getForVehicle(vehicleId);
        if(vehicleLocation == null)
            return TransitResponseBuilder.getFailResponse(TransitResponse.Status.NOT_FOUND, "Vehicle ID not found.");

        if(vehicleLocation.getTripId() == null)
            return TransitResponseBuilder.getFailResponse(TransitResponse.Status.NOT_FOUND, "Vehicle lacks a trip.");
        
        TransitTripDetails tripDetails = getTripDetails(vehicleLocation.getTripId(), vehicleLocation.getServiceDate());
        
        if(tripDetails == null)
            return TransitResponseBuilder.getFailResponse(TransitResponse.Status.UNKNOWN_ERROR, "Failed to create TripDetails...");
        
        if(!includeSchedule)
            tripDetails.setSchedule(null);
        if(!includeStatus)
            tripDetails.setStatus(null);
        
        return responseBuilder.getResponseForTrip(tripDetails);
    }
}
