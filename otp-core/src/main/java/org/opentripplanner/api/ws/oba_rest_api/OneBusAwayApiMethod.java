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

package org.opentripplanner.api.ws.oba_rest_api;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.sun.jersey.api.core.InjectParam;
import com.sun.jersey.api.spring.Autowire;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import lombok.Setter;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.model.Trip;
import org.onebusaway.gtfs.model.calendar.ServiceDate;
import org.onebusaway.gtfs.services.calendar.CalendarService;
import org.opentripplanner.api.ws.oba_rest_api.beans.TransitAlert;
import org.opentripplanner.api.ws.oba_rest_api.beans.TransitPoint;
import org.opentripplanner.api.ws.oba_rest_api.beans.TransitResponse;
import org.opentripplanner.api.ws.oba_rest_api.beans.TransitResponseBuilder;
import org.opentripplanner.api.ws.oba_rest_api.beans.TransitScheduleStopTime;
import org.opentripplanner.api.ws.oba_rest_api.beans.TransitStopTime;
import org.opentripplanner.api.ws.oba_rest_api.beans.TransitTrip;
import org.opentripplanner.api.ws.oba_rest_api.beans.TransitTripDetails;
import org.opentripplanner.api.ws.oba_rest_api.beans.TransitTripSchedule;
import org.opentripplanner.api.ws.oba_rest_api.beans.TransitTripStatus;
import org.opentripplanner.api.ws.oba_rest_api.beans.TransitVehicle;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.common.model.T2;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.ServiceDay;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.edgetype.PatternHop;
import org.opentripplanner.routing.edgetype.PreAlightEdge;
import org.opentripplanner.routing.edgetype.PreBoardEdge;
import org.opentripplanner.routing.edgetype.TableTripPattern;
import org.opentripplanner.routing.edgetype.Timetable;
import org.opentripplanner.routing.edgetype.TransitBoardAlight;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.patch.Alert;
import org.opentripplanner.routing.patch.Patch;
import org.opentripplanner.routing.services.GraphService;
import org.opentripplanner.routing.services.PatchService;
import org.opentripplanner.routing.services.TransitIndexService;
import org.opentripplanner.routing.transit_index.RouteVariant;
import org.opentripplanner.routing.transit_index.adapters.TripsModelInfo;
import org.opentripplanner.routing.trippattern.CanceledTripTimes;
import org.opentripplanner.routing.trippattern.ScheduledTripTimes;
import org.opentripplanner.routing.trippattern.TripTimes;
import org.opentripplanner.routing.vertextype.TransitStop;
import org.opentripplanner.updater.vehicle_location.VehicleLocation;
import org.opentripplanner.updater.vehicle_location.VehicleLocationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A base class for imitating the OneBusAway REST API.
 * 
 * The following methods are implements:
 *  - agencies-with-coverage           {@link AgenciesWithCoverageMethod}
 *  - agency                           {@link AgencyMethod}
 *  - arrival-and-departure-for-stop   
 *  - arrivals-and-departures-for-stop 
 *  - cancel-alarm                     -
 *  - current-time                     {@link MetadataMethod}
 *  - plan-trip                        {@link PlanTripMethod}, <i>not OBA compatible</i>
 *  - register-alarm-for-arrival-and-departure-at-stop
 *  - report-problem-with-stop          -
 *  - report-problem-with-trip          -
 *  - route-ids-for-agency              {@link RouteIdsForAgencyMethod}
 *  - route                             {@link RouteMethod}
 *  - routes-for-agency                 {@link RoutesForAgencyMethod}
 *  - routes-for-location               {@link RoutesForLocationMethod}
 *  - schedule-for-stop                 
 *  - shape                             
 *  - stop-ids-for-agency               {@link StopIdsForAgencyMethod}
 *  - stop                              {@link StopMethod}
 *  - stops-for-location                {@link StopsForLocationMethod}
 *  - stops-for-route                   {@link StopsForRouteMethod}
 *  - trip-details                      {@link TripDetailsMethod}
 *  - trip-for-vehicle                  {@link TripForVehicleMethod}
 *  - trip                              {@link TripMethod}
 *  - trips-for-location                
 *  - trips-for-route                   
 *  - vehicles-for-agency               {@link VehiclesForAgencyMethod}
 * 
 * With the following "extras":
 *  - metadata                          {@link MetadataMethod}
 *  - search                            {@link SearchMethod}
 *  - alert-search                      {@link AlertSearchMethod}
 *  - trip-details                      {@link TripDetailsOTPMethod}
 *  - route-details                     {@link RouteDetailsMethod}
 *  - route-details-for-stop            {@link RouteDetailsForStopMethod}
 *  - vehicles-for-location             {@link VehiclesForLocationMethod}
 *  - vehicles-for-stop                 {@link VehiclesForStopMethod}
 *  - vehicles-for-route                {@link VehiclesForRouteMethod}
 * 
 * Differences
 *  - StopV2Bean#direction
 *  - RouteV2Bean#type
 * 
 * @see http://developer.onebusaway.org/modules/onebusaway-application-modules/current/api/where/index.html
 */

