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

import com.vividsolutions.jts.geom.LineString;
import java.util.ArrayList;
import java.util.Collection;
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
import org.opentripplanner.api.ws.Response;
import org.opentripplanner.gtfs.GtfsLibrary;
import org.opentripplanner.routing.edgetype.TableTripPattern;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.patch.Alert;
import org.opentripplanner.routing.services.TransitIndexService;
import org.opentripplanner.routing.transit_index.RouteVariant;
import org.opentripplanner.updater.vehicle_location.VehicleLocation;
import org.opentripplanner.util.PolylineEncoder;

public class TransitResponseBuilder {
    private final static int API_VERSION = 2;
    
    public enum Dialect {
        otp, oba;
    };
    
    private Graph _graph;
    private Dialect _dialect;
    private TransitIndexService _transitIndexSerivce;
    private OTPTransitReferences _references = new OTPTransitReferences();
    
    private boolean _returnReferences;
    
    public TransitResponseBuilder(Graph graph, boolean references, Dialect dialect) {
        _graph = graph;
        _dialect = dialect;
        _returnReferences = references;
        _transitIndexSerivce = _graph.getService(TransitIndexService.class);
    }
    
    /* RESPONSE */
    
    public TransitResponse getResponseForMetadata(TransitMetadata metadata) {
        return getOkResponse(entity(metadata));
    }
    
    public TransitResponse getResponseForAgenciesWithCoverage(List<TransitAgencyWithCoverage> agenciesWithCoverage) {
        for(TransitAgencyWithCoverage agencyWithCoverage : agenciesWithCoverage) {
            addToReferences(_transitIndexSerivce.getAgency(agencyWithCoverage.getAgencyId()));
        }
        return getOkResponse(list(agenciesWithCoverage));
    }
    
    public TransitResponse getResponseForAgency(Agency agency) {
        return getOkResponse(entity(getAgency(agency)));
    }
    
    public TransitResponse getResponseForRoute(Route route) {
        return getOkResponse(entity(getRoute(route)));
    }
    
    public TransitResponse getResponseForRoutes(Collection<Route> routes) {
        return getOkResponse(list(getRoutes(routes)));
    }
    
    public TransitResponse getResponseForRoutes(List<TransitRoute> transitRoutes) {
        return getOkResponse(list(transitRoutes));
    }
    
    public TransitResponse getResponseForRoute(Route route, List<RouteVariant> variants, List<String> alertIds) {
        return getOkResponse(entity(getRoute(route, variants, alertIds)));
    }
    
    public TransitResponse getResponseForStopsForRoute(Route route, List<RouteVariant> variants, List<String> alertIds,
            boolean includePolylines) {
        
        return getOkResponse(entity(getStopsForRoute(route, variants, alertIds, includePolylines)));
    }
    
    public TransitResponse getResponseForTrip(Trip trip) {
        return getOkResponse(entity(getTrip(trip)));
    }
    
    public TransitResponse getResponseForTrip(TransitTripDetails tripDetails) {
        return getOkResponse(entity(tripDetails));
    }
    
    public TransitResponse getResponseForTrip(TransitTrip trip, ServiceDate serviceDate, List<String> alertIds,
            List<Stop> stops, List<TransitStopTime> stopTimes, TransitVehicle vehicle, RouteVariant variant) {
        
        return getOkResponse(entity(getTrip(trip, serviceDate, alertIds, stops, stopTimes, vehicle, variant)));
    }
    
    public TransitResponse getResponseForStop(Stop stop) {
        return getOkResponse(entity(getStop(stop)));
    }
    
