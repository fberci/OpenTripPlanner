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

@Data
public class TransitTripDetailsOTP {
    private String tripId;
    private String serviceDate;
    private TransitVehicle vehicle;
    private TransitPolyline polyline;
    private Collection<String> alertIds;
    private Collection<TransitStopTime> stopTimes;
}