@Autowire
@Produces({ MediaType.APPLICATION_JSON }) //, MediaType.APPLICATION_XML, MediaType.TEXT_XML })
public abstract class OneBusAwayApiMethod {
    private static final Logger LOG = LoggerFactory.getLogger(OneBusAwayApiMethod.class);

    /**
     * The base API path which all methods are relative to.
     */
    public final static String API_BASE_PATH = "/{dialect : (?:oba|otp)}/api/where/";
    
    /**
     * The accepted content type extensions.
     */
    public final static String API_CONTENT_TYPE = "{contentType : (?:|\\.xml|\\.json)}";
    
    public static final int NEARBY_STOP_RADIUS = 100;
    
    @QueryParam("key") protected String apiKey;
    @PathParam ("dialect") protected TransitResponseBuilder.Dialect dialect;
    @QueryParam("routerId") protected String routerId;
    @PathParam ("contentType") protected String contentType;
    @QueryParam("includeReferences") protected @DefaultValue("true") boolean references;

    @Setter @InjectParam
    private GraphService graphService;
    
    protected Graph graph;
    protected TransitIndexService transitIndexService;
    
    protected TransitResponseBuilder responseBuilder;
    
    @GET
    public TransitResponse processResponse() {
        graph = getGraph(routerId);
        if(graph == null) {
            return TransitResponseBuilder.getFailResponse(TransitResponse.Status.ERROR_NO_GRAPH);
        }
        
        transitIndexService = graph.getService(TransitIndexService.class);
        if (transitIndexService == null) {
            return TransitResponseBuilder.getFailResponse(TransitResponse.Status.ERROR_TRANSIT_INDEX_SERVICE);
        }
        
        TransitResponse transitResponse;
        responseBuilder = new TransitResponseBuilder(graph, references, dialect);
        
        try {
            transitResponse = getResponse();
        } catch (Exception e) {
            transitResponse = TransitResponseBuilder.getFailResponse();
            LOG.warn("Unhandled exception: ", e);
        }
        
        return transitResponse;
    }
    
    abstract protected TransitResponse getResponse();
    
    // ---- [] ---- //
    
    protected TransitTripDetails getTripDetails(AgencyAndId tripId, ServiceDate serviceDate) {
        
        if(transitIndexService.getTripPatternForTrip(tripId) == null) {
            return null;
        }
        
        Trip trip = getTrip(tripId, serviceDate);
        
        CalendarService calendarService = graph.getCalendarService();
        ServiceDay serviceDay = new ServiceDay(graph, serviceDate.getAsDate(graph.getTimeZone()).getTime() / 1000, calendarService, trip.getId().getAgencyId());
        
        long startTime = serviceDate.getAsDate(graph.getTimeZone()).getTime() / 1000;
        long endTime = serviceDate.next().getAsDate(graph.getTimeZone()).getTime() / 1000 - 1;
        
        if(!graph.transitFeedCovers(startTime) && graph.transitFeedCovers(endTime)) {
            return null;
        }
        
        TableTripPattern pattern = transitIndexService.getTripPatternForTrip(tripId);
        if(!serviceDay.serviceIdRunning(pattern.getServiceId()))
            return null;
        
        Timetable timetable;
        if(graph.getTimetableSnapshotSource() != null && graph.getTimetableSnapshotSource().getTimetableSnapshot() != null) {
            timetable = graph.getTimetableSnapshotSource().getTimetableSnapshot().resolve(pattern, serviceDate);
        } else {
            timetable = pattern.getScheduledTimetable();
        }
        
        int tripIndex = timetable.getTripIndex(tripId);
        if(timetable.getTripTimes(tripIndex) instanceof CanceledTripTimes)
            return null;
        
        return getTripDetails(trip, serviceDate, pattern, timetable);
    }
    
    protected TransitTripDetails getTripDetails(Trip trip, ServiceDate serviceDate, TableTripPattern pattern,
            Timetable timetable) {
        
        List<TransitStopTime> stopTimes = getStopTimesForTrip(routerId, trip.getId(), serviceDate, pattern, timetable);
        
        int tripIndex = timetable.getTripIndex(trip.getId());
        TripTimes tripTimes = timetable.getTripTimes(tripIndex);
        
        VehicleLocation vehicle = null;
        VehicleLocationService vehicleLocationService = graph.getService(VehicleLocationService.class);
        if(vehicleLocationService != null) {
            vehicle = vehicleLocationService.getForTrip(trip.getId());
        }
        
        if(vehicle == null) {
            vehicle = createFakeVehicleForTrip(trip, serviceDate, pattern, tripTimes, stopTimes);
        }
        
        TransitTrip transitTrip = responseBuilder.getTrip(trip);
        transitTrip.setWheelchairAccessible(tripTimes.isWheelchairAccessible());
        responseBuilder.addToReferences(transitTrip);
        
        TransitStopTime first = stopTimes.get(0),
                        last  = stopTimes.get(stopTimes.size() - 1);
        long startTime = Math.min(first.hasDepartureTime() ? first.getDepartureTime() : Long.MAX_VALUE,
                                  first.hasPredictedDepartureTime() ? first.getPredictedDepartureTime() : Long.MAX_VALUE),
             endTime   = Math.max(last.hasArrivalTime() ? last.getArrivalTime() : Long.MIN_VALUE,
                                  last.hasPredictedArrivalTime() ? last.getPredictedArrivalTime() : Long.MIN_VALUE);
        RoutingRequest options = makeTraverseOptions(startTime, routerId);
        
        Collection<String> alertIds = getAlertsForTrip(trip.getId(), trip.getRoute().getId(), options, startTime, endTime);
        TransitTripStatus tripStatus = getTripStatus(trip, serviceDate, tripTimes, stopTimes, vehicle, alertIds);
        TransitTripSchedule tripSchedule = getTripSchedule(serviceDate, stopTimes); // mutates StopTimes to seconds-since-midnight
        
        TransitTripDetails tripDetails = new TransitTripDetails();
        tripDetails.setTripId(trip.getId().toString());
        tripDetails.setServiceDate(serviceDate.getAsDate(graph.getTimeZone()).getTime());
        tripDetails.setFrequency(null);
        tripDetails.setSchedule(tripSchedule);
        tripDetails.setStatus(tripStatus);
        tripDetails.setSituationIds(alertIds);
        return tripDetails;
    }
    
    protected VehicleLocation createFakeVehicleForTrip(Trip trip, ServiceDate serviceDate, TableTripPattern pattern,
            TripTimes tripTimes, List<TransitStopTime> stopTimes) {
        
        AgencyAndId tripId = trip.getId();
        AgencyAndId routeId = trip.getRoute().getId();
        
        long now = System.currentTimeMillis() / 1000;
        TransitStopTime nextStop = stopTimes.get(0);
        VehicleLocation.Status status = VehicleLocation.Status.STOPPED_AT;
        int hopIndex = -1;
        for(TransitStopTime stopTime : stopTimes) {
            if(stopTime.hasPredictedArrivalTime() && stopTime.getPredictedArrivalTime() >= now) {
                status = VehicleLocation.Status.IN_TRANSIT_TO;
                break;
            }
            if(stopTime.hasPredictedDepartureTime() && stopTime.getPredictedDepartureTime() >= now) {
                status = VehicleLocation.Status.STOPPED_AT;
                break;
            }
            if(stopTime.hasArrivalTime() && stopTime.getArrivalTime() >= now) {
                status = VehicleLocation.Status.IN_TRANSIT_TO;
                break;
            }
            if(stopTime.hasDepartureTime() && stopTime.getDepartureTime() >= now) {
                status = VehicleLocation.Status.STOPPED_AT;
                break;
            }
            nextStop = stopTime;
            hopIndex++;
        }
        
        double lat, lon;
        Float bearing = null;
        AgencyAndId stopId = parseAgencyAndId(nextStop.getStopId());
        
        if(status == VehicleLocation.Status.STOPPED_AT) {
            
            Stop stop = transitIndexService.getAllStops().get(stopId);
            lat = stop.getLat();
            lon = stop.getLon();
            if(stop.getDirection() != null) {
                bearing = Float.parseFloat(stop.getDirection());
            }
        } else {
            long arrivalTime = tripTimes.getArrivalTime(hopIndex);
            long departureTime = tripTimes.getDepartureTime(hopIndex);
            double factor = (now - departureTime) / (arrivalTime - departureTime);
            PatternHop patternHop = pattern.getPatternHops().get(hopIndex);
            LineString linestring = patternHop.getGeometry();
            LineString splitLineString = GeometryUtils.splitGeometryAtFraction(linestring, factor).getFirst();
            Point lastPoint = splitLineString.getEndPoint();
            lon = lastPoint.getX();
            lat = lastPoint.getY();
        }
        
        VehicleLocation vehicleLocation = new VehicleLocation(routeId, (float) lat, (float) lon, tripId,
                                                              bearing, status, stopId, serviceDate);
        return vehicleLocation;
    }
    
