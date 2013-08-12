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

import com.vividsolutions.jts.geom.Envelope;
import java.util.Collection;
import java.util.List;
import org.onebusaway.gtfs.model.AgencyAndId;

public interface VehicleLocationService {
    
    public long getLastUpdateTime();
    
    public VehicleLocation getForVehicle(AgencyAndId routeId);
    public VehicleLocation getForTrip(AgencyAndId tripId);
    public List<VehicleLocation> getForRoute(AgencyAndId routeId);
    public List<VehicleLocation> getForAgency(String agencyId);
    public List<VehicleLocation> getForArea(Envelope area);
    public Collection<VehicleLocation> getAll();
    
    public boolean add(VehicleLocation vehicle);
    public VehicleLocation remove(AgencyAndId vehicleId);

    public void refresh(Collection<VehicleLocation> updatedLocations);
}
