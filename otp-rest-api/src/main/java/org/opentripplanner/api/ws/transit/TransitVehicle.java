
package org.opentripplanner.api.ws.transit;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Getter;
import lombok.Setter;
import org.opentripplanner.updater.vehicle_location.VehicleLocation;

public class TransitVehicle {
    @Getter @Setter private long timestamp;
    @Getter @Setter private String id;
    @Getter @Setter private String stopId;
    @Getter @Setter private Float bearing;
    @Getter @Setter private Float latitude;
    @Getter @Setter private Float longitude;
    @Getter @Setter private String serviceDate;
    @Getter @Setter private String licensePlate;
    @Getter @Setter private VehicleLocation.Status status;
    
    @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    @Getter @Setter private String tripId;
}
