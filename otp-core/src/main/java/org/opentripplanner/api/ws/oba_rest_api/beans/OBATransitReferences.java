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
public class OBATransitReferences implements TransitReferences {

    public OBATransitReferences(OTPTransitReferences references) {
        agencies = references.getAgencies().values();
        routes = references.getRoutes().values();
        stops = references.getStops().values();
        trips = references.getTrips().values();
        situations = references.getAlerts().values();
    }
    
    private Collection<TransitAgency> agencies;
    private Collection<TransitRoute> routes;
    private Collection<TransitStop> stops;
    private Collection<TransitTrip> trips;
    private Collection<TransitAlert> situations;
}
