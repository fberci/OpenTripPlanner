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

package org.opentripplanner.api.ws.oba_rest_api.beans;

import java.util.Collection;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
public class TransitTripStatus {
    private long serviceDate;
    private String activeTripId;
    private String vehicleId;
    private String status = "default";
    private boolean predicted;
    private Object frequency;
    private Collection<String> situationIds;
    private String phase = "";
    private int scheduleDeviation;
    private int nextStopTimeOffset;
    private int closestStopTimeOffset;
    private int blockTripSequence;
    
    private TransitPoint position;
    private TransitPoint lastKnownLocation;
    private Float orientation;
    private Float lastKnownOrientation;
    private String closestStop;
    private String nextStop;
    private long lastUpdateTime;
    private long lastLocationUpdateTime;
    private Double distanceAlongTrip;
    private Double lastKnownDistanceAlongTrip;
    private Double totalDistanceAlongTrip;
    private Double scheduledDistanceAlongTrip;
}
