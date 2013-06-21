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
import com.google.transit.realtime.GtfsRealtime;
import com.vividsolutions.jts.geom.Coordinate;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;
import lombok.Setter;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.calendar.ServiceDate;
import org.opentripplanner.routing.services.GraphService;
import org.opentripplanner.updater.stoptime.GtfsRealtimeAbstractUpdateStreamer;
import org.opentripplanner.util.HttpUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;


public class GtfsRealtimeVehicleLocationUpdateProvider implements VehicleLocationUpdateProvider {
    
    private static final Logger LOG = LoggerFactory .getLogger(GtfsRealtimeAbstractUpdateStreamer.class);
    
    private static final SimpleDateFormat ymdParser = new SimpleDateFormat("yyyyMMdd");
    {
        ymdParser.setTimeZone(TimeZone.getTimeZone("GMT"));
    }

    protected ExtensionRegistry extensionRegistry = ExtensionRegistry.newInstance();
    {
        extensionRegistry.add(GtfsRealtime.deviated);
        extensionRegistry.add(GtfsRealtime.wheelchairAccessible);
    }
    
    @Setter
    private String url;

    @Setter
    private String defaultAgencyId;
    
    @Override
    public List<VehicleLocation> getVehicleLocationUpdates() {
        GtfsRealtime.FeedMessage feed = getFeedMessage();
        if (feed == null)
            return null;
        
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
                        Date date = ymdParser.parse(descriptor.getStartDate());
                        serviceDate = new ServiceDate(date);
                    } catch (ParseException e) {
                        LOG.warn("Failed to parse startDate in gtfs-rt feed: \n{}", entity);
                        continue;
                    }
                }
            }
            
            Integer stopSequence = null;
            AgencyAndId stopId = null;
            if(vehiclePosition.hasCurrentStopSequence())
                stopSequence = vehiclePosition.getCurrentStopSequence();
            if(vehiclePosition.hasStopId())
                stopId = new AgencyAndId(defaultAgencyId, vehiclePosition.getStopId());

            AgencyAndId vehicleId = new AgencyAndId(defaultAgencyId, entity.getId());
            String licensePlate = null;
            if(vehiclePosition.hasVehicle()) {
                GtfsRealtime.VehicleDescriptor vehicle = vehiclePosition.getVehicle();
                if(vehicle.hasLicensePlate())
                    licensePlate = vehicle.getLicensePlate();
                if(vehicle.hasId())
                    vehicleId = new AgencyAndId(defaultAgencyId, vehicle.getId());
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
            
            VehicleLocation vehicleLocation = new VehicleLocation(timestamp, vehicleId, lat, lon, tripId, licensePlate, bearing, status, stopId, stopSequence, serviceDate);
            ret.add(vehicleLocation);
        }
        
        return ret;
    }
    
    protected GtfsRealtime.FeedMessage getFeedMessage() {
        GtfsRealtime.FeedMessage feed = null;
        try {
            InputStream is = HttpUtils.getData(url);
            feed = GtfsRealtime.FeedMessage.parseFrom(is, extensionRegistry);
        } catch (IOException e) {
            LOG.warn("Failed to parse gtfs-rt feed from " + url + ":", e);
        }
        return feed;
    }
}