    protected TransitTripSchedule getTripSchedule(ServiceDate serviceDate, Collection<TransitStopTime> stopTimes) {
        
        long midnight = serviceDate.getAsDate(graph.getTimeZone()).getTime() / 100;
        for(TransitStopTime stopTime : stopTimes) {
            long arrivalTime = stopTime.hasArrivalTime() ? stopTime.getArrivalTime() : stopTime.getPredictedArrivalTime(),
                 departureTime = stopTime.hasDepartureTime() ? stopTime.getDepartureTime() : stopTime.getPredictedDepartureTime();
            
            stopTime.setArrivalTime(arrivalTime - midnight);
            stopTime.setDepartureTime(departureTime - midnight);
            
            if(stopTime.hasPredictedArrivalTime())
                stopTime.setPredictedArrivalTime(stopTime.getPredictedArrivalTime() - midnight);
            if(stopTime.hasPredictedDepartureTime())
                stopTime.setPredictedDepartureTime(stopTime.getPredictedDepartureTime() - midnight);
        }
        
        TransitTripSchedule tripSchedule = new TransitTripSchedule();
        
        tripSchedule.setFrequency(null);
        tripSchedule.setNextTripId(null);
        tripSchedule.setPreviousTripId(null);
        tripSchedule.setTimeZone(graph.getTimeZone().getDisplayName());
        tripSchedule.setStopTimes(stopTimes);
        
        return tripSchedule;
    }
    
    protected TransitTripStatus getTripStatus(Trip trip, ServiceDate serviceDate, TripTimes timetable,
            List<TransitStopTime> stopTimes, VehicleLocation vehicleLocation, Collection<String> alertIds) {
        
        TransitTripStatus tripStatus = new TransitTripStatus();
        
        tripStatus.setActiveTripId(trip.getId().toString());
        tripStatus.setBlockTripSequence(0);
        tripStatus.setPredicted(!(timetable instanceof ScheduledTripTimes));
        tripStatus.setFrequency(null);
        tripStatus.setPhase("");
        tripStatus.setServiceDate(serviceDate.getAsDate(graph.getTimeZone()).getTime());
        tripStatus.setSituationIds(alertIds);
        tripStatus.setStatus("default");
        
        if(vehicleLocation != null) {

            String nextStopId = vehicleLocation.getStopId().toString();
            TransitStopTime nextStop = null;
            for(TransitStopTime stopTime : stopTimes) {
                if(stopTime.getStopId().equals(nextStopId)) {
                    nextStop = stopTime;
                    break;
                }
            }
            
            long now = System.currentTimeMillis() / 1000,
                 nextArrivalTime = nextStop.hasPredictedArrivalTime() ? nextStop.getPredictedArrivalTime() : nextStop.getArrivalTime();
            
            if(vehicleLocation.getVehicleId() != null)
                tripStatus.setVehicleId(vehicleLocation.getVehicleId().toString());
            tripStatus.setClosestStop(vehicleLocation.getStopId().toString());
            tripStatus.setLastKnownLocation(new TransitPoint(vehicleLocation.getLatitude(), vehicleLocation.getLongitude()));
            tripStatus.setPosition(new TransitPoint(vehicleLocation.getLatitude(), vehicleLocation.getLongitude()));
            if(vehicleLocation.getBearing() != null) {
                tripStatus.setOrientation(90 + vehicleLocation.getBearing());
                tripStatus.setLastKnownOrientation(90 + vehicleLocation.getBearing());
            }
            tripStatus.setLastLocationUpdateTime(vehicleLocation.getTimestamp() * 1000);
            tripStatus.setLastUpdateTime(vehicleLocation.getTimestamp() * 1000);
            tripStatus.setLastKnownDistanceAlongTrip(.0); // TODO
            tripStatus.setTotalDistanceAlongTrip(.0); // TODO
            tripStatus.setDistanceAlongTrip(.0); // TODO
            tripStatus.setNextStop(nextStopId);
            tripStatus.setClosestStopTimeOffset((int) (nextArrivalTime - now));
            tripStatus.setNextStopTimeOffset((int) (nextArrivalTime - now));
            
            if(nextStop.hasPredictedArrivalTime() && nextStop.hasArrivalTime())
                tripStatus.setScheduleDeviation((int) (nextStop.getPredictedArrivalTime() - nextStop.getArrivalTime()));
            
        } else {
            int lastHop = timetable.getNumHops() - 1;
            tripStatus.setScheduleDeviation(timetable.getArrivalDelay(lastHop));
        }
        
        return tripStatus;
    }
    