    public TransitResponse getResponseForStop(Stop stop, List<TransitScheduleStopTime> stopTimes,
            List<String> alertIds, List<TransitTrip> trips, List<String> nearbyStopIds) {
        
        return getOkResponse(entity(getArrivalsAndDepartures(stop, stopTimes, alertIds, trips, nearbyStopIds)));
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
    
    public TransitResponse getResponseForVehicle(VehicleLocation vehicle) {
        return getOkResponse(entity(getVehicle(vehicle)));
    }
    
    public TransitResponse getResponseForSearch(String query, List<String> alertIds) {
        return getOkResponse(entity(getSearch(query, alertIds)));
    }
    
    public TransitResponse getResponseForSearch(String query, List<String> stopIds, List<String> routeIds, List<String> alertIds) {
        return getOkResponse(entity(getSearch(query, stopIds, routeIds, alertIds)));
    }

    public TransitResponse getResponseForTripPlan(Response plan) {
        return getOkResponse(entity(plan));
    }

    public TransitResponse getResponseForErrorTripPlan(TransitResponse.Status status, Response plan) {
        return getFailResponse(status, plan.getError().getMsg(), entity(plan));
    }

    public TransitResponse getResponseForList(List<?> list) {
        return getOkResponse(list(list));
    }
    
    private TransitResponse getOkResponse(Object data) {
        return new TransitResponse(API_VERSION, TransitResponse.Status.OK, "OK", data);
    }
    
    public static TransitResponse getFailResponse() {
        return getFailResponse(TransitResponse.Status.UNKNOWN_ERROR, "An unknown error occured...");
    }
    
    public static TransitResponse getFailResponse(String text) {
        return new TransitResponse(API_VERSION, TransitResponse.Status.UNKNOWN_ERROR, text, null);
    }
    
    public static TransitResponse getFailResponse(TransitResponse.Status status) {
        return new TransitResponse(API_VERSION, status, null, null);
    }
    
    public static TransitResponse getFailResponse(TransitResponse.Status status, String text) {
        return new TransitResponse(API_VERSION, status, text, null);
    }
    
    public static TransitResponse getFailResponse(TransitResponse.Status status, String text, Object data) {
        return new TransitResponse(API_VERSION, status, text, data);
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
        transitVariant.setHeadsign(variant.getHeadsign());
        transitVariant.setDirection(variant.getDirection());
        transitVariant.setGeometry(PolylineEncoder.createEncodings(variant.getGeometry()).getPoints());
        transitVariant.setStopIds(stopIds);
        
        return transitVariant;
    }
    
    public TransitTripDetailsOTP getTrip(TransitTrip trip, ServiceDate serviceDate, List<String> alertIds,
            List<Stop> stops, List<TransitStopTime> stopTimes, TransitVehicle vehicle, RouteVariant variant) {
        
        addToReferences(trip);
        
        for(Stop stop : stops)
            addToReferences(stop);
        
        TransitTripDetailsOTP transitTripDetails = new TransitTripDetailsOTP();
        transitTripDetails.setVehicle(vehicle);
        transitTripDetails.setAlertIds(alertIds);
        transitTripDetails.setTripId(trip.getId());
        transitTripDetails.setStopTimes(stopTimes);
        transitTripDetails.setServiceDate(serviceDate.getAsString());
        transitTripDetails.setGeometry(PolylineEncoder.createEncodings(variant.getGeometry()).getPoints());
        
        return transitTripDetails;
    }
    
    public TransitArrivalsAndDepartures getArrivalsAndDepartures(Stop stop,
            List<TransitScheduleStopTime> stopTimes, List<String> alertIds, List<TransitTrip> trips, List<String> nearbyStopIds) {
        
        TransitArrivalsAndDepartures tad = new TransitArrivalsAndDepartures();
        tad.setStopId(stop.getId().toString());
        tad.setStopTimes(stopTimes);
        tad.setAlertIds(alertIds);
        tad.setNearbyStopIds(nearbyStopIds);
        addToReferences(stop);
        if(trips != null) {
            for(TransitTrip trip : trips) {
                addToReferences(trip);
            }
        }
        return tad;
    }

    public TransitVehicle getVehicle(VehicleLocation vehicle) {
        TransitVehicle transitVehicle = new TransitVehicle();
        transitVehicle.setVehicleId(vehicle.getVehicleId().toString());
        transitVehicle.setStatus(vehicle.getStatus());
        transitVehicle.setBearing(vehicle.getBearing());
        transitVehicle.setLocation(new TransitPoint(vehicle.getLatitude(), vehicle.getLongitude()));
        transitVehicle.setLastUpdateTime(vehicle.getTimestamp());
        transitVehicle.setLicensePlate(vehicle.getLicensePlate());
        transitVehicle.setLabel(vehicle.getLabel());
        transitVehicle.setDeviated(vehicle.isDeviated());
        transitVehicle.setServiceDate(vehicle.getServiceDate().getAsString());
        transitVehicle.setCongestionLevel(vehicle.getCongestionLevel());
        
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

    public TransitSearch getSearch(String query, List<String> alertIds) {
        TransitSearch transitSearch = new TransitSearch();
        transitSearch.setQuery(query);
        transitSearch.setAlertIds(alertIds);
        
        return transitSearch;
    }

    public TransitSearch getSearch(String query, List<String> stopIds, List<String> routeIds, List<String> alertIds) {
        TransitSearch transitSearch = new TransitSearch();
        transitSearch.setQuery(query);
        transitSearch.setStopIds(stopIds);
        transitSearch.setRouteIds(routeIds);
        transitSearch.setAlertIds(alertIds);
        
        return transitSearch;
    }
    
    public List<TransitRoute> getRoutes(Collection<Route> routes) {
        List<TransitRoute> transitRoutes = new ArrayList<TransitRoute>(routes.size());
        
        for(Route route : routes) {
            transitRoutes.add(getRoute(route));
            addToReferences(route.getAgency());
        }
        
        return transitRoutes;
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
        if(_dialect == Dialect.otp) {
            transitRoute.setType(GtfsLibrary.getTraverseMode(route));
        } else {
            transitRoute.setType(route.getType());
        }
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
    
    public TransitStopsForRoute getStopsForRoute(Route route, List<RouteVariant> variants, List<String> alertIds, boolean includePolylines) {

        List<TransitPolyline> polylines = new LinkedList<TransitPolyline>();
        List<String> stopIds = new LinkedList<String>();
        
        List<TransitStopGroup> stopGroups = new LinkedList<TransitStopGroup>();
        for(RouteVariant variant : variants) {
            
            TransitStopGroupName stopGroupName = new TransitStopGroupName();
            stopGroupName.setType("destination");
            stopGroupName.setName(variant.getHeadsign());
            stopGroupName.setNames(Collections.singletonList(variant.getHeadsign()));
            
            List<String> stopGroupStopIds = new LinkedList<String>();
            for(Stop stop : variant.getStops()) {
                addToReferences(stop);
                stopGroupStopIds.add(stop.getId().toString());
            }
            stopIds.addAll(stopGroupStopIds);
            
            TransitPolyline variantPolyline = getPolyline(variant.getGeometry());
            polylines.add(variantPolyline);
            
            List<TransitPolyline> stopGroupPolylines = new LinkedList<TransitPolyline>();
            stopGroupPolylines.add(variantPolyline);
            
            TransitStopGroup stopGroup = new TransitStopGroup();
            stopGroup.setId("" + variant.getId());
            stopGroup.setName(stopGroupName);
            stopGroup.setStopIds(stopGroupStopIds);
            if(includePolylines)
                stopGroup.setPolylines(stopGroupPolylines);
            stopGroup.setSubGroups(Collections.emptyList());
            
            stopGroups.add(stopGroup);
        }
        
        TransitStopGrouping stopGrouping = new TransitStopGrouping();
        stopGrouping.setStopGroups(stopGroups);
        stopGrouping.setType("direction");
        stopGrouping.setOrdered(true);
        
        List<TransitStopGrouping> stopGroupings = new ArrayList<TransitStopGrouping>(variants.size());
        stopGroupings.add(stopGrouping);
        
        TransitStopsForRoute stopsForRoute = new TransitStopsForRoute();
        stopsForRoute.setRouteId(route.getId().toString());
        stopsForRoute.setStopIds(stopIds);
        if(includePolylines)
            stopsForRoute.setPolylines(polylines);
        stopsForRoute.setSituationIds(alertIds);
        stopsForRoute.setStopGroupings(stopGroupings);
        
        addToReferences(route);
        return stopsForRoute;
    }
    
    public TransitPolyline getPolyline(LineString linestring) {
        TransitPolyline polyline = new TransitPolyline();
        polyline.setLevels("");
        polyline.setLength(linestring.getNumPoints());
        polyline.setPoints(PolylineEncoder.createEncodings(linestring).getPoints());
        return polyline;
    }
    
    public TransitTrip getTrip(Trip trip) {
        
        addToReferences(trip.getRoute());
        
        TransitTrip transitTrip = new TransitTrip();
        transitTrip.setServiceId(trip.getServiceId().toString());
        transitTrip.setBlockId(trip.getBlockId());
        transitTrip.setShapeId(trip.getShapeId().toString());
        transitTrip.setTripHeadsign(trip.getTripHeadsign());
        transitTrip.setDirectionId(trip.getDirectionId());
        transitTrip.setId(trip.getId().toString());
        transitTrip.setRouteId(trip.getRoute().getId().toString());
        transitTrip.setTripShortName(trip.getTripShortName());
        transitTrip.setWheelchairAccessible(trip.getWheelchairAccessible() == TableTripPattern.FLAG_WHEELCHAIR_ACCESSIBLE);
        
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
        transitStop.setDescription(stop.getDesc());
        transitStop.setDirection(stop.getDirection());
        transitStop.setLocationType(stop.getLocationType());
        transitStop.setWheelchairBoarding(1 == stop.getWheelchairBoarding());
        transitStop.setType(_transitIndexSerivce.getModeForStop(stop.getId()));
        transitStop.setRouteIds(routeIds);
        
        if(_dialect == Dialect.oba) {
            if(stop.getDirection() != null) {
                transitStop.setDirection(getAngleAsDirection(Double.parseDouble(stop.getDirection())));
            }
        }
        
        return transitStop;
    }
    
    public TransitAlert getAlert(Alert alert) {
        
        TransitAlert transitAlert = new TransitAlert();
        transitAlert.setId(alert.alertId.toString());
        transitAlert.setUrl(alert.alertUrl);
        transitAlert.setHeader(alert.alertHeaderText);
        transitAlert.setDescription(alert.alertDescriptionText);
        
        if(alert.effectiveStartDate != null)
            transitAlert.setStart(alert.effectiveStartDate.getTime() / 1000);
        if(alert.effectiveEndDate != null)
            transitAlert.setEnd(alert.effectiveEndDate.getTime() / 1000);
        
        if(alert.stopIds != null) {
            List<String> stopIds = new LinkedList<String>();
            for(AgencyAndId stopId : alert.stopIds) {
                Stop stop = _transitIndexSerivce.getAllStops().get(stopId);
                if(stop != null) {
                    addToReferences(stop);
                    stopIds.add(stopId.toString());
                }
            }
            transitAlert.setStopIds(stopIds);
        }
        
        if(alert.routeIds != null) {
            List<Route> routes = new ArrayList<Route>(alert.routeIds.size());
            for(AgencyAndId routeId : alert.routeIds) {
                Route route = _transitIndexSerivce.getAllRoutes().get(routeId);
                if(route != null)
                    routes.add(route);
            }
            Collections.sort(routes, ROUTE_COMPARATOR);
            
            List<String> routeIds = new ArrayList<String>(alert.routeIds.size());
            for(Route route : routes) {
                addToReferences(route);
                routeIds.add(route.getId().toString());
            }
            transitAlert.setRouteIds(routeIds);
        }
        
        return transitAlert;
    }
    
    /* HELPERS */
    
    public <T> TransitEntryWithReferences<T> entity(T entry) {
        if(_returnReferences)
            return new TransitEntryWithReferences<T>(entry, getDialectReferences());
        else
            return new TransitEntryWithReferences<T>(entry, null);
    }
    
    public <T> TransitListEntryWithReferences<T> list(List<T> entry) {
        if(_returnReferences)
            return new TransitListEntryWithReferences<T>(entry, getDialectReferences());
        else
            return new TransitListEntryWithReferences<T>(entry, null);
    }
    
    private TransitReferences getDialectReferences() {
        if(_dialect == Dialect.otp) {
            return _references;
        }
        
        return new OBATransitReferences(_references);
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
    
    private String getAngleAsDirection(double theta) {
        double t = 360 / 8;

        int r = (int) Math.floor((theta + t / 2) / t);

        switch (r) {
            case 0:
                return "N";
            case 1:
                return "NE";
            case 2:
                return "E";
            case 3:
                return "SE";
            case 4:
                return "S";
            case -1:
                return "NW";
            case -2:
                return "W";
            case -3:
                return "SW";
            case -4:
                return "S";
            default:
                return "?";
        }
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
            if(a == 1) return -1;
            if(b == 1) return  1;
            
            // hév
            if(a == 2) return -1;
            if(b == 2) return  1;
            
            // hajó
            if(a == 4) return -1;
            if(b == 4) return  1;
            
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
                } else {
                    if(ma.group(1).length() > 0) return  1;
                    if(mb.group(1).length() > 0) return -1;
                }
                
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
