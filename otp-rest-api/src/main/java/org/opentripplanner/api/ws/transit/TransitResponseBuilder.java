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

package org.opentripplanner.api.ws.transit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.onebusaway.gtfs.model.Agency;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Route;
import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.model.Trip;
import org.onebusaway.gtfs.model.calendar.ServiceDate;
import org.opentripplanner.gtfs.GtfsLibrary;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.patch.Alert;
import org.opentripplanner.routing.services.TransitIndexService;
import org.opentripplanner.routing.transit_index.RouteVariant;
import org.opentripplanner.routing.trippattern.TripTimes;
import org.opentripplanner.updater.vehicle_location.VehicleLocation;
import org.opentripplanner.util.PolylineEncoder;

public class TransitResponseBuilder {

    private Graph _graph;
    private TransitIndexService _transitIndexSerivce;
    private TransitReferences _references = new TransitReferences();
    
    public TransitResponseBuilder(Graph graph) {
        _graph = graph;
        _transitIndexSerivce = _graph.getService(TransitIndexService.class);
    }
    
    /* RESPONSE */
    
    public static TransitResponse getResponseForAgency(Graph graph, Agency agency) {
        TransitResponseBuilder factory = new TransitResponseBuilder(graph);
        return factory.getResponseForAgency(agency);
    }
    
    public TransitResponse getResponseForAgency(Agency agency) {
        return getOkResponse(entity(getAgency(agency)));
    }
    
    public static TransitResponse getResponseForRoute(Graph graph, Route route) {
        TransitResponseBuilder factory = new TransitResponseBuilder(graph);
        return factory.getResponseForRoute(route);
    }
    
    public TransitResponse getResponseForRoute(Route route) {
        return getOkResponse(entity(getRoute(route)));
    }
    
    public TransitResponse getResponseForRoutes(List<TransitRoute> transitRoutes) {
        return getOkResponse(list(transitRoutes));
    }
    
    public static TransitResponse getResponseForRoute(Graph graph, Route route, List<RouteVariant> variants, List<String> alertIds) {
        TransitResponseBuilder factory = new TransitResponseBuilder(graph);
        return factory.getResponseForRoute(route, variants, alertIds);
    }
    
    public TransitResponse getResponseForRoute(Route route, List<RouteVariant> variants, List<String> alertIds) {
        return getOkResponse(entity(getRoute(route, variants, alertIds)));
    }
    
    public static TransitResponse getResponseForTrip(Graph graph, Trip trip) {
        TransitResponseBuilder factory = new TransitResponseBuilder(graph);
        return factory.getResponseForTrip(trip);
    }
    
    public TransitResponse getResponseForTrip(Trip trip) {
        return getOkResponse(entity(getTrip(trip)));
    }
    
    public TransitResponse getResponseForTrip(TransitTrip trip, ServiceDate serviceDate, List<String> alertIds,
            List<Stop> stops, List<TransitStopTime> stopTimes, TransitVehicle vehicle, RouteVariant variant) {
        
        return getOkResponse(entity(getTrip(trip, serviceDate, alertIds, stops, stopTimes, vehicle, variant)));
    }
    
    public static TransitResponse getResponseForStop(Graph graph, Stop stop) {
        TransitResponseBuilder factory = new TransitResponseBuilder(graph);
        return factory.getResponseForStop(stop);
    }
    
    public TransitResponse getResponseForStop(Stop stop) {
        return getOkResponse(entity(getStop(stop)));
    }
    
    public TransitResponse getResponseForStop(Stop stop, List<TransitScheduleStopTime> stopTimes,
            List<String> alertIds, List<TransitTrip> trips) {
        
        return getOkResponse(entity(getArrivalsAndDepartures(stop, stopTimes, alertIds, trips)));
    }
    
    public static TransitResponse getResponseForStops(Graph graph, List<Stop> stops) {
        TransitResponseBuilder factory = new TransitResponseBuilder(graph);
        return factory.getResponseForStops(stops);
    }
    
    public TransitResponse getResponseForStops(List<Stop> stops) {
        List<TransitStop> transitStops = new ArrayList<TransitStop>(stops.size());
        for(Stop stop : stops) {
            TransitStop transitStop = getStop(stop);
            transitStops.add(transitStop);
        }
        return getOkResponse(list(transitStops));
    }
    