    protected List<TransitStopTime> getStopTimesForTrip(String routerId, AgencyAndId tripId, ServiceDate serviceDate, TableTripPattern pattern, Timetable timetable) {

        List<TransitStopTime> stopTimes = new LinkedList<TransitStopTime>();

        long time = serviceDate.getAsDate(graph.getTimeZone()).getTime() / 1000;
        int tripIndex = pattern.getTripIndex(tripId);
        
        int numStops = pattern.getStops().size();
        for(int i = 0; i < numStops; ++i) {
            TransitStopTime stopTime = new TransitStopTime();
            stopTime.setStopId(pattern.getStops().get(i).getId().toString());

            TripTimes tripTimes = timetable.getTripTimes(tripIndex);
            TripTimes scheduledTripTimes = tripTimes.getScheduledTripTimes();
            
            if(i + 1 < numStops) {
                stopTime.setStopHeadsign(tripTimes.getHeadsign(i));
            
                if(!tripTimes.isScheduled())
                    stopTime.setPredictedDepartureTime(time + tripTimes.getDepartureTime(i));
                if(scheduledTripTimes != null)
                    stopTime.setDepartureTime(time + scheduledTripTimes.getDepartureTime(i));
            }
            if(i > 0) {
                if(!tripTimes.isScheduled())
                    stopTime.setPredictedArrivalTime(time + tripTimes.getArrivalTime(i - 1));
                if(scheduledTripTimes != null)
                    stopTime.setArrivalTime(time + scheduledTripTimes.getArrivalTime(i - 1));
            }
            
            // TODO: alerts?
            
            stopTimes.add(stopTime);
        }
        
        return stopTimes;
    }
    
    // TODO: return alights also
    protected List<T2<TransitScheduleStopTime, TransitTrip>> getStopTimesForStop(long startTime, long endTime, AgencyAndId stopId) {

        PreAlightEdge preAlightEdge = transitIndexService.getPreAlightEdge(stopId);
        PreBoardEdge preBoardEdge = transitIndexService.getPreBoardEdge(stopId);
        
        RoutingRequest options = makeTraverseOptions(startTime, routerId);
        List<T2<TransitScheduleStopTime, TransitTrip>> boardingTimes = getStopTimesForPreBoardEdge(stopId.toString(), startTime, endTime, options, preBoardEdge);
        //List<T2<TransitScheduleStopTime, TransitTrip>> alightingTimes = getStopTimesForPreAlightEdge(builder, stopId.toString(), startTime, endTime, options, preAlightEdge);
        //List<T2<TransitScheduleStopTime, TransitTrip>> mergedStopTimes = mergeStopTimes(boardingTimes, alightingTimes);
        
        return boardingTimes;
    }
    
