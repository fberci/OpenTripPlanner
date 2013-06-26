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
import java.util.LinkedList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
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
        
        List<String> routeIds = new LinkedList<String>();
        for(AgencyAndId routeId : _transitIndexSerivce.getRoutesForStop(stop.getId())) {
            Route route = _transitIndexSerivce.getAllRoutes().get(routeId);
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
}