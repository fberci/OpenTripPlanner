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

import lombok.Data;

import javax.xml.bind.annotation.XmlRootElement;

@Data
@XmlRootElement(name = "arrivalAndDeparture")
public class TransitArrivalAndDeparture {
    private String routeId;
    private String tripId;
    private long serviceDate;
    private int stopSequence;
    private int blockStopSequence;
    private String routeShortName;
    private String routeLongName;
    private String tripHeadsign;
    private boolean arrivalEnabled;
    private boolean departureEnabled;
    private long scheduledArrivalTime;
    private long scheduledDepartureTime;
    private Object frequency;
    private boolean predicted;
    private long predictedArrivalTime;
    private long predictedDepartureTime;
    private double distanceFromStop;
    private int numberOfStopsAway;
    private TransitTripStatus tripStatus;
}