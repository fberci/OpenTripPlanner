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

import java.util.LinkedList;
import java.util.List;
import lombok.Setter;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.services.GraphService;
import org.opentripplanner.routing.services.TransitIndexService;
import org.opentripplanner.updater.UpdateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class VehicleLocationUpdater implements Runnable {
    
    private static final Logger LOG = LoggerFactory.getLogger(VehicleLocationUpdater.class);
    
    @Setter
    private VehicleLocationUpdateProvider _provider;

    @Setter @Autowired
    private GraphService graphService;
    
    @Override
    public void run() {
        List<VehicleLocation> updatedLocations = _provider.getVehicleLocationUpdates();
        if(updatedLocations == null)
            return;
        
        Graph graph = graphService.getGraph();
        TransitIndexService transitIndexService = graph.getService(TransitIndexService.class);
        VehicleLocationService vehicleLocationService = graph.getService(VehicleLocationService.class);
        if(vehicleLocationService == null) {
            VehicleLocationServiceImpl impl = new VehicleLocationServiceImpl();
            impl.setGraphService(graphService);
            vehicleLocationService = impl;
            graph.putService(VehicleLocationService.class, vehicleLocationService);
        }
        
        List<VehicleLocation> validLocations = new LinkedList<VehicleLocation>();
        for(VehicleLocation location : updatedLocations) {
            if(location.getTripId() != null && transitIndexService.getPatternForTrip(location.getTripId()) == null) {
                LOG.warn("Location update references an unknown trip: " + location);
                continue;
            }
            if(location.getStopId() != null && !transitIndexService.getAllStops().containsKey(location.getStopId())) {
                LOG.warn("Location update references an unknown stop: " + location);
                continue;
            }
            if(location.getRouteId() != null && !transitIndexService.getAllRoutes().containsKey(location.getRouteId())) {
                LOG.warn("Location update references an unknown route: " + location);
                continue;
            }
            
            validLocations.add(location);
        }
        
        vehicleLocationService.refresh(validLocations);
    }
}
