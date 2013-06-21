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
import com.vividsolutions.jts.index.quadtree.Quadtree;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import lombok.Setter;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.opentripplanner.common.geometry.DirectionUtils;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.services.GraphService;
import org.opentripplanner.routing.services.TransitIndexService;
import org.opentripplanner.util.MapUtils;
import org.springframework.beans.factory.annotation.Autowired;

public class VehicleLocationServiceImpl implements VehicleLocationService {
    
    @Setter
    @Autowired
    private GraphService graphService;
    
    private Quadtree _quadtree = new Quadtree();
    
    private Map<AgencyAndId, VehicleLocation> _vehicleLocations = new HashMap<AgencyAndId, VehicleLocation>();
    
    private Map<AgencyAndId, VehicleLocation> _vehicleLocationsByTrip = new HashMap<AgencyAndId, VehicleLocation>();
    
    private Map<AgencyAndId, List<VehicleLocation>> _vehicleLocationsByRoute = new HashMap<AgencyAndId, List<VehicleLocation>>();

    @Override
    public VehicleLocation getForVehicle(AgencyAndId vehicleId) {
        return _vehicleLocations.get(vehicleId);
    }

    @Override
    public VehicleLocation getForTrip(AgencyAndId tripId) {
        return _vehicleLocationsByTrip.get(tripId);
    }

    @Override
    public List<VehicleLocation> getForRoute(AgencyAndId routeId) {
        List<VehicleLocation> locations = _vehicleLocationsByRoute.get(routeId);
        if(locations == null)
            return Collections.emptyList();
        return Collections.unmodifiableList(locations);
    }

    @Override
    public List<VehicleLocation> getForArea(Envelope area) {
        List maybeInside = _quadtree.query(area);
        
        List<VehicleLocation> vehicles = new LinkedList<VehicleLocation>();
        for(Object o : maybeInside) {
            VehicleLocation vehicle = (VehicleLocation) o;
            if(area.contains(vehicle.getCoordinate()))
                vehicles.add(vehicle);
        }
        
        return vehicles;
    }
    
    @Override
    public Collection<VehicleLocation> getAll() {
        return Collections.unmodifiableCollection(_vehicleLocations.values());
    }

    @Override
    public boolean add(VehicleLocation vehicle) {
        if(_vehicleLocations.containsKey(vehicle.getVehicleId())) {
            VehicleLocation existingLocation = _vehicleLocations.get(vehicle.getVehicleId());
            if(vehicle.getTimestamp() <= existingLocation.getTimestamp())
                return false;
            
            remove(vehicle.getVehicleId());
            
            if(!existingLocation.getCoordinate().equals(vehicle.getCoordinate()))
                vehicle.setBearing(new Float(DirectionUtils.getAzimuth(existingLocation.getCoordinate(), vehicle.getCoordinate())));
            else
                vehicle.setBearing(existingLocation.getBearing());
        }
        
        Graph graph = graphService.getGraph();
        TransitIndexService transitIndexService = graph.getService(TransitIndexService.class);
        AgencyAndId routeId = null;
        if(vehicle.getTripId() != null)
            routeId = transitIndexService.getPatternForTrip(vehicle.getTripId()).getExemplar().getRoute().getId();
        
        synchronized(this) {
            _quadtree.insert(new Envelope(vehicle.getCoordinate()), vehicle);
            _vehicleLocations.put(vehicle.getVehicleId(), vehicle);
            if(vehicle.getTripId() != null)
                _vehicleLocationsByTrip.put(vehicle.getTripId(), vehicle);
            if(routeId != null)
                MapUtils.addToMapList(_vehicleLocationsByRoute, routeId, vehicle);
        }
        
        return true;
    }

    @Override
    public VehicleLocation remove(AgencyAndId vehicleId) {
        if(!_vehicleLocations.containsKey(vehicleId))
            return null;
        
        VehicleLocation vehicle = _vehicleLocations.get(vehicleId);
        
        Graph graph = graphService.getGraph();
        TransitIndexService transitIndexService = graph.getService(TransitIndexService.class);        AgencyAndId routeId = null;
        if(vehicle.getTripId() != null)
            routeId = transitIndexService.getPatternForTrip(vehicle.getTripId()).getExemplar().getRoute().getId();
        
        synchronized(this) {
            _quadtree.remove(new Envelope(vehicle.getCoordinate()), vehicle);
            _vehicleLocations.remove(vehicleId);
            if(vehicle.getTripId() != null)
                _vehicleLocationsByTrip.remove(vehicle.getTripId());
            if(routeId != null)
                MapUtils.removeFromMapList(_vehicleLocationsByRoute, routeId, vehicle);
        }
        
        return vehicle;
    }
}