    protected List<T2<TransitScheduleStopTime, TransitTrip>> getStopTimesForPreAlightEdge(String stopId, long startTime, long endTime,
            RoutingRequest options, PreAlightEdge edge) {
        
        List<T2<TransitScheduleStopTime, TransitTrip>> result = new ArrayList<T2<TransitScheduleStopTime, TransitTrip>>();
        for(Edge e : edge.getFromVertex().getIncoming()) {
            if(!(e instanceof TransitBoardAlight))
                continue;
            
            result.addAll(getStopTimesForTransitBoardAlightEdge(stopId, startTime, endTime, options, (TransitBoardAlight) e));
        }
        
        return result;
    }
    
    protected List<T2<TransitScheduleStopTime, TransitTrip>> getStopTimesForPreBoardEdge(String stopId, long startTime, long endTime,
            RoutingRequest options, PreBoardEdge edge) {
        
        List<T2<TransitScheduleStopTime, TransitTrip>> result = new ArrayList<T2<TransitScheduleStopTime, TransitTrip>>();
        for(Edge e : edge.getToVertex().getOutgoing()) {
            if(!(e instanceof TransitBoardAlight))
                continue;
            
            result.addAll(getStopTimesForTransitBoardAlightEdge(stopId, startTime, endTime, options, (TransitBoardAlight) e));
        }
        
        return result;
    }

    protected List<T2<TransitScheduleStopTime, TransitTrip>> getStopTimesForTransitBoardAlightEdge(String stopId, long startTime, long endTime,
            RoutingRequest options, TransitBoardAlight tba) {
        
        List<T2<TransitScheduleStopTime, TransitTrip>> out = new ArrayList<T2<TransitScheduleStopTime, TransitTrip>>();
        
        State result;
        long time = startTime;
        int stopIndex = tba.getStopIndex();
        int numStops = tba.getPattern().getStops().size();
        
        do {
            // TODO verify options/state correctness
            State s0 = new State(tba.getFromVertex(), time, options);
            result = tba.traverse(s0);
            if (result == null)
                break;
            time = result.getTimeSeconds();
            if (time > endTime)
                break;

            long midnight = result.getServiceDay().time(0);
            TripTimes tripTimes = result.getTripTimes();
            TripTimes scheduledTripTimes = tripTimes.getScheduledTripTimes();
            
            Trip trip = result.getBackTrip();
            TransitTrip transitTrip = responseBuilder.getTrip(trip);
            transitTrip.setWheelchairAccessible(tripTimes.isWheelchairAccessible());
            
            TransitScheduleStopTime stopTime = new TransitScheduleStopTime();
            stopTime.setTripId(transitTrip.getId());
            stopTime.setStopId(stopId);
            stopTime.setServiceDate(result.getServiceDay().getServiceDate().getAsString());
            
            Set<Alert> alerts = result.getBackAlerts();
            if(alerts != null && !alerts.isEmpty()) {
                List<String> alertIds = new LinkedList<String>();
                for(Alert alert : alerts) {
                    responseBuilder.addToReferences(alert);
                    alertIds.add(alert.alertId.toString());
                }
                stopTime.setAlertIds(alertIds);
            }
            
            if(stopIndex + 1 < numStops) {
                if(!tripTimes.isScheduled())
                    stopTime.setPredictedDepartureTime(midnight + tripTimes.getDepartureTime(stopIndex));
                if(scheduledTripTimes != null)
                    stopTime.setDepartureTime(midnight + scheduledTripTimes.getDepartureTime(stopIndex));
            }
            if(stopIndex > 0) {
                if(!tripTimes.isScheduled())
                    stopTime.setPredictedArrivalTime(midnight + tripTimes.getArrivalTime(stopIndex - 1));
                if(scheduledTripTimes != null)
                    stopTime.setArrivalTime(midnight + scheduledTripTimes.getArrivalTime(stopIndex - 1));
            }
            out.add(new T2<TransitScheduleStopTime, TransitTrip>(stopTime, transitTrip));
            
            responseBuilder.addToReferences(trip.getRoute());

            time += 1; // move to the next board time
        } while (true);
        
        return out;
    }    
    
