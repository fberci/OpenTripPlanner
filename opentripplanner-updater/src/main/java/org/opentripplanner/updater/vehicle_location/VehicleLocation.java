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

package org.opentripplanner.updater.vehicle_location;

import com.vividsolutions.jts.geom.Coordinate;
import lombok.Getter;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.calendar.ServiceDate;

/**
 * Describes the position of a vehicle on a trip;
 */
public class VehicleLocation {
    
    public VehicleLocation(long timestamp, AgencyAndId vehicleId, Float lat, Float lon, AgencyAndId tripId, String licensePlate, Float bearing, Status status, ServiceDate serviceDate) {
        this.timestamp = timestamp;
        this.vehicleId = vehicleId;
        this.latitude = lat;
        this.longitude = lon;
        this.tripId = tripId;
        this.bearing = bearing;
        this.status = status;
        this.licensePlate = licensePlate;
        this.serviceDate = serviceDate;
    }

    public VehicleLocation(long timestamp, AgencyAndId vehicleId, Float lat, Float lon, AgencyAndId tripId, String licensePlate, Float bearing, Status status, AgencyAndId stopId, Integer stopSequence, ServiceDate serviceDate) {
        this.timestamp = timestamp;
        this.vehicleId = vehicleId;
        this.latitude = lat;
        this.longitude = lon;
        this.tripId = tripId;
        this.bearing = bearing;
        this.status = status;
        this.licensePlate = licensePlate;
        this.stopId = stopId;
        this.stopSequence = stopSequence;
        this.serviceDate = serviceDate;
    }
    
    public Coordinate getCoordinate() {
        if(coordinate == null)
            coordinate = new Coordinate(longitude, latitude);
        return coordinate;
    }
    
    @Getter
    private long timestamp;
    
    @Getter
    private AgencyAndId vehicleId;

    @Getter
    private Float latitude;

    @Getter
    private Float longitude;
    
    @Getter
    private Float bearing;
    
    @Getter
    private AgencyAndId tripId;
    
    @Getter
    private String licensePlate;
    
    @Getter
    private Status status;
    
    @Getter
    private AgencyAndId stopId;
    
    @Getter
    private Integer stopSequence;
    
    @Getter
    private ServiceDate serviceDate;
    
    private Coordinate coordinate;

    void setBearing(Float bearing) {
        this.bearing = bearing;
    }
    
    public enum Status {
        INCOMING_AT,
        STOPPED_AT,
        IN_TRANSIT_TO
    };
}
