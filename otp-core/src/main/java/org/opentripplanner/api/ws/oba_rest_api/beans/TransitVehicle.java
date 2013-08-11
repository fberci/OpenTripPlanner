
package org.opentripplanner.api.ws.oba_rest_api.beans;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Data;
import org.opentripplanner.updater.vehicle_location.VehicleLocation;

@Data
public class TransitVehicle {
    private String vehicleId;
    private String stopId;
    private String routeId;
    private Float bearing;
    private TransitPoint location;
    private String serviceDate;
    private String licensePlate;
    private String label;
    private boolean deviated;
    private long lastUpdateTime;
    private VehicleLocation.Status status;
    private VehicleLocation.CongestionLevel congestionLevel;
    
    @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    private String tripId;
}
