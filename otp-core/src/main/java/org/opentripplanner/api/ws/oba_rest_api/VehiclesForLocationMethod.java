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

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import java.util.Calendar;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import org.onebusaway.gtfs.model.Agency;
import org.onebusaway.gtfs.model.calendar.ServiceDate;
import org.opentripplanner.api.ws.oba_rest_api.beans.TransitMetadata;
import org.opentripplanner.api.ws.oba_rest_api.beans.TransitResponse;
import org.opentripplanner.api.ws.oba_rest_api.beans.TransitResponseBuilder;
import org.opentripplanner.api.ws.oba_rest_api.beans.TransitVehicle;
import org.opentripplanner.updater.vehicle_location.VehicleLocation;
import org.opentripplanner.updater.vehicle_location.VehicleLocationService;

@Path(OneBusAwayApiMethod.API_BASE_PATH + "vehicles-for-location" + OneBusAwayApiMethod.API_CONTENT_TYPE)
public class VehiclesForLocationMethod extends OneBusAwayApiMethod {
    
    @QueryParam("lat") private Float lat;
    @QueryParam("lon") private Float lon;
    @QueryParam("latSpan") private Float latSpan;
    @QueryParam("lonSpan") private Float lonSpan;
    @QueryParam("radius") @DefaultValue("100") private int radius;
    @QueryParam("ifModifiedSince") @DefaultValue("-1") long ifModifiedSince;
    
    @Override
    protected TransitResponse getResponse() {
        VehicleLocationService vehicleLocationService = graph.getService(VehicleLocationService.class);
        if(vehicleLocationService == null)
            return TransitResponseBuilder.getFailResponse(TransitResponse.Status.ERROR_VEHICLE_LOCATION_SERVICE);
        
        if(ifModifiedSince >= vehicleLocationService.getLastUpdateTime()) {
            return TransitResponseBuilder.getFailResponse(TransitResponse.Status.NOT_MODIFIED);
        }

        Collection<VehicleLocation> vehicles;
        
        if(lat != null && lon != null) {
            Coordinate center = new Coordinate(lon, lat);
            Envelope area;
            
            if(lonSpan != null && latSpan != null) {
                Coordinate c1 = new Coordinate(lon - lonSpan, lat - latSpan),
                           c2 = new Coordinate(lon + lonSpan, lat + latSpan);
                
                area = new Envelope(c1, c2);
            } else {
                area = new Envelope(center);
                area.expandBy(radius);
            }
            
            vehicles = vehicleLocationService.getForArea(area);
        } else {
            vehicles = vehicleLocationService.getAll();            
        }
        
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
