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

import java.util.HashMap;
import java.util.Map;
import lombok.Data;

@Data
public class OTPTransitReferences implements TransitReferences {
    
    private Map<String, TransitAgency> agencies = new HashMap<String, TransitAgency>();
    private Map<String, TransitRoute> routes = new HashMap<String, TransitRoute>();
    private Map<String, TransitStop> stops = new HashMap<String, TransitStop>();
    private Map<String, TransitTrip> trips = new HashMap<String, TransitTrip>();
    private Map<String, TransitAlert> alerts = new HashMap<String, TransitAlert>();

    public void addAgency(TransitAgency agency) {
        agencies.put(agency.getId(), agency);
    }
    
    public void addRoute(TransitRoute route) {
        routes.put(route.getId(), route);
    }
    
    public void addStop(TransitStop stop) {
        stops.put(stop.getId(), stop);
    }
    
    public void addTrip(TransitTrip trip) {
        trips.put(trip.getId(), trip);
    }
    
    public void addAlert(TransitAlert alert) {
        alerts.put(alert.getId(), alert);
    }
}