    protected List<TransitVehicle> getTransitVehiclesForRoute(VehicleLocationService vehicleLocationService, AgencyAndId routeId) {
        List<VehicleLocation> vehicles = new ArrayList<VehicleLocation>(vehicleLocationService.getForRoute(routeId));

        List<TransitVehicle> transitVehicles = new ArrayList<TransitVehicle>(vehicles.size());
        for(VehicleLocation vehicle : vehicles) {
            TransitTrip transitTrip = null;
            
            if(vehicle.getTripId() != null) {
                Trip trip = getTrip(vehicle.getTripId(), vehicle.getServiceDate());
                TableTripPattern pattern = transitIndexService.getTripPatternForTrip(trip.getId());

                if(pattern != null && pattern.getTripIndex(trip.getId()) >= 0) { 
                    int tripIndex = pattern.getTripIndex(trip.getId());
                    Timetable timetable;
                    if(graph.getTimetableSnapshotSource() != null && graph.getTimetableSnapshotSource().getTimetableSnapshot() != null) {
                        timetable = graph.getTimetableSnapshotSource().getTimetableSnapshot().resolve(pattern, vehicle.getServiceDate());
                    } else {
                        timetable = pattern.getScheduledTimetable();
                    }

                    if(vehicle.getStopId() != null) {
                        Stop vehicleStop = transitIndexService.getAllStops().get(vehicle.getStopId());
                        responseBuilder.addToReferences(vehicleStop);
                    }

                    transitTrip = responseBuilder.getTrip(trip);
                    transitTrip.setWheelchairAccessible(timetable.isWheelchairAccessible(tripIndex));
                    responseBuilder.addToReferences(transitTrip);
                }
            }

            TransitVehicle transitVehicle = responseBuilder.getVehicle(vehicle);
            if(transitTrip != null)
                transitVehicle.setTripId(transitTrip.getId());
            transitVehicles.add(transitVehicle);
        }
        
        return transitVehicles;
    }

    protected List<String> getAlertsForStop(AgencyAndId stopId, RoutingRequest options, long startTime, long endTime) {
        
        Set<String> alertIds = new HashSet<String>();
        
        PatchService patchService = graph.getService(PatchService.class);
        if(patchService != null) {
            Collection<Patch> patches = patchService.getStopPatches(stopId);
            for(Patch patch : patches) {
                if(patch.activeDuring(options, startTime, endTime)) {
                    Alert alert = patch.getAlert();
                    if(alert != null) {
                        responseBuilder.addToReferences(alert);
                        alertIds.add(alert.alertId.toString());
                    }
                }
            }
        }
        
        return new ArrayList<String>(alertIds);
    }

    protected List<String> getAlertsForRoute(AgencyAndId routeId, RoutingRequest options, long startTime, long endTime) {
        
        Set<String> alertIds = new HashSet<String>();
        
        PatchService patchService = graph.getService(PatchService.class);
        if(patchService != null) {
            Collection<Patch> patches = patchService.getRoutePatches(routeId);
            for(Patch patch : patches) {
                if(patch.activeDuring(options, startTime, endTime)) {
                    Alert alert = patch.getAlert();
                    if(alert != null) {
                        responseBuilder.addToReferences(alert);
                        alertIds.add(alert.alertId.toString());
                    }
                }
            }
        }
        
        return new ArrayList<String>(alertIds);
    }
    
    protected List<String> getAlertsForTrip(AgencyAndId tripId, AgencyAndId routeId, RoutingRequest options, long startTime, long endTime) {
        return getAlertsForRoute(routeId, options, startTime, endTime); // TODO ~ return trip alerts
    }
    
    protected List<String> getNearbyStops(Stop stop) {
        List<TransitStop> nearbyTransitStops = graph.streetIndex.getNearbyTransitStops(new Coordinate(stop.getLat(), stop.getLon()), NEARBY_STOP_RADIUS);
        List<String> nearbyStopIds = new ArrayList<String>(nearbyTransitStops.size());
        
        for(TransitStop transitStop : nearbyTransitStops) {
            responseBuilder.addToReferences(transitStop.getStop());
            nearbyStopIds.add(transitStop.getStop().getId().toString());
        }
        
        return nearbyStopIds;
    }
    
    // -- -- -- | -- -- -- //
    
    private Graph getGraph(String routerId) {
        try {
            return graphService.getGraph(routerId);
        } catch(Exception e) {
            return null;
        }
    }
    
    protected AgencyAndId parseAgencyAndId(String fullId) {
        if(!fullId.contains("_"))
            return null;
        String[] parts = fullId.split("_", 2);
        return new AgencyAndId(parts[0], parts[1]);
    }

