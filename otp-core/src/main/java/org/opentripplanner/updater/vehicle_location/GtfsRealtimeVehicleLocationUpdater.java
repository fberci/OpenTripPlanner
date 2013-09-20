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

import com.google.protobuf.ExtensionRegistry;
import java.io.IOException;
import java.io.InputStream;
import java.util.prefs.Preferences;

import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.impl.PatchServiceImpl;
import org.opentripplanner.routing.services.PatchService;
import org.opentripplanner.updater.GraphUpdaterManager;
import org.opentripplanner.updater.GraphWriterRunnable;
import org.opentripplanner.updater.PollingGraphUpdater;
import org.opentripplanner.util.HttpUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.transit.realtime.GtfsRealtime;
import com.google.transit.realtime.GtfsRealtimeBplanner;
import java.text.ParseException;
import java.util.LinkedList;
import java.util.List;
import lombok.Setter;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.model.calendar.ServiceDate;
import org.opentripplanner.routing.edgetype.TableTripPattern;
import org.opentripplanner.routing.services.TransitIndexService;

/**
 * GTFS-RT vehicle locations updater
 * 
 * Usage example ('myalert' name is an example) in file 'Graph.properties':
 * 
 * <pre>
 * myalert.type = vehicle-locations
 * myalert.frequencySec = 60
 * myalert.url = http://host.tld/path
 * myalert.defaultAgencyId = TA
 * </pre>
 */
public class GtfsRealtimeVehicleLocationUpdater extends PollingGraphUpdater {
    
    private static final Logger LOG = LoggerFactory.getLogger(GtfsRealtimeVehicleLocationUpdater.class);

    protected ExtensionRegistry extensionRegistry = ExtensionRegistry.newInstance();
    {
        extensionRegistry.add(GtfsRealtimeBplanner.deviated);
        extensionRegistry.add(GtfsRealtimeBplanner.wheelchairAccessible);
    }
    
    private Graph graph;
    
    private Long lastTimestamp = Long.MIN_VALUE;
    
    private HttpUtils httpUtils;
    
    @Setter
    private String url;

    @Setter
    private String defaultAgencyId;

    @Setter
    private GraphUpdaterManager graphUpdaterManager;

    @Override
    protected void configurePolling(Graph graph, Preferences preferences) throws Exception {
        String url = preferences.get("url", null);
        if (url == null)
            throw new IllegalArgumentException("Missing mandatory 'url' parameter");
        this.url = url;
        this.defaultAgencyId = preferences.get("defaultAgencyId", null);
        LOG.info("Creating real-time vehicle location updater running every {} seconds : {}",
                getFrequencySec(), url);
        
        this.graph = graph;
    }

    @Override
    public void setup() {
        httpUtils = new HttpUtils();
    }

    @Override
    protected void runPolling() throws Exception {
        try {
            List<VehicleLocation> updatedLocations = getVehicleLocationUpdates();
            if(updatedLocations == null)
                return;

            TransitIndexService transitIndexService = graph.getService(TransitIndexService.class);
            VehicleLocationService vehicleLocationService = graph.getService(VehicleLocationService.class);
            if(vehicleLocationService == null) {
                VehicleLocationServiceImpl impl = new VehicleLocationServiceImpl();
                impl.setGraph(graph);
                vehicleLocationService = impl;
                graph.putService(VehicleLocationService.class, vehicleLocationService);
            }

            List<VehicleLocation> validLocations = new LinkedList<VehicleLocation>();
            for(VehicleLocation location : updatedLocations) {
                TableTripPattern tripPattern = null;
                if(location.getRouteId() != null && !transitIndexService.getAllRoutes().containsKey(location.getRouteId())) {
                    LOG.warn("Location update references an unknown route: " + location);
                    continue;
                }
                if(location.getTripId() != null) {
                    tripPattern = transitIndexService.getTripPatternForTrip(location.getTripId());
                    if(tripPattern == null) {
                        LOG.warn("Location update references an unknown trip: " + location);
                        continue;
                    }
                }
                Stop stop = null;
                if(location.getStopId() != null) {
                    stop = transitIndexService.getAllStops().get(location.getStopId());
                    if(stop == null) {
                        LOG.warn("Location update references an unknown stop: " + location);
                        continue;
                    }
                }
                if(stop != null && tripPattern != null && !tripPattern.getStops().contains(stop)) {
                    LOG.warn("Location update references an unknown stop for trip: " + location);
                    continue;
                }

                validLocations.add(location);
            }

            vehicleLocationService.refresh(validLocations);
        } catch (Exception e) {
            LOG.error("Eror reading gtfs-realtime feed from " + url, e);
        }
    }
    