    public TransitResponse getResponseForStopSchedule(Stop stop, TransitSchedule schedule) {
        addToReferences(stop);
        return getOkResponse(entity(schedule));
    }
    
    public static TransitResponse getResponseForVehicle(Graph graph, VehicleLocation vehicle) {
        TransitResponseBuilder factory = new TransitResponseBuilder(graph);
        return factory.getResponseForVehicle(vehicle);
    }
    
    public TransitResponse getResponseForVehicle(VehicleLocation vehicle) {
        return getOkResponse(entity(getVehicle(vehicle)));
    }

    public TransitResponse getResponseForVehicles(List<TransitVehicle> transitVehicles) {
        return getOkResponse(list(transitVehicles));
    }
    
    public static TransitResponse getFailResponse() {
        return getFailResponse("ERROR");
    }
    
    public static TransitResponse getFailResponse(String error) {
        return new TransitResponse(1, 500, error, null);
    }
    
    /* SUB-RESPONSE */

    public TransitRouteVariant getTransitVariant(RouteVariant variant) {
        
        List<String> stopIds = new ArrayList<String>(variant.getStops().size());
        for(Stop stop : variant.getStops()) {
            addToReferences(stop);
            stopIds.add(stop.getId().toString());
        }
        
        TransitRouteVariant transitVariant = new TransitRouteVariant();
        transitVariant.setName(variant.getName());
        transitVariant.setDirection(variant.getDirection());
        transitVariant.setGeometry(PolylineEncoder.createEncodings(variant.getGeometry()).getPoints());
        transitVariant.setStopIds(stopIds);
        
        return transitVariant;
    }
    
    public TransitTripDetails getTrip(TransitTrip trip, ServiceDate serviceDate, List<String> alertIds,
            List<Stop> stops, List<TransitStopTime> stopTimes, TransitVehicle vehicle, RouteVariant variant) {
        
        addToReferences(trip);
        
        for(Stop stop : stops)
            addToReferences(stop);
        
        TransitTripDetails transitTripDetails = new TransitTripDetails();
        transitTripDetails.setVehicle(vehicle);
        transitTripDetails.setAlertIds(alertIds);
        transitTripDetails.setTripId(trip.getId());
        transitTripDetails.setStopTimes(stopTimes);
        transitTripDetails.setServiceDate(serviceDate.getAsString());
        transitTripDetails.setGeometry(PolylineEncoder.createEncodings(variant.getGeometry()).getPoints());
        
        return transitTripDetails;
    }
    
    public TransitArrivalsAndDepartures getArrivalsAndDepartures(Stop stop,
            List<TransitScheduleStopTime> stopTimes, List<String> alertIds, List<TransitTrip> trips) {
        
        TransitArrivalsAndDepartures tad = new TransitArrivalsAndDepartures();
        tad.setStopId(stop.getId().toString());
        tad.setStopTimes(stopTimes);
        tad.setAlertIds(alertIds);
        addToReferences(stop);
        for(TransitTrip trip : trips) {
            addToReferences(trip);
        }
        return tad;
    }

    public TransitVehicle getVehicle(VehicleLocation vehicle) {
        TransitVehicle transitVehicle = new TransitVehicle();
        transitVehicle.setId(vehicle.getVehicleId().toString());
        transitVehicle.setBearing(vehicle.getBearing());
        transitVehicle.setLatitude(vehicle.getLatitude());
        transitVehicle.setLongitude(vehicle.getLongitude());
        transitVehicle.setTimestamp(vehicle.getTimestamp());
        transitVehicle.setLicensePlate(vehicle.getLicensePlate());
        transitVehicle.setServiceDate(vehicle.getServiceDate().getAsString());
        
        if(vehicle.getRouteId() != null) {
            Route route = _transitIndexSerivce.getAllRoutes().get(vehicle.getRouteId());
            if(route != null) {
                transitVehicle.setRouteId(route.getId().toString());
                addToReferences(route);
            }
        }
        
        if(vehicle.getStopId() != null) {
            Stop stop = _transitIndexSerivce.getAllStops().get(vehicle.getStopId());
            if(stop != null) {
                transitVehicle.setStopId(stop.getId().toString());
                addToReferences(stop);
            }
        }
        
        if(vehicle.getTripId() != null)
            transitVehicle.setTripId(vehicle.getTripId().toString());
        
        return transitVehicle;
    }
    