    protected Trip getTrip(AgencyAndId tripId, ServiceDate serviceDate) {
        TableTripPattern pattern = transitIndexService.getTripPatternForTrip(tripId);
        int tripIndex = pattern.getTripIndex(tripId);
        Trip trip = pattern.getTrip(tripIndex);
        return trip;
    }
    
    protected RoutingRequest makeTraverseOptions(long startTime, String routerId) {
        RoutingRequest options = new RoutingRequest();
        // if (graphService.getCalendarService() != null) {
        // options.setCalendarService(graphService.getCalendarService());
        // options.setServiceDays(startTime, agencies);
        // }
        // TODO: verify correctness
        options.dateTime = startTime;
        Collection<Vertex> vertices = graph.getVertices();
        Iterator<Vertex> it = vertices.iterator();
        options.setFromString(it.next().getLabel());
        options.setToString(it.next().getLabel());
        options.setRoutingContext(graph);
        return options;
    }

    protected List<RouteVariant> getReferenceVariantsForRoute(AgencyAndId routeId) {
        List<RouteVariant> routeVariants = transitIndexService.getVariantsForRoute(routeId);
        Iterable<RouteVariant> referenceVariants = Iterables.filter(routeVariants, new Predicate<RouteVariant>() {
            @Override
            public boolean apply(RouteVariant input) {
                boolean reference = false;
                for(TripsModelInfo tripsModelInfo : input.getTrips()) {
                    reference |= "REFERENCE".equals(tripsModelInfo.getCalendarId());
                }
                return reference;
            }
        });
        
        List<RouteVariant> filteredVariants = Lists.newArrayList(referenceVariants);
        if(filteredVariants.isEmpty()) {
            referenceVariants = Iterables.filter(routeVariants, new Predicate<RouteVariant>() {
                @Override
                public boolean apply(RouteVariant input) {
                    boolean reference = false;
                    for(TripsModelInfo tripsModelInfo : input.getTrips()) {
                        reference |= tripsModelInfo.isReference();
                    }
                    return reference;
                }
            });
            filteredVariants = Lists.newArrayList(referenceVariants);
        }
        
        return filteredVariants;
    }

    protected void sortStopTimesWithTrips(List<T2<TransitScheduleStopTime, TransitTrip>> stopTimes) {
        Collections.sort(stopTimes, new Comparator<T2<TransitScheduleStopTime, TransitTrip>>() {

            @Override
            public int compare(T2<TransitScheduleStopTime, TransitTrip> t2a, T2<TransitScheduleStopTime, TransitTrip> t2b) {
                TransitScheduleStopTime a = t2a.getFirst();
                TransitScheduleStopTime b = t2b.getFirst();
                
                return SCHDULE_COMPARATOR.compare(a, b);
            }
        });
    }

    protected void sortStopTimes(List<TransitScheduleStopTime> stopTimes) {
        Collections.sort(stopTimes, SCHDULE_COMPARATOR);
    }
    protected static class TransitScheduleStopTimeComparator implements Comparator<TransitScheduleStopTime> {
        @Override
        public int compare(TransitScheduleStopTime a, TransitScheduleStopTime b) {
            long ret;
            
            Long a_arrivalTime   = a.getArrivalTime();
            Long a_departureTime = a.getDepartureTime();
            
            Long b_arrivalTime   = b.getArrivalTime();
            Long b_departureTime = b.getDepartureTime();
            
            if(a.getPredictedDepartureTime() != null)
                a_departureTime = a.getPredictedDepartureTime();
            
            if(a.getPredictedArrivalTime() != null)
                a_arrivalTime = a.getPredictedArrivalTime();
            
            if(b.getPredictedDepartureTime() != null)
                b_departureTime = b.getPredictedDepartureTime();
            
            if(b.getPredictedArrivalTime() != null)
                b_arrivalTime = b.getPredictedArrivalTime();

            if(a_departureTime != null && b_departureTime != null) {
                ret = a_departureTime - b_departureTime;
                if(ret != 0)
                    return (int) ret;
            }

            if(a_arrivalTime != null && b_arrivalTime != null) {
                ret = a_arrivalTime - b_arrivalTime;
                if(ret != 0)
                    return (int) ret;
            }

            return a.getTripId().compareTo(b.getTripId());
        }
    }
    
    protected static TransitScheduleStopTimeComparator SCHDULE_COMPARATOR = new TransitScheduleStopTimeComparator();
}