    public List<VehicleLocation> getVehicleLocationUpdates() throws Exception {
        GtfsRealtime.FeedMessage feed = getFeedMessage();
        if (feed == null)
            return null;
        
        GtfsRealtime.FeedHeader header = feed.getHeader();
        if(header.hasIncrementality() && header.getIncrementality() == GtfsRealtime.FeedHeader.Incrementality.DIFFERENTIAL) {
            LOG.error("DIFFERENTIAL GTFS-rt updates are not supported.");
            return null;
        }
        
        long feedTimestamp = header.getTimestamp();
        if(feedTimestamp <= lastTimestamp) {
            LOG.info("Ignoring feed with an old timestamp.");
            return null;
        }
        
        List<VehicleLocation> ret = new LinkedList<VehicleLocation>();
        for (GtfsRealtime.FeedEntity entity : feed.getEntityList()) {
            if(!entity.hasVehicle()) {
                continue;
            }
            
            GtfsRealtime.VehiclePosition vehiclePosition = entity.getVehicle();
            GtfsRealtime.TripDescriptor descriptor = vehiclePosition.getTrip();
            
            AgencyAndId tripId = null;
            ServiceDate serviceDate = new ServiceDate();
            if(descriptor.hasTripId()) {
                tripId = new AgencyAndId(defaultAgencyId, descriptor.getTripId());
                if (descriptor.hasStartDate()) {
                    try {
                       serviceDate = ServiceDate.parseString(descriptor.getStartDate());
                    } catch (ParseException e) {
                        LOG.warn("Failed to parse startDate in gtfs-rt feed: \n{}", entity);
                        continue;
                    }
                }
            }
            
            AgencyAndId routeId = null;
            if(descriptor.hasRouteId()) {
                routeId = new AgencyAndId(defaultAgencyId, descriptor.getRouteId());
            }
            
            Integer stopSequence = null;
            AgencyAndId stopId = null;
            if(vehiclePosition.hasCurrentStopSequence())
                stopSequence = vehiclePosition.getCurrentStopSequence();
            if(vehiclePosition.hasStopId())
                stopId = new AgencyAndId(defaultAgencyId, vehiclePosition.getStopId());

            boolean deviated = false;
            AgencyAndId vehicleId = new AgencyAndId(defaultAgencyId, entity.getId());
            String licensePlate = null, label = null;
            if(vehiclePosition.hasVehicle()) {
                GtfsRealtime.VehicleDescriptor vehicle = vehiclePosition.getVehicle();
                if(vehicle.hasLicensePlate())
                    licensePlate = vehicle.getLicensePlate();
                if(vehicle.hasLabel())
                    label = vehicle.getLabel();
                if(vehicle.hasId())
                    vehicleId = new AgencyAndId(defaultAgencyId, vehicle.getId());
                if(vehicle.hasExtension(GtfsRealtimeBplanner.deviated))
                    deviated = vehicle.getExtension(GtfsRealtimeBplanner.deviated);
            }
            
            long timestamp = feed.getHeader().getTimestamp();
            if(vehiclePosition.hasTimestamp())
                timestamp = vehiclePosition.getTimestamp();
            
            Float lat = null, lon = null;
            Float bearing = null;
            if(vehiclePosition.hasPosition()) {
                GtfsRealtime.Position position = vehiclePosition.getPosition();
                lat = position.getLatitude();
                lon = position.getLongitude();
                if(position.hasBearing())
                    bearing = position.getBearing();
            }
            
            VehicleLocation.Status status = null;
            if(vehiclePosition.hasCurrentStatus()) {
                switch(vehiclePosition.getCurrentStatus()) {
                    case INCOMING_AT:
                        status = VehicleLocation.Status.INCOMING_AT;
                        break;
                    case IN_TRANSIT_TO:
                        status = VehicleLocation.Status.IN_TRANSIT_TO;
                        break;
                    case STOPPED_AT:
                        status = VehicleLocation.Status.STOPPED_AT;
                        break;
                }
            }
            
            VehicleLocation.CongestionLevel congestionLevel = null;
            if(vehiclePosition.hasCongestionLevel())
                switch(vehiclePosition.getCongestionLevel()) {
                    case CONGESTION:
                        congestionLevel = VehicleLocation.CongestionLevel.CONGESTION;
                        break;
                    default:
                        congestionLevel = VehicleLocation.CongestionLevel.UNKNOWN;
                        break;
                }
            
            VehicleLocation vehicleLocation = new VehicleLocation(timestamp, vehicleId, routeId, lat, lon,
                    tripId, licensePlate, label, bearing, status, stopId, stopSequence, serviceDate, congestionLevel, deviated);
            ret.add(vehicleLocation);
        }
        
        lastTimestamp = feedTimestamp;
        
        return ret;
    }
    
    protected GtfsRealtime.FeedMessage getFeedMessage() throws Exception {
        GtfsRealtime.FeedMessage feed = null;
        InputStream is = null;
        try {
            is = httpUtils.getData(url, lastTimestamp);
            if(is != null)
                feed = GtfsRealtime.FeedMessage.PARSER.parseFrom(is, extensionRegistry);
        } catch (IOException e) {
            LOG.warn("Failed to parse gtfs-rt feed from " + url + ":", e);
        } finally {
            if(is != null) {
                is.close();
            }
        }
        return feed;
    }

    @Override
    public void teardown() {
        httpUtils.cleanup();
    }

    public String toString() {
        return "GtfsRealtimeVehicleLocationUpdater(" + url + ")";
    }

}