    /* BASE ENTITIES */
    
    public TransitAgency getAgency(Agency agency) {
        TransitAgency transitAgency = new TransitAgency();
        transitAgency.setId(agency.getId());
        transitAgency.setLang(agency.getLang());
        transitAgency.setName(agency.getName());
        transitAgency.setPhone(agency.getPhone());
        transitAgency.setTimezone(agency.getTimezone());
        transitAgency.setUrl(agency.getUrl());
        return transitAgency;
    }
    
    public TransitRoute getRoute(Route route) {
        TransitRoute transitRoute = new TransitRoute();
        transitRoute.setAgencyId(route.getAgency().getId());
        transitRoute.setColor(route.getColor());
        transitRoute.setDescription(route.getDesc());
        transitRoute.setId(route.getId().toString());
        transitRoute.setLongName(route.getLongName());
        transitRoute.setShortName(route.getShortName());
        transitRoute.setTextColor(route.getTextColor());
        transitRoute.setType(GtfsLibrary.getTraverseMode(route));
        transitRoute.setUrl(route.getUrl());
        
        addToReferences(route.getAgency());
        return transitRoute;
    }
    
    public TransitRoute getRoute(Route route, List<RouteVariant> variants, List<String> alertIds) {
        
        List<TransitRouteVariant> transitVariants = new ArrayList<TransitRouteVariant>(variants.size());
        for(RouteVariant variant : variants) {
            TransitRouteVariant transitVariant = getTransitVariant(variant);
            transitVariants.add(transitVariant);
        }
        
        TransitRouteDetails transitRoute = new TransitRouteDetails();
        transitRoute.setAgencyId(route.getAgency().getId());
        transitRoute.setColor(route.getColor());
        transitRoute.setDescription(route.getDesc());
        transitRoute.setId(route.getId().toString());
        transitRoute.setLongName(route.getLongName());
        transitRoute.setShortName(route.getShortName());
        transitRoute.setTextColor(route.getTextColor());
        transitRoute.setType(GtfsLibrary.getTraverseMode(route));
        transitRoute.setUrl(route.getUrl());
        transitRoute.setVariants(transitVariants);
        transitRoute.setAlertIds(alertIds);
        
        addToReferences(route.getAgency());
        return transitRoute;
    }
    
    public TransitTrip getTrip(Trip trip) {
        
        addToReferences(trip.getRoute());
        
        TransitTrip transitTrip = new TransitTrip();
        transitTrip.setHeadsign(trip.getTripHeadsign());
        transitTrip.setDirectionId(trip.getDirectionId());
        transitTrip.setId(trip.getId().toString());
        transitTrip.setRouteId(trip.getRoute().getId().toString());
        transitTrip.setShortName(trip.getTripShortName());
        transitTrip.setWheelchairAccessible(trip.getWheelchairAccessible() == TripTimes.WHEELCHAIR_ACCESSIBLE);
        
        return transitTrip;
    }
    
    public TransitStop getStop(Stop stop) {
        
        List<Route> routes = new ArrayList<Route>();
        for(AgencyAndId routeId : _transitIndexSerivce.getRoutesForStop(stop.getId())) {
            Route route = _transitIndexSerivce.getAllRoutes().get(routeId);
            routes.add(route);
        }
        Collections.sort(routes, ROUTE_COMPARATOR);
        
        List<String> routeIds = new LinkedList<String>();
        for(Route route : routes) {
            routeIds.add(route.getId().toString());
            addToReferences(route);
        }
        
        TransitStop transitStop = new TransitStop();
        transitStop.setId(stop.getId().toString());
        transitStop.setCode(stop.getCode());
        transitStop.setName(stop.getName());
        transitStop.setLat(stop.getLat());
        transitStop.setLon(stop.getLon());
        transitStop.setDirection(stop.getDirection());
        transitStop.setWheelchairBoarding(1 == stop.getWheelchairBoarding());
        transitStop.setType(_transitIndexSerivce.getModeForStop(stop.getId()));
        transitStop.setRouteIds(routeIds);
        
        return transitStop;
    }
    
    public TransitAlert getAlert(Alert alert) {
        
        TransitAlert transitAlert = new TransitAlert();
        transitAlert.setId(alert.alertId.toString());
        transitAlert.setUrl(alert.alertUrl);
        transitAlert.setHeader(alert.alertHeaderText);
        transitAlert.setDescription(alert.alertDescriptionText);
        
        return transitAlert;
    }
    
    /* HELPERS */
    
    private TransitResponse getOkResponse(Object data) {
        return new TransitResponse(1, 200, "OK", data);
    }
    
    private <T> TransitEntryWithReferences<T> entity(T entry) {
        return new TransitEntryWithReferences<T>(entry, _references);
    }
    
    private <T> TransitListEntryWithReferences<T> list(List<T> entry) {
        return new TransitListEntryWithReferences<T>(entry, _references);
    }
    
    /* REFERENCES */
    
    public void addToReferences(Agency agency) {
        if(_references.getAgencies().containsKey(agency.getId().toString())) {
            return;
        }
        
        TransitAgency transitAgency = getAgency(agency);
        _references.addAgency(transitAgency);
    }

    public void addToReferences(Route route) {
        if(_references.getRoutes().containsKey(route.getId().toString())) {
            return;
        }
        
        TransitRoute transitRoute = getRoute(route);
        _references.addRoute(transitRoute);
    }

    public void addToReferences(Trip trip) {
        if(_references.getTrips().containsKey(trip.getId().toString())) {
            return;
        }
        
        TransitTrip transitTrip = getTrip(trip);
        _references.addTrip(transitTrip);
    }

    public void addToReferences(TransitTrip transitTrip) {
        if(_references.getTrips().containsKey(transitTrip.getId())) {
            return;
        }
        
        _references.addTrip(transitTrip);
    }

    public void addToReferences(Stop stop) {
        if(_references.getStops().containsKey(stop.getId().toString())) {
            return;
        }
        
        TransitStop transitStop = getStop(stop);
        _references.addStop(transitStop);
    }

    public void addToReferences(TransitStop transitStop) {
        if(_references.getStops().containsKey(transitStop.getId())) {
            return;
        }
        
        _references.addStop(transitStop);
    }

    public void addToReferences(Alert alert) {
        if(_references.getAlerts().containsKey(alert.alertId.toString())) {
            return;
        }
        
        TransitAlert transitAlert = getAlert(alert);
        _references.addAlert(transitAlert);
    }

    public final static RouteComparator ROUTE_COMPARATOR = new RouteComparator();
    public final static class RouteComparator implements Comparator<Route> {

        @Override
        public int compare(Route a, Route b) {
            int ret = 0;
            
            if(a.getType() != b.getType())
                ret = this.compareRouteType(a.getType(), b.getType());
            
            if(ret != 0)
                return ret;
            
            return this.compareRouteShortName(a.getShortName(), b.getShortName());
        }

        private int compareRouteType(int a, int b) {
            // metró
            if(a == 1) return  1;
            if(b == 1) return -1;
            
            // hév
            if(a == 2) return  1;
            if(b == 2) return -1;
            
            // hajó
            if(a == 4) return  1;
            if(b == 4) return -1;
            
            // többi egyenlő ~ szám alapján rendeződnek
            return 0;
        }

        private Pattern pattern = Pattern.compile("^(\\D*)(\\d+)");
        
        private int compareRouteShortName(String a, String b) {
            
            Matcher ma = pattern.matcher(a);
            Matcher mb = pattern.matcher(b);
            
            if(ma.find() && mb.find()) {
                int ret;
                if(ma.group(1).length() > 0 && ma.group(1).length() > 0) {
                    ret = ma.group(1).compareTo(mb.group(1));
                    if(ret != 0)
                        return ret;
                }
                
                if(ma.group(1).length() > 0) return  1;
                if(mb.group(1).length() > 0) return -1;
                
                int na = Integer.parseInt(ma.group(2));
                int nb = Integer.parseInt(mb.group(2));
            
                ret = na - nb;
                if(ret != 0)
                    return ret;
            }
            
            return a.compareTo(b);
        }
    }
}
