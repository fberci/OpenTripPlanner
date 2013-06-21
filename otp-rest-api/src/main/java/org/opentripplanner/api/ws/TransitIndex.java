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
package org.opentripplanner.api.ws;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.xml.bind.annotation.XmlRootElement;

import lombok.Setter;

import org.codehaus.jettison.json.JSONException;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Route;
import org.onebusaway.gtfs.model.ServiceCalendar;
import org.onebusaway.gtfs.model.ServiceCalendarDate;
import org.onebusaway.gtfs.model.Stop;
import org.opentripplanner.api.model.error.TransitError;
import org.opentripplanner.api.model.transit.AgencyList;
import org.opentripplanner.api.model.transit.CalendarData;
import org.opentripplanner.api.model.transit.ModeList;
import org.opentripplanner.api.model.transit.RouteData;
import org.opentripplanner.api.model.transit.RouteDataList;
import org.opentripplanner.api.model.transit.RouteList;
import org.opentripplanner.api.model.transit.ServiceCalendarData;
import org.opentripplanner.api.model.transit.StopList;
import org.opentripplanner.api.model.transit.StopTime;
import org.opentripplanner.api.model.transit.StopTimeList;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.services.GraphService;
import org.opentripplanner.routing.services.StreetVertexIndexService;
import org.opentripplanner.routing.services.TransitIndexService;
import org.opentripplanner.routing.transit_index.RouteSegment;
import org.opentripplanner.routing.transit_index.RouteVariant;
import org.opentripplanner.routing.transit_index.adapters.RouteType;
import org.opentripplanner.routing.transit_index.adapters.ServiceCalendarDateType;
import org.opentripplanner.routing.transit_index.adapters.ServiceCalendarType;
import org.opentripplanner.routing.transit_index.adapters.StopType;
import org.opentripplanner.routing.transit_index.adapters.TripType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jersey.api.core.InjectParam;
import com.sun.jersey.api.spring.Autowire;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TimeZone;
import java.util.TreeMap;
import javax.ws.rs.DefaultValue;
import org.onebusaway.gtfs.model.Trip;
import org.onebusaway.gtfs.model.calendar.ServiceDate;
import org.onebusaway.gtfs.services.calendar.CalendarService;
import org.opentripplanner.api.ws.transit.TransitAlert;
import org.opentripplanner.api.ws.transit.TransitResponse;
import org.opentripplanner.api.ws.transit.TransitResponseBuilder;
import org.opentripplanner.api.ws.transit.TransitRoute;
import org.opentripplanner.api.ws.transit.TransitRouteSchedule;
import org.opentripplanner.api.ws.transit.TransitRouteScheduleForDirection;
import org.opentripplanner.api.ws.transit.TransitSchedule;
import org.opentripplanner.api.ws.transit.TransitScheduleGroup;
import org.opentripplanner.api.ws.transit.TransitScheduleStopTime;
import org.opentripplanner.api.ws.transit.TransitStop;
import org.opentripplanner.api.ws.transit.TransitStopTime;
import org.opentripplanner.api.ws.transit.TransitTrip;
import org.opentripplanner.api.ws.transit.TransitVehicle;
import org.opentripplanner.common.model.T2;
import org.opentripplanner.routing.core.ServiceDay;
import org.opentripplanner.routing.edgetype.PreAlightEdge;
import org.opentripplanner.routing.edgetype.PreBoardEdge;
import org.opentripplanner.routing.edgetype.TableTripPattern;
import org.opentripplanner.routing.edgetype.Timetable;
import org.opentripplanner.routing.edgetype.TransitBoardAlight;
import org.opentripplanner.routing.patch.Alert;
import org.opentripplanner.routing.patch.Patch;
import org.opentripplanner.routing.services.PatchService;
import org.opentripplanner.routing.trippattern.TripTimes;
import org.opentripplanner.updater.vehicle_location.VehicleLocation;
import org.opentripplanner.updater.vehicle_location.VehicleLocationService;
import org.opentripplanner.util.MapUtils;

// NOTE - /ws/transit is the full path -- see web.xml

@Path("/transit")
@XmlRootElement
@Autowire
public class TransitIndex {

    private static final Logger LOG = LoggerFactory.getLogger(TransitIndex.class);

    private static final double STOP_SEARCH_RADIUS = 200;

    @Setter @InjectParam 
    private GraphService graphService;

    @Setter @InjectParam
    private PatchService patchService;
    
    private static final long MAX_STOP_TIME_QUERY_INTERVAL = 86400 * 2;
    
    private static final SimpleDateFormat ymdParser = new SimpleDateFormat("yyyyMMdd");
    {
        ymdParser.setTimeZone(TimeZone.getTimeZone("GMT"));
    }

    /**
     * Return a list of all agency ids in the graph
     */
    @GET
    @Path("/agencyIds")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_XML })
    public AgencyList getAgencyIds(@QueryParam("routerId") String routerId) throws JSONException {

        Graph graph = getGraph(routerId);

        AgencyList response = new AgencyList();
        response.agencies = graph.getAgencies();
        return response;
    }

    /**

     Return data about a route, such as its names, color, variants,
     stops, and directions.

     A variant represents a particular stop pattern (ordered list of
     stops) on a particular route. For example, the N train has at
     least four different variants: express (over the Manhattan
     bridge), and local (via lower Manhattan and the tunnel) x to
     Astoria and to Coney Island.

     Variant names are machine-generated, and are guaranteed to be
     unique (among variants for a route) but not stable across graph
     builds.

     A route's stops include stops made by any variant of the route.

    */
    @GET
    @Path("/routeData")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_XML })
    public Object getRouteData(@QueryParam("agency") String agency, @QueryParam("id") String id,
            @QueryParam("references") Boolean references, @QueryParam("extended") Boolean extended,
            @QueryParam("routerId") String routerId) throws JSONException {

        TransitIndexService transitIndexService = getGraph(routerId).getService(
                TransitIndexService.class);
        if (transitIndexService == null) {
            return new TransitError(
                    "No transit index found.  Add TransitIndexBuilder to your graph builder configuration and rebuild your graph.");
        }
        RouteDataList respond = new RouteDataList();

        for (String agencyId : getAgenciesIds(agency, routerId)) {
            AgencyAndId routeId = new AgencyAndId(agencyId, id);

            List<RouteVariant> variants = transitIndexService.getVariantsForRoute(routeId);

            if (variants.isEmpty())
                continue;

            RouteData response = new RouteData();
            response.id = routeId;
            response.variants = variants;
            response.directions = new ArrayList<String>(
                    transitIndexService.getDirectionsForRoute(routeId));
            response.route = new RouteType();
            for (RouteVariant variant : transitIndexService.getVariantsForRoute(routeId)) {
                Route route = variant.getRoute();
                response.route = new RouteType(route, extended);
                break;
            }

            if (references != null && references.equals(true)) {
                response.stops = new ArrayList<StopType>();
                for (org.onebusaway.gtfs.model.Stop stop : transitIndexService
                        .getStopsForRoute(routeId))
                    response.stops.add(new StopType(stop, extended));
            }

            respond.routeData.add(response);
        }

        return respond;
    }

    /**
     * Return a list of route ids
     */
    @GET
    @Path("/routes")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_XML })
    public Object getRoutes(@QueryParam("agency") String agency,
            @QueryParam("extended") Boolean extended, @QueryParam("routerId") String routerId)
            throws JSONException {

        TransitIndexService transitIndexService = getGraph(routerId).getService(
                TransitIndexService.class);
        if (transitIndexService == null) {
            return new TransitError(
                    "No transit index found.  Add TransitIndexBuilder to your graph builder configuration and rebuild your graph.");
        }
        Collection<AgencyAndId> allRouteIds = transitIndexService.getAllRouteIds();
        RouteList response = makeRouteList(allRouteIds, agency, extended, routerId);
        return response;
    }

    private RouteList makeRouteList(Collection<AgencyAndId> routeIds, String agencyFilter,
            @QueryParam("extended") Boolean extended, @QueryParam("routerId") String routerId) {
        RouteList response = new RouteList();
        TransitIndexService transitIndexService = getGraph(routerId).getService(
                TransitIndexService.class);
        for (AgencyAndId routeId : routeIds) {
            for (RouteVariant variant : transitIndexService.getVariantsForRoute(routeId)) {
                Route route = variant.getRoute();
                if (agencyFilter != null && !agencyFilter.equals(route.getAgency().getId()))
                    continue;
                RouteType routeType = new RouteType(route, extended);
                response.routes.add(routeType);
                break;
            }
        }
        return response;
    }

    /**
     * Return stops near a point. The default search radius is 200m, but this can be changed with the radius parameter (in meters)
     */
    @GET
    @Path("/stopsNearPoint")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_XML })
    public Object getStopsNearPoint(@QueryParam("agency") String agency,
            @QueryParam("lat") Double lat, @QueryParam("lon") Double lon,
            @QueryParam("extended") Boolean extended, @QueryParam("routerId") String routerId,
            @QueryParam("radius") Double radius) throws JSONException {

        // default search radius.
        Double searchRadius = (radius == null) ? STOP_SEARCH_RADIUS : radius;

        Graph graph = getGraph(routerId);

        if (Double.isNaN(searchRadius) || searchRadius <= 0) {
            searchRadius = STOP_SEARCH_RADIUS;
        }

        StreetVertexIndexService streetVertexIndexService = graph.streetIndex;
        List<org.opentripplanner.routing.vertextype.TransitStop> stops = streetVertexIndexService.getNearbyTransitStops(new Coordinate(
                lon, lat), searchRadius);
        TransitIndexService transitIndexService = graph.getService(TransitIndexService.class);
        if (transitIndexService == null) {
            return new TransitError(
                    "No transit index found.  Add TransitIndexBuilder to your graph builder configuration and rebuild your graph.");
        }

        StopList response = new StopList();
        for (org.opentripplanner.routing.vertextype.TransitStop transitStop : stops) {
            AgencyAndId stopId = transitStop.getStopId();
            if (agency != null && !agency.equals(stopId.getAgencyId()))
                continue;
            StopType stop = new StopType(transitStop.getStop(), extended);
            stop.routes = transitIndexService.getRoutesForStop(stopId);
            response.stops.add(stop);
        }

        return response;
    }

    /**
     * Return routes that a stop is served by
     */
    @GET
    @Path("/routesForStop")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_XML })
    public Object getRoutesForStop(@QueryParam("agency") String agency,
            @QueryParam("id") String id, @QueryParam("extended") Boolean extended,
            @QueryParam("routerId") String routerId) throws JSONException {

        TransitIndexService transitIndexService = getGraph(routerId).getService(
                TransitIndexService.class);
        if (transitIndexService == null) {
            return new TransitError(
                    "No transit index found.  Add TransitIndexBuilder to your graph builder configuration and rebuild your graph.");
        }

        RouteList result = new RouteList();

        for (String string : getAgenciesIds(agency, routerId)) {
            List<AgencyAndId> routes = transitIndexService.getRoutesForStop(new AgencyAndId(string,
                    id));
            result.routes.addAll(makeRouteList(routes, null, extended, routerId).routes);
        }

        return result;
    }

    /**
     * Return stop times for a stop, in seconds since the epoch startTime and endTime are in milliseconds since epoch
     */
    @GET
    @Path("/stopTimesForStop")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_XML })
    public Object getStopTimesForStop(@QueryParam("agency") String stopAgency,
            @QueryParam("id") String stopId, @QueryParam("startTime") long startTime,
            @QueryParam("endTime") Long endTime, @QueryParam("extended") Boolean extended,
            @QueryParam("references") Boolean references, @QueryParam("routeId") String routeId,
            @QueryParam("routerId") String routerId) throws JSONException {

        startTime /= 1000;

        if (endTime == null) {
            endTime = startTime + 86400;
        } else {
            endTime /= 1000;
        }

        if (endTime - startTime > MAX_STOP_TIME_QUERY_INTERVAL) {
            return new TransitError("Max stop time query interval is " + (endTime - startTime)
                    + " > " + MAX_STOP_TIME_QUERY_INTERVAL);
        }
        TransitIndexService transitIndexService = getGraph(routerId).getService(
                TransitIndexService.class);
        if (transitIndexService == null) {
            return new TransitError(
                    "No transit index found.  Add TransitIndexBuilder to your graph builder configuration and rebuild your graph.");
        }

        // if no stopAgency is set try to search through all different agencies
        Graph graph = getGraph(routerId);

        // add all departures
        HashSet<TripType> trips = new HashSet<TripType>();
        StopTimeList result = new StopTimeList();
        result.stopTimes = new ArrayList<StopTime>();

        if (references != null && references.equals(true)) {
            result.routes = new HashSet<Route>();
        }

        for (String stopAgencyId : getAgenciesIds(stopAgency, routerId)) {

            AgencyAndId stop = new AgencyAndId(stopAgencyId, stopId);
            Edge preBoardEdge = transitIndexService.getPreBoardEdge(stop);
            if (preBoardEdge == null)
                continue;
            Vertex boarding = preBoardEdge.getToVertex();

            RoutingRequest options = makeTraverseOptions(startTime, routerId);

            HashMap<Long, Edge> seen = new HashMap();
            OUTER: for (Edge e : boarding.getOutgoing()) {
                // each of these edges boards a separate set of trips
                for (StopTime st : getStopTimesForBoardEdge(startTime, endTime, options, e,
                        extended)) {
                    // different parameters
                    st.phase = "departure";
                    if (extended != null && extended.equals(true)) {
                        if (routeId != null && !routeId.equals("")
                                && !st.trip.getRoute().getId().getId().equals(routeId))
                            continue;
                        if (references != null && references.equals(true))
                            result.routes.add(st.trip.getRoute());
                        result.stopTimes.add(st);
                    } else
                        result.stopTimes.add(st);
                    trips.add(st.trip);
                    if (seen.containsKey(st.time)) {
                        Edge old = seen.get(st.time);
                        System.out.println("DUP: " + old);
                        getStopTimesForBoardEdge(startTime, endTime, options, e, extended);
                        // break OUTER;
                    }
                    seen.put(st.time, e);
                }
            }

            // add the arriving stop times for cases where there are no departures
            Edge preAlightEdge = transitIndexService.getPreAlightEdge(stop);
            Vertex alighting = preAlightEdge.getFromVertex();
            for (Edge e : alighting.getIncoming()) {
                for (StopTime st : getStopTimesForAlightEdge(startTime, endTime, options, e,
                        extended)) {
                    if (!trips.contains(st.trip)) {
                        // diffrent parameters
                        st.phase = "arrival";
                        if (extended != null && extended.equals(true)) {
                            if (references != null && references.equals(true))
                                result.routes.add(st.trip.getRoute());
                            if (routeId != null && !routeId.equals("")
                                    && !st.trip.getRoute().getId().getId().equals(routeId))
                                continue;
                            result.stopTimes.add(st);
                        } else
                            result.stopTimes.add(st);
                    }
                }
            }

        }
        Collections.sort(result.stopTimes, new Comparator<StopTime>() {

            @Override
            public int compare(StopTime o1, StopTime o2) {
                if (o1.phase.equals("arrival") && o2.phase.equals("departure"))
                    return 1;
                if (o1.phase.equals("departure") && o2.phase.equals("arrival"))
                    return -1;
                return o1.time - o2.time > 0 ? 1 : -1;
            }

        });

        return result;
    }

    private RoutingRequest makeTraverseOptions(long startTime, String routerId) {
        RoutingRequest options = new RoutingRequest();
        // if (graphService.getCalendarService() != null) {
        // options.setCalendarService(graphService.getCalendarService());
        // options.setServiceDays(startTime, agencies);
        // }
        // TODO: verify correctness
        options.dateTime = startTime;
        Graph graph = getGraph(routerId);
        Collection<Vertex> vertices = graph.getVertices();
        Iterator<Vertex> it = vertices.iterator();
        options.setFromString(it.next().getLabel());
        options.setToString(it.next().getLabel());
        options.setRoutingContext(graph);
        return options;
    }

    /**
     * Return variant for a trip
     */
    @GET
    @Path("/variantForTrip")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_XML })
    public Object getVariantForTrip(@QueryParam("tripAgency") String tripAgency,
            @QueryParam("tripId") String tripId, @QueryParam("routerId") String routerId)
            throws JSONException {

        TransitIndexService transitIndexService = getGraph(routerId).getService(
                TransitIndexService.class);

        if (transitIndexService == null) {
            return new TransitError(
                    "No transit index found.  Add TransitIndexBuilder to your graph builder configuration and rebuild your graph.");
        }

        AgencyAndId trip = new AgencyAndId(tripAgency, tripId);
        RouteVariant variant = transitIndexService.getVariantForTrip(trip);

        return variant;
    }

    /**
     * Return information about calendar for given agency
     */
    @GET
    @Path("/calendar")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_XML })
    public Object getCalendar(@QueryParam("agency") String agency,
            @QueryParam("routerId") String routerId) throws JSONException {

        TransitIndexService transitIndexService = getGraph(routerId).getService(
                TransitIndexService.class);

        if (transitIndexService == null) {
            return new TransitError(
                    "No transit index found.  Add TransitIndexBuilder to your graph builder configuration and rebuild your graph.");
        }

        CalendarData response = new CalendarData();
        response.calendarList = new ArrayList<ServiceCalendarType>();
        response.calendarDatesList = new ArrayList<ServiceCalendarDateType>();

        for (String agencyId : getAgenciesIds(agency, routerId)) {
            List<ServiceCalendar> scList = transitIndexService.getCalendarsByAgency(agencyId);
            List<ServiceCalendarDate> scdList = transitIndexService
                    .getCalendarDatesByAgency(agencyId);

            if (scList != null)
                for (ServiceCalendar sc : scList)
                    response.calendarList.add(new ServiceCalendarType(sc));
            if (scdList != null)
                for (ServiceCalendarDate scd : scdList)
                    response.calendarDatesList.add(new ServiceCalendarDateType(scd));
        }

        return response;
    }

    /**
     * Return subsequent stop times for a trip; time is in milliseconds since epoch
     */
    @GET
    @Path("/stopTimesForTrip")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_XML })
    public Object getStopTimesForTrip(@QueryParam("stopAgency") String stopAgency,
            @QueryParam("stopId") String stopId, @QueryParam("tripAgency") String tripAgency,
            @QueryParam("tripId") String tripId, @QueryParam("time") long time,
            @QueryParam("extended") Boolean extended, @QueryParam("routerId") String routerId)
            throws JSONException {

        time /= 1000;

        AgencyAndId firstStop = null;
        if (stopId != null) {
            firstStop = new AgencyAndId(stopAgency, stopId);
        }
        AgencyAndId trip = new AgencyAndId(tripAgency, tripId);

        TransitIndexService transitIndexService = getGraph(routerId).getService(
                TransitIndexService.class);

        if (transitIndexService == null) {
            return new TransitError(
                    "No transit index found.  Add TransitIndexBuilder to your graph builder configuration and rebuild your graph.");
        }

        RouteVariant variant = transitIndexService.getVariantForTrip(trip);
        RoutingRequest options = makeTraverseOptions(time, routerId);

        StopTimeList result = new StopTimeList();
        result.stopTimes = new ArrayList<StopTime>();
        State state = null;
        RouteSegment start = null;
        for (RouteSegment segment : variant.getSegments()) {
            // this is all segments across all patterns that match this variant
            if (segment.stop.equals(firstStop)) {
                // this might be the correct start segment, but we need to try traversing and see if we get this trip
                // TODO: verify options and state creation correctness (AMB)
                State s0 = new State(segment.board.getFromVertex(), options);
                state = segment.board.traverse(s0);
                if (state == null)
                    continue;
                if (state.getBackTrip().getId().equals(trip)) {
                    start = segment;
                    StopTime st = new StopTime();
                    st.time = state.getTimeSeconds();
                    for (org.onebusaway.gtfs.model.Stop stop : variant.getStops())
                        if (stop.getId().equals(segment.stop)) {
                            st.stop = new StopType(stop, extended);
                        }
                    result.stopTimes.add(st);
                    break;
                }
            }
        }
        if (start == null) {
            return null;
        }

        for (RouteSegment segment : variant.segmentsAfter(start)) {
            // TODO: verify options/state init correctness
            State s0 = new State(segment.hopIn.getFromVertex(), state.getTimeSeconds(), options);
            state = segment.hopIn.traverse(s0);
            StopTime st = new StopTime();
            st.time = state.getTimeSeconds();
            for (org.onebusaway.gtfs.model.Stop stop : variant.getStops())
                if (stop.getId().equals(segment.stop))
                    if (stop.getId().equals(segment.stop)) {
                        if (extended != null && extended.equals(true)) {
                            st.stop = new StopType(stop, extended);
                        }
                    }
            result.stopTimes.add(st);
        }

        return result;
    }

    private List<StopTime> getStopTimesForBoardEdge(long startTime, long endTime,
            RoutingRequest options, Edge e, Boolean extended) {
        List<StopTime> out = new ArrayList<StopTime>();
        State result;
        long time = startTime;
        do {
            // TODO verify options/state correctness
            State s0 = new State(e.getFromVertex(), time, options);
            result = e.traverse(s0);
            if (result == null)
                break;
            time = result.getTimeSeconds();
            if (time > endTime)
                break;
            StopTime stopTime = new StopTime();
            stopTime.time = time;
            stopTime.trip = new TripType(result.getBackTrip(), extended);
            out.add(stopTime);

            time += 1; // move to the next board time
        } while (true);
        return out;
    }

    private List<StopTime> getStopTimesForAlightEdge(long startTime, long endTime,
            RoutingRequest options, Edge e, Boolean extended) {
        List<StopTime> out = new ArrayList<StopTime>();
        State result;
        long time = endTime;
        options = options.reversedClone();
        do {
            // TODO: verify options/state correctness
            State s0 = new State(e.getToVertex(), time, options);
            result = e.traverse(s0);
            if (result == null)
                break;
            time = result.getTimeSeconds();
            if (time < startTime)
                break;
            StopTime stopTime = new StopTime();
            stopTime.time = time;
            stopTime.trip = new TripType(result.getBackTrip(), extended);
            out.add(stopTime);
            time -= 1; // move to the previous alight time
        } while (true);
        return out;
    }

    /**
     * Return a list of all available transit modes supported, if any.
     * 
     * @throws JSONException
     */
    @GET
    @Path("/modes")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_XML })
    public Object getModes(@QueryParam("routerId") String routerId) throws JSONException {
        TransitIndexService transitIndexService = getGraph(routerId).getService(
                TransitIndexService.class);
        if (transitIndexService == null) {
            return new TransitError(
                    "No transit index found.  Add TransitIndexBuilder to your graph builder configuration and rebuild your graph.");
        }

        ModeList modes = new ModeList();
        modes.modes = new ArrayList<TraverseMode>();
        for (TraverseMode mode : transitIndexService.getAllModes()) {
            modes.modes.add(mode);
        }
        return modes;
    }

    private Graph getGraph(String routerId) {
        return graphService.getGraph(routerId);
    }

    public Object getCalendarServiceDataForAgency(@QueryParam("agency") String agency,
            @QueryParam("routerId") String routerId) {
        TransitIndexService transitIndexService = getGraph(routerId).getService(
                TransitIndexService.class);
        if (transitIndexService == null) {
            return new TransitError(
                    "No transit index found.  Add TransitIndexBuilder to your graph builder configuration and rebuild your graph.");
        }

        ServiceCalendarData data = new ServiceCalendarData();

        data.calendars = transitIndexService.getCalendarsByAgency(agency);
        data.calendarDates = transitIndexService.getCalendarDatesByAgency(agency);

        return data;
    }

    /**
     * Return a list of all routes that operate between start stop and end stop.
     */
    @GET
    @Path("/routesBetweenStops")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_XML })
    public Object routesBetweenStops(@QueryParam("startAgency") String startAgency,
            @QueryParam("endAgency") String endAgency,
            @QueryParam("startStopId") String startStopId,
            @QueryParam("endStopId") String endStopId, @QueryParam("extended") Boolean extended,
            @QueryParam("routerId") String routerId) throws JSONException {

        RouteList response = new RouteList();

        RouteList routeList = (RouteList) this.getRoutesForStop(startAgency, startStopId, extended,
                routerId);

        for (RouteType route : routeList.routes) {
            for (String agency : getAgenciesIds(null, routerId)) {
                if (ifRouteBetweenStops(route, agency, routerId, startStopId, endStopId, endAgency))
                    response.routes.add(route);
            }
        }

        return response;
    }

    private Boolean ifRouteBetweenStops(RouteType route, String agency, String routerId,
            String startStopId, String endStopId, String endAgency) throws JSONException {

        RouteDataList routeDataList = (RouteDataList) this.getRouteData(agency, route.getId()
                .getId(), false, false, routerId);
        for (RouteData routeData : routeDataList.routeData)
            for (RouteVariant variant : routeData.variants)
                for (String endStopAgency : getAgenciesIds(endAgency, routerId)) {
                    Boolean start = false;
                    for (Stop stop : variant.getStops()) {
                        if (stop.getId().getId().equals(startStopId))
                            start = true;
                        if (start && stop.getId().equals(new AgencyAndId(endStopAgency, endStopId))) {
                            return true;
                        }
                    }
                }
        return false;
    }

    private ArrayList<String> getAgenciesIds(String agency, String routerId) {

        Graph graph = getGraph(routerId);

        ArrayList<String> agencyList = new ArrayList<String>();
        if (agency == null || agency.equals("")) {
            for (String a : graph.getAgencyIds()) {
                agencyList.add(a);
            }
        } else {
            agencyList.add(agency);
        }
        return agencyList;
    }
    
    /* REWRITE */
    
    @GET
    @Path("/stop")
    @Produces({ MediaType.APPLICATION_JSON })
    public TransitResponse stop(@QueryParam("stopId") String id, @QueryParam("routerId") String routerId) throws JSONException {

        Graph graph = getGraph(routerId);
        TransitIndexService transitIndexService = graph.getService(TransitIndexService.class);
        if (transitIndexService == null) {
            return TransitResponseBuilder.getFailResponse(
                    "No transit index found.  Add TransitIndexBuilder to your graph builder configuration and rebuild your graph.");
        }
        
        AgencyAndId stopId = parseAgencyAndId(id);
        Stop stop = transitIndexService.getAllStops().get(stopId);
        if(stop == null) {
            return TransitResponseBuilder.getFailResponse("Unknown stopId.");
        }

        return TransitResponseBuilder.getResponseForStop(graph, stop);
    }
    
    @GET
    @Path("/stops-for-location")
    @Produces({ MediaType.APPLICATION_JSON })
    public TransitResponse stopsForLocation(@QueryParam("agency") String agency,
            @QueryParam("neLonLat") String ne, @QueryParam("swLonLat") String sw,
            @QueryParam("extended") Boolean extended, @QueryParam("routerId") String routerId) throws JSONException {

        Graph graph = getGraph(routerId);
        TransitIndexService transitIndexService = graph.getService(TransitIndexService.class);
        if (transitIndexService == null) {
            return TransitResponseBuilder.getFailResponse(
                    "No transit index found.  Add TransitIndexBuilder to your graph builder configuration and rebuild your graph.");
        }
        
        Coordinate c1 = null, c2 = null;
        if(ne != null && sw != null) {
            if(ne.indexOf(',') > 0 && sw.indexOf(',') > 0
                    && ne.indexOf(',') == ne.lastIndexOf(',') && sw.indexOf(',') == sw.lastIndexOf(',')) {
                String[] parts1 = ne.split(",");
                String[] parts2 = sw.split(",");
                c1 = new Coordinate(Double.parseDouble(parts1[0]), Double.parseDouble(parts1[1]));
                c2 = new Coordinate(Double.parseDouble(parts2[0]), Double.parseDouble(parts2[1]));
            }
            
            if(c1 == null || c2 == null)
                return TransitResponseBuilder.getFailResponse("Failed to parse coordinates.");
        }
    
        List<Stop> stops = new LinkedList<Stop>();
        if (c1 == null || c2 == null) {
            for (Vertex gv : graph.getVertices()) {
                if (gv instanceof org.opentripplanner.routing.vertextype.TransitStop) {
                    stops.add(((org.opentripplanner.routing.vertextype.TransitStop) gv).getStop());
                }
            }
        } else {
            StreetVertexIndexService streetVertexIndexService = graph.streetIndex;
            List<org.opentripplanner.routing.vertextype.TransitStop> stopVertices = streetVertexIndexService.getNearbyTransitStops(c1, c2);

            for (org.opentripplanner.routing.vertextype.TransitStop transitStop : stopVertices) {
                AgencyAndId stopId = transitStop.getStopId();
                if (agency != null && !agency.equals(stopId.getAgencyId()))
                    continue;
               stops.add(transitStop.getStop());
            }
        }

        return TransitResponseBuilder.getResponseForStops(graph, stops);
    }
    
    @GET
    @Path("/route")
    @Produces({ MediaType.APPLICATION_JSON })
    public TransitResponse route(@QueryParam("routeId") String id, @QueryParam("routerId") String routerId) throws JSONException {

        Graph graph = getGraph(routerId);
        TransitIndexService transitIndexService = graph.getService(TransitIndexService.class);
        if (transitIndexService == null) {
            return TransitResponseBuilder.getFailResponse(
                    "No transit index found.  Add TransitIndexBuilder to your graph builder configuration and rebuild your graph.");
        }
        
        AgencyAndId routeId = parseAgencyAndId(id);
        Route route =  transitIndexService.getAllRoutes().get(routeId);
        if(route == null) {
            return TransitResponseBuilder.getFailResponse("Unknown routeId.");
        }

        return TransitResponseBuilder.getResponseForRoute(graph, route);
    }
    
    @GET
    @Path("/route-details")
    @Produces({ MediaType.APPLICATION_JSON })
    public TransitResponse routeDetails(@QueryParam("routeId") String id, @QueryParam("date") String date, @QueryParam("routerId") String routerId) throws JSONException {

        Graph graph = getGraph(routerId);
        TransitIndexService transitIndexService = graph.getService(TransitIndexService.class);
        if (transitIndexService == null) {
            return TransitResponseBuilder.getFailResponse(
                    "No transit index found.  Add TransitIndexBuilder to your graph builder configuration and rebuild your graph.");
        }
        
        TransitResponseBuilder builder = new TransitResponseBuilder(graph);
        AgencyAndId routeId = parseAgencyAndId(id);
        Route route =  transitIndexService.getAllRoutes().get(routeId);
        if(route == null) {
            return TransitResponseBuilder.getFailResponse("Unknown routeId.");
        }
        
        ServiceDate serviceDate = new ServiceDate();
        if(date != null) {
            try {
                serviceDate = new ServiceDate(ymdParser.parse(date));
            } catch (ParseException ex) {
                return TransitResponseBuilder.getFailResponse("Failed to parse service date.");
            }
        }
        
        long startTime = serviceDate.getAsDate().getTime() / 1000;
        long endTime = serviceDate.next().getAsDate().getTime() / 1000;
        RoutingRequest options = makeTraverseOptions(startTime, routerId);
        
        List<String> alertIds = getAlertsForRoute(builder, routeId, options, startTime, endTime);
        
        List<RouteVariant> routeVariants = transitIndexService.getVariantsForRoute(routeId);
        
        return builder.getResponseForRoute(route, routeVariants, alertIds);
    }
    
    @GET
    @Path("/route-details-for-stop")
    @Produces({ MediaType.APPLICATION_JSON })
    public TransitResponse routeDetailsForStop(@QueryParam("stopId") String id, @QueryParam("routerId") String routerId) throws JSONException {

        Graph graph = getGraph(routerId);
        TransitIndexService transitIndexService = graph.getService(TransitIndexService.class);
        if (transitIndexService == null) {
            return TransitResponseBuilder.getFailResponse(
                    "No transit index found.  Add TransitIndexBuilder to your graph builder configuration and rebuild your graph.");
        }
        
        AgencyAndId stopId = parseAgencyAndId(id);
        Stop stop =  transitIndexService.getAllStops().get(stopId);
        if(stop == null) {
            return TransitResponseBuilder.getFailResponse("Unknown stopId.");
        }
        
        TransitResponseBuilder builder = new TransitResponseBuilder(graph);
        List<TransitRoute> routes = new LinkedList<TransitRoute>();
        for(AgencyAndId routeId : transitIndexService.getRoutesForStop(stopId)) {
            Route route = transitIndexService.getAllRoutes().get(routeId);
            List<RouteVariant> routeVariants = transitIndexService.getVariantsForRoute(routeId);
            TransitRoute transitRoute = builder.getRoute(route, routeVariants, Collections.<String> emptyList()); // TODO: alerts?
            routes.add(transitRoute);
        }
        
        return builder.getResponseForRoutes(routes);
    }
    
    @GET
    @Path("/trip")
    @Produces({ MediaType.APPLICATION_JSON })
    public TransitResponse trip(@QueryParam("tripId") String id, @QueryParam("routerId") String routerId) throws JSONException {

        Graph graph = getGraph(routerId);
        TransitIndexService transitIndexService = graph.getService(TransitIndexService.class);
        if (transitIndexService == null) {
            return TransitResponseBuilder.getFailResponse(
                    "No transit index found.  Add TransitIndexBuilder to your graph builder configuration and rebuild your graph.");
        }
        
        AgencyAndId tripId = parseAgencyAndId(id);
        if(transitIndexService.getPatternForTrip(tripId) == null)
            return TransitResponseBuilder.getFailResponse("Unknown tripId.");
        
        Trip trip = getTrip(transitIndexService, tripId);
        return TransitResponseBuilder.getResponseForTrip(graph, trip);
    }
    
    @GET
    @Path("/trip-details")
    @Produces({ MediaType.APPLICATION_JSON })
    public TransitResponse tripDetails(@QueryParam("tripId") String id, @QueryParam("date") String date, @QueryParam("routerId") String routerId) throws JSONException {

        Graph graph = getGraph(routerId);
        VehicleLocationService vehicleLocationService = graph.getService(VehicleLocationService.class);
        TransitIndexService transitIndexService = graph.getService(TransitIndexService.class);
        if (transitIndexService == null) {
            return TransitResponseBuilder.getFailResponse(
                    "No transit index found.  Add TransitIndexBuilder to your graph builder configuration and rebuild your graph.");
        }
        
        AgencyAndId tripId = parseAgencyAndId(id);
        if(transitIndexService.getPatternForTrip(tripId) == null)
            return TransitResponseBuilder.getFailResponse("Unknown tripId.");
            
        Trip trip = getTrip(transitIndexService, tripId);
        ServiceDate serviceDate = new ServiceDate();
        if(date != null) {
            try {
                serviceDate = new ServiceDate(ymdParser.parse(date));
            } catch (ParseException ex) {
                return TransitResponseBuilder.getFailResponse("Failed to parse service date.");
            }
        }
        
        TransitResponseBuilder builder = new TransitResponseBuilder(graph);
        CalendarService calendarService = graph.getCalendarService();
        ServiceDay serviceDay = new ServiceDay(graph, serviceDate.getAsDate().getTime() / 1000, calendarService, trip.getId().getAgencyId());
        
        long startTime = serviceDate.getAsDate().getTime() / 1000;
        long endTime = serviceDate.next().getAsDate().getTime() / 1000;
        RoutingRequest options = makeTraverseOptions(startTime, routerId);
        
        RouteVariant variant = transitIndexService.getVariantForTrip(tripId);
        
        TableTripPattern pattern = transitIndexService.getPatternForTrip(tripId);
        if(!serviceDay.serviceIdRunning(pattern.getServiceId()))
            return TransitResponseBuilder.getFailResponse("Trip isn't operation on the given service date.");
        
        int tripIndex = pattern.getTripIndex(tripId);
        Timetable timetable;
        if(graph.timetableSnapshotSource != null && graph.timetableSnapshotSource.getSnapshot() != null) {
            timetable = graph.timetableSnapshotSource.getSnapshot().resolve(pattern, serviceDate);
        } else {
            timetable = pattern.getScheduledTimetable();
        }
        List<TransitStopTime> stopTimes = getStopTimesForTrip(routerId, tripId, serviceDate, pattern, timetable);
        List<TransitAlert> alerts = new LinkedList<TransitAlert>(); // TODO
        
        TransitVehicle transitVehicle = null;
        if(vehicleLocationService != null) {
            VehicleLocation vehicle = vehicleLocationService.getForTrip(tripId);
            if(vehicle != null)
                transitVehicle = builder.getVehicle(vehicle);
        }
        
        TransitTrip transitTrip = builder.getTrip(trip);
        transitTrip.setWheelchairAccessible(timetable.isWheelchairAccessible(tripIndex));
        
        AgencyAndId routeId = trip.getRoute().getId();
        List<String> alertIds = getAlertsForRoute(builder, routeId, options, startTime, endTime);
                
        return builder.getResponseForTrip(transitTrip, serviceDate, alertIds, pattern.getStops(), stopTimes, transitVehicle, variant);
    }
    
    public List<TransitStopTime> getStopTimesForTrip(String routerId, AgencyAndId tripId, ServiceDate serviceDate, TableTripPattern pattern, Timetable timetable) {

        List<TransitStopTime> stopTimes = new LinkedList<TransitStopTime>();

        long time = serviceDate.getAsDate().getTime() / 1000;
        int tripIndex = pattern.getTripIndex(tripId);
        
        int numStops = pattern.getStops().size();
        for(int i = 0; i < numStops; ++i) {
            TransitStopTime stopTime = new TransitStopTime();
            stopTime.setStopId(pattern.getStops().get(i).getId().toString());

            TripTimes tripTimes = timetable.getTripTimes(tripIndex);
            TripTimes scheduledTripTimes = tripTimes.getScheduledTripTimes();
            
            if(tripTimes.canBoard(i) && i + 1 < numStops) {
                if(!tripTimes.isScheduled())
                    stopTime.setPredictedDepartureTime(time + tripTimes.getDepartureTime(i));
                if(scheduledTripTimes != null)
                    stopTime.setDepartureTime(time + scheduledTripTimes.getDepartureTime(i));
            }
            if(tripTimes.canAlight(i) && i > 0) {
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
    
    @GET
    @Path("/arrivals-and-departures")
    @Produces({ MediaType.APPLICATION_JSON })
    public TransitResponse arrivalsAndDepartures(@QueryParam("stopId") String id, @QueryParam("time") Long time, 
             @QueryParam("minutesBefore") @DefaultValue("2") int minutesBefore, @QueryParam("minutesAfter") @DefaultValue("30") int minutesAfter,
             @QueryParam("routerId") String routerId ) throws JSONException {

        Graph graph = getGraph(routerId);
        TransitIndexService transitIndexService = graph.getService(TransitIndexService.class);
        if (transitIndexService == null) {
            return TransitResponseBuilder.getFailResponse(
                    "No transit index found.  Add TransitIndexBuilder to your graph builder configuration and rebuild your graph.");
        }
        
        if(time == null)
            time = System.currentTimeMillis() / 1000;
        
        long startTime = time - minutesBefore * 60;
        long endTime   = time + minutesAfter  * 60;
        RoutingRequest options = makeTraverseOptions(startTime, routerId);
        
        TransitResponseBuilder builder = new TransitResponseBuilder(graph);
        AgencyAndId stopId = parseAgencyAndId(id);
        Stop stop = transitIndexService.getAllStops().get(stopId);
        if(stop == null)
            return TransitResponseBuilder.getFailResponse("Unknown stopId.");
        
        List<T2<TransitScheduleStopTime, TransitTrip>> stopTimesWithTrips = getStopTimesForStop(routerId, builder, startTime, endTime, stopId);
        sortStopTimesWithTrips(stopTimesWithTrips);
        
        List<TransitScheduleStopTime> stopTimes = new LinkedList<TransitScheduleStopTime>();
        List<TransitTrip> trips = new LinkedList<TransitTrip>();
        for(T2<TransitScheduleStopTime, TransitTrip> stopTimeWithTrip : stopTimesWithTrips) {
            stopTimes.add(stopTimeWithTrip.getFirst());
            trips.add(stopTimeWithTrip.getSecond());
        }
        
        List<String> alertIds = getAlertsForStop(builder, stopId, options, startTime, endTime);

        return builder.getResponseForStop(stop, stopTimes, alertIds, trips);
    }
    
    // TODO: return alights also
    private List<T2<TransitScheduleStopTime, TransitTrip>> getStopTimesForStop(String routerId, TransitResponseBuilder builder, long startTime, long endTime, AgencyAndId stopId) {
        Graph graph = getGraph(routerId);
        TransitIndexService transitIndexService = graph.getService(TransitIndexService.class);
        
        PreAlightEdge preAlightEdge = transitIndexService.getPreAlightEdge(stopId);
        PreBoardEdge preBoardEdge = transitIndexService.getPreBoardEdge(stopId);
        
        RoutingRequest options = makeTraverseOptions(startTime, routerId);
        List<T2<TransitScheduleStopTime, TransitTrip>> boardingTimes = getStopTimesForPreBoardEdge(builder, stopId.toString(), startTime, endTime, options, preBoardEdge);
        //List<T2<TransitScheduleStopTime, TransitTrip>> alightingTimes = getStopTimesForPreAlightEdge(builder, stopId.toString(), startTime, endTime, options, preAlightEdge);
        //List<T2<TransitScheduleStopTime, TransitTrip>> mergedStopTimes = mergeStopTimes(boardingTimes, alightingTimes);
        
        return boardingTimes;
    }
    
    private List<T2<TransitScheduleStopTime, TransitTrip>> getStopTimesForPreAlightEdge(TransitResponseBuilder builder, String stopId, long startTime, long endTime,
            RoutingRequest options, PreAlightEdge edge) {
        List<T2<TransitScheduleStopTime, TransitTrip>> result = new ArrayList<T2<TransitScheduleStopTime, TransitTrip>>();
        for(Edge e : edge.getFromVertex().getIncoming()) {
            if(!(e instanceof TransitBoardAlight))
                continue;
            
            result.addAll(getStopTimesForTransitBoardAlightEdge(builder, stopId, startTime, endTime, options, (TransitBoardAlight) e));
        }
        
        return result;
    }
    
    private List<T2<TransitScheduleStopTime, TransitTrip>> getStopTimesForPreBoardEdge(TransitResponseBuilder builder, String stopId, long startTime, long endTime,
            RoutingRequest options, PreBoardEdge edge) {
        List<T2<TransitScheduleStopTime, TransitTrip>> result = new ArrayList<T2<TransitScheduleStopTime, TransitTrip>>();
        for(Edge e : edge.getToVertex().getOutgoing()) {
            if(!(e instanceof TransitBoardAlight))
                continue;
            
            result.addAll(getStopTimesForTransitBoardAlightEdge(builder, stopId, startTime, endTime, options, (TransitBoardAlight) e));
        }
        
        return result;
    }

    private List<T2<TransitScheduleStopTime, TransitTrip>> getStopTimesForTransitBoardAlightEdge(TransitResponseBuilder builder, String stopId, long startTime, long endTime,
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
            
            TripTimes tripTimes = result.getTripTimes();
            TripTimes scheduledTripTimes = tripTimes.getScheduledTripTimes();
            
            Trip trip = result.getBackTrip();
            TransitTrip transitTrip = builder.getTrip(trip);
            transitTrip.setWheelchairAccessible(tripTimes.isWheelchairAccessible());
            
            TransitScheduleStopTime stopTime = new TransitScheduleStopTime();
            stopTime.setTripId(transitTrip.getId());
            stopTime.setStopId(stopId);
            stopTime.setServiceDate(result.getServiceDay().getServiceDate().getAsString());
            
            Set<Alert> alerts = result.getBackAlerts();
            if(alerts != null && !alerts.isEmpty()) {
                List<String> alertIds = new LinkedList<String>();
                for(Alert alert : alerts) {
                    builder.addToReferences(alert);
                    alertIds.add(alert.alertId.toString());
                }
                stopTime.setAlertIds(alertIds);
            }
            
            if(tripTimes.canBoard(stopIndex) && stopIndex + 1 < numStops) {
                if(!tripTimes.isScheduled())
                    stopTime.setPredictedDepartureTime(time + tripTimes.getDepartureTime(stopIndex));
                if(scheduledTripTimes != null)
                    stopTime.setDepartureTime(time + scheduledTripTimes.getDepartureTime(stopIndex));
            }
            if(tripTimes.canAlight(stopIndex) && stopIndex > 0) {
                if(!tripTimes.isScheduled())
                    stopTime.setPredictedArrivalTime(time + tripTimes.getArrivalTime(stopIndex - 1));
                if(scheduledTripTimes != null)
                    stopTime.setArrivalTime(time + scheduledTripTimes.getArrivalTime(stopIndex - 1));
            }
            out.add(new T2<TransitScheduleStopTime, TransitTrip>(stopTime, transitTrip));
            
            builder.addToReferences(trip.getRoute());

            time += 1; // move to the next board time
        } while (true);
        
        return out;
    }
    
    @GET
    @Path("/schedule-for-stop")
    @Produces({ MediaType.APPLICATION_JSON })
    public TransitResponse scheduleForStop(@QueryParam("stopId") String id, @QueryParam("date") String date, @QueryParam("routerId") String routerId) throws JSONException {

        Graph graph = getGraph(routerId);
        TransitIndexService transitIndexService = graph.getService(TransitIndexService.class);
        if (transitIndexService == null) {
            return TransitResponseBuilder.getFailResponse(
                    "No transit index found.  Add TransitIndexBuilder to your graph builder configuration and rebuild your graph.");
        }
        
        ServiceDate serviceDate = new ServiceDate();
        if(date != null) {
            try {
                serviceDate = new ServiceDate(ymdParser.parse(date));
            } catch (ParseException ex) {
                return TransitResponseBuilder.getFailResponse("Failed to parse service date.");
            }
        }
        
        TransitResponseBuilder builder = new TransitResponseBuilder(graph);
        AgencyAndId stopId = parseAgencyAndId(id);
        Stop stop = transitIndexService.getAllStops().get(stopId);
        if(stop == null)
            return TransitResponseBuilder.getFailResponse("Unknown stopId.");
        
        long startTime = serviceDate.getAsDate().getTime() / 1000;
        long endTime = serviceDate.next().getAsDate().getTime() / 1000;
        RoutingRequest options = makeTraverseOptions(startTime, routerId);
        
        List<T2<TransitScheduleStopTime, TransitTrip>> stopTimesWithTrips = getStopTimesForStop(routerId, builder, startTime, endTime, stopId);
        
        Map<T2<String, String>, List<TransitScheduleStopTime>> stopTimesByRoute = new HashMap<T2<String, String>, List<TransitScheduleStopTime>>();
        for(T2<TransitScheduleStopTime, TransitTrip> stopTimeWithTrip : stopTimesWithTrips) {
            TransitScheduleStopTime stopTime = stopTimeWithTrip.getFirst();
            TransitTrip transitTrip = stopTimeWithTrip.getSecond();
            stopTime.setWheelchairAccessible(transitTrip.isWheelchairAccessible());
            stopTime.setHeadsign(transitTrip.getHeadsign());
            //builder.addToReferences(transitTrip);
            MapUtils.addToMapList(stopTimesByRoute, new T2<String, String>(transitTrip.getRouteId(), transitTrip.getDirectionId()), stopTime);
        }
        
        Map<String, TransitRouteSchedule> scheduleByRoute = new HashMap<String, TransitRouteSchedule>();
        for(T2<String, String> key : stopTimesByRoute.keySet()) {
            String SrouteId = key.getFirst();
            String direction = key.getSecond();
            
            TransitRouteSchedule routeSchedule = scheduleByRoute.get(SrouteId);
            if(routeSchedule == null) {
                routeSchedule = new TransitRouteSchedule();
                routeSchedule.setRouteId(SrouteId);
                scheduleByRoute.put(SrouteId, routeSchedule);
                
                AgencyAndId routeId = parseAgencyAndId(SrouteId);
                List<String> alertIds = getAlertsForRoute(builder, routeId, options, startTime, endTime);
                routeSchedule.setAlertIds(alertIds);
            }
            
            List<TransitScheduleStopTime> stopTimes = stopTimesByRoute.get(key);
            sortStopTimes(stopTimes);
            
            Map<String, TransitScheduleGroup> groups = new HashMap<String, TransitScheduleGroup>();
            SortedMap<Integer, Integer> groupCounts = new TreeMap<Integer, Integer>();
            for(TransitScheduleStopTime stopTime : stopTimes) {
                AgencyAndId tripId = parseAgencyAndId(stopTime.getTripId());
                RouteVariant variant = transitIndexService.getVariantForTrip(tripId);
                Integer groupId = variant.getId();
                
                if(!groups.containsKey(groupId.toString())) {
                    TransitScheduleGroup group = new TransitScheduleGroup();
                    group.setGroupId(groupId.toString());
                    group.setDescription(variant.getName());
                    group.setHeadsign(stopTime.getHeadsign());
                    groups.put(groupId.toString(), group);
                }
                
                if(groupCounts.containsKey(groupId)) {
                    groupCounts.put(groupId, groupCounts.get(groupId) + 1);
                } else {
                    groupCounts.put(groupId, 1);
                }
                
                stopTime.setGroupIds(Collections.singletonList(groupId.toString()));
            }
            
            TransitRouteScheduleForDirection directionSchedule = new TransitRouteScheduleForDirection();
            directionSchedule.setDirectionId(direction);
            directionSchedule.setStopTimes(stopTimes);
            directionSchedule.setGroups(groups);
            routeSchedule.getDirections().add(directionSchedule);
        }
        
        List<String> alertIds = getAlertsForStop(builder, stopId, options, startTime, endTime);
        
        TransitSchedule schedule = new TransitSchedule();
        schedule.setStopId(id);
        schedule.setAlertIds(alertIds);
        schedule.setServiceDate(serviceDate.getAsString());
        schedule.setSchedules(new ArrayList<TransitRouteSchedule>(scheduleByRoute.values()));
        
        return builder.getResponseForStopSchedule(stop, schedule);
    }
    
    @GET
    @Path("/vehicles-for-location")
    @Produces({ MediaType.APPLICATION_JSON })
    public TransitResponse vehicles(@QueryParam("neLonLat") String ne, @QueryParam("swLonLat") String sw, @QueryParam("routerId") String routerId) throws JSONException {

        Graph graph = getGraph(routerId);
        TransitIndexService transitIndexService = graph.getService(TransitIndexService.class);
        if (transitIndexService == null) {
            return TransitResponseBuilder.getFailResponse(
                    "No transit index found.  Add TransitIndexBuilder to your graph builder configuration and rebuild your graph.");
        }
        
        VehicleLocationService vehicleLocationService = graph.getService(VehicleLocationService.class);
        if(vehicleLocationService == null)
            return TransitResponseBuilder.getFailResponse("VehicleLocationService not found.");
        
        TransitResponseBuilder builder = new TransitResponseBuilder(graph);
        
        Collection<VehicleLocation> vehicles = null;
        
        if(ne != null && sw != null) {
            Coordinate c1 = null, c2 = null;
            if(ne.indexOf(',') > 0 && sw.indexOf(',') > 0
                    && ne.indexOf(',') == ne.lastIndexOf(',') && sw.indexOf(',') == sw.lastIndexOf(',')) {
                String[] parts1 = ne.split(",");
                String[] parts2 = sw.split(",");
                c1 = new Coordinate(Double.parseDouble(parts1[0]), Double.parseDouble(parts1[1]));
                c2 = new Coordinate(Double.parseDouble(parts2[0]), Double.parseDouble(parts2[1]));
            }
            
            if(c1 == null || c2 == null)
                return TransitResponseBuilder.getFailResponse("Failed to parse coordinates.");
            
            Envelope area = new Envelope(c1, c2);
            vehicles = vehicleLocationService.getForArea(area);
        } else {
            vehicles = vehicleLocationService.getAll();            
        }
        
        List<TransitVehicle> transitVehicles = new LinkedList<TransitVehicle>();
        for(VehicleLocation vehicle : vehicles) {
            if(vehicle.getTripId() != null) {
                builder.addToReferences(getTrip(transitIndexService, vehicle.getTripId()));
            }
            transitVehicles.add(builder.getVehicle(vehicle));
        }
        
        return builder.getResponseForVehicles(transitVehicles);
    }
    
    @GET
    @Path("/vehicle-details")
    @Produces({ MediaType.APPLICATION_JSON })
    public TransitResponse vehicle(@QueryParam("vehicleId") String id, @QueryParam("routerId") String routerId) throws JSONException {

        Graph graph = getGraph(routerId);
        VehicleLocationService vehicleLocationService = graph.getService(VehicleLocationService.class);
        AgencyAndId vehicleId = parseAgencyAndId(id);
        
        if(vehicleLocationService == null)
            return TransitResponseBuilder.getFailResponse("VehicleLocationService not found.");
        
        VehicleLocation vehicle = vehicleLocationService.getForVehicle(vehicleId);
        if(vehicle == null)
            return TransitResponseBuilder.getFailResponse("A vehicle of the given id doesn't exist.");
        
        if(vehicle.getTripId() != null)
            return tripDetails(vehicle.getTripId().toString(), vehicle.getServiceDate().getAsString(), routerId);
        else
            return TransitResponseBuilder.getResponseForVehicle(graph, vehicle);
    }
    
    @GET
    @Path("/vehicles-for-route")
    @Produces({ MediaType.APPLICATION_JSON })
    public TransitResponse vehiclesForRoute(@QueryParam("routeId") List<String> ids, @QueryParam("routerId") String routerId) throws JSONException {

        Graph graph = getGraph(routerId);
        TransitIndexService transitIndexService = graph.getService(TransitIndexService.class);
        if (transitIndexService == null) {
            return TransitResponseBuilder.getFailResponse(
                    "No transit index found.  Add TransitIndexBuilder to your graph builder configuration and rebuild your graph.");
        }
        
        VehicleLocationService vehicleLocationService = graph.getService(VehicleLocationService.class);
        if(vehicleLocationService == null)
            return TransitResponseBuilder.getFailResponse("VehicleLocationService not found.");
        
        TransitResponseBuilder builder = new TransitResponseBuilder(graph);
        
        List<TransitVehicle> transitVehicles = new ArrayList<TransitVehicle>();
        for(String id : ids) {
            AgencyAndId routeId = parseAgencyAndId(id);
            Route route = transitIndexService.getAllRoutes().get(routeId);
            if(route == null)
                return TransitResponseBuilder.getFailResponse("Unknown route.");
            transitVehicles.addAll(getTransitVehiclesForRoute(vehicleLocationService, routeId, transitIndexService, graph, builder));
        }
        
        return builder.getResponseForVehicles(transitVehicles);
    }
    
    @GET
    @Path("/vehicles-for-stop")
    @Produces({ MediaType.APPLICATION_JSON })
    public TransitResponse vehiclesForStop(@QueryParam("stopId") String id, @QueryParam("routerId") String routerId) throws JSONException {

        Graph graph = getGraph(routerId);        TransitIndexService transitIndexService = graph.getService(TransitIndexService.class);
        if (transitIndexService == null) {
            return TransitResponseBuilder.getFailResponse(
                    "No transit index found.  Add TransitIndexBuilder to your graph builder configuration and rebuild your graph.");
        }
        
        VehicleLocationService vehicleLocationService = graph.getService(VehicleLocationService.class);
        if(vehicleLocationService == null)
            return TransitResponseBuilder.getFailResponse("VehicleLocationService not found.");
        
        TransitResponseBuilder builder = new TransitResponseBuilder(graph);
        AgencyAndId stopId = parseAgencyAndId(id);
        Stop stop = transitIndexService.getAllStops().get(stopId);
        if(stop == null)
            return TransitResponseBuilder.getFailResponse("Unknown stop.");
        
        builder.addToReferences(stop);

        List<TransitVehicle> transitVehicles = new ArrayList<TransitVehicle>();
        for(AgencyAndId routeId : transitIndexService.getRoutesForStop(stopId)) {
            transitVehicles.addAll(getTransitVehiclesForRoute(vehicleLocationService, routeId, transitIndexService, graph, builder));
        }
        
        return builder.getResponseForVehicles(transitVehicles);
    }

    private List<TransitVehicle> getTransitVehiclesForRoute(VehicleLocationService vehicleLocationService, AgencyAndId routeId, TransitIndexService transitIndexService, Graph graph, TransitResponseBuilder builder) {
        List<VehicleLocation> vehicles = new ArrayList<VehicleLocation>(vehicleLocationService.getForRoute(routeId));

        List<TransitVehicle> transitVehicles = new ArrayList<TransitVehicle>(vehicles.size());
        for(VehicleLocation vehicle : vehicles) {
            Trip trip = getTrip(transitIndexService, vehicle.getTripId());
            TableTripPattern pattern = transitIndexService.getPatternForTrip(trip.getId());

            int tripIndex = pattern.getTripIndex(trip.getId());
            Timetable timetable;
            if(graph.timetableSnapshotSource != null && graph.timetableSnapshotSource.getSnapshot() != null) {
                timetable = graph.timetableSnapshotSource.getSnapshot().resolve(pattern, vehicle.getServiceDate());
            } else {
                timetable = pattern.getScheduledTimetable();
            }
            
            if(vehicle.getStopId() != null) {
                Stop vehicleStop = transitIndexService.getAllStops().get(vehicle.getStopId());
                builder.addToReferences(vehicleStop);
            }

            TransitTrip transitTrip = builder.getTrip(trip);
            transitTrip.setWheelchairAccessible(timetable.isWheelchairAccessible(tripIndex));
            builder.addToReferences(transitTrip);

            TransitVehicle transitVehicle = builder.getVehicle(vehicle);
            transitVehicle.setTripId(transitTrip.getId());
            transitVehicles.add(transitVehicle);
        }
        
        return transitVehicles;
    }

    protected List<String> getAlertsForStop(TransitResponseBuilder builder, AgencyAndId stopId,
            RoutingRequest options, long startTime, long endTime) {
        
        List<String> alertIds = new LinkedList<String>();
        if(patchService != null) {
            Collection<Patch> patches = patchService.getStopPatches(stopId);
            for(Patch patch : patches) {
                if(patch.activeDuring(options, startTime, endTime)) {
                    Alert alert = patch.getAlert();
                    if(alert != null) {
                        builder.addToReferences(alert);
                        alertIds.add(alert.alertId.toString());
                    }
                }
            }
        }
        return alertIds;
    }

    protected List<String> getAlertsForRoute(TransitResponseBuilder builder, AgencyAndId routeId, RoutingRequest options, long startTime, long endTime) {
        List<String> alertIds = new LinkedList<String>();
        if(patchService != null) {
            Collection<Patch> patches = patchService.getRoutePatches(routeId);
            for(Patch patch : patches) {
                if(patch.activeDuring(options, startTime, endTime)) {
                    Alert alert = patch.getAlert();
                    if(alert != null) {
                        builder.addToReferences(alert);
                        alertIds.add(alert.alertId.toString());
                    }
                }
            }
        }
        return alertIds;
    }
    
    // -- -- -- | -- -- -- //
    
    private AgencyAndId parseAgencyAndId(String fullId) {
        if(!fullId.contains("_"))
            return null;
        String[] parts = fullId.split("_", 2);
        return new AgencyAndId(parts[0], parts[1]);
    }

    private Trip getTrip(TransitIndexService transitIndexService, AgencyAndId tripId) {
        TableTripPattern pattern = transitIndexService.getPatternForTrip(tripId);
        int tripIndex = pattern.getTripIndex(tripId);
        Trip trip = pattern.getTrip(tripIndex);
        return trip;
    }

    private void sortStopTimesWithTrips(List<T2<TransitScheduleStopTime, TransitTrip>> stopTimes) {
        Collections.sort(stopTimes, new Comparator<T2<TransitScheduleStopTime, TransitTrip>>() {

            @Override
            public int compare(T2<TransitScheduleStopTime, TransitTrip> t2a, T2<TransitScheduleStopTime, TransitTrip> t2b) {
                TransitScheduleStopTime a = t2a.getFirst();
                TransitScheduleStopTime b = t2b.getFirst();
                
                return SCHDULE_COMPARATOR.compare(a, b);
            }
        });
    }

    private void sortStopTimes(List<TransitScheduleStopTime> stopTimes) {
        Collections.sort(stopTimes, SCHDULE_COMPARATOR);
    }
    private static class TransitScheduleStopTimeComparator implements Comparator<TransitScheduleStopTime> {
        @Override
        public int compare(TransitScheduleStopTime a, TransitScheduleStopTime b) {
            long ret;

            if(a.getPredictedDepartureTime() != null && b.getPredictedDepartureTime() != null) {
                ret = a.getPredictedDepartureTime() - b.getPredictedDepartureTime();
                if(ret != 0)
                    return (int) ret;
            }

            if(a.getPredictedArrivalTime() != null && b.getPredictedArrivalTime() != null) {
                ret = a.getPredictedArrivalTime() - b.getPredictedArrivalTime();
                if(ret != 0)
                    return (int) ret;
            }

            if(a.getDepartureTime() != null && b.getDepartureTime() != null) {
                ret = a.getDepartureTime() - b.getDepartureTime();
                if(ret != 0)
                    return (int) ret;
            }

            if(a.getArrivalTime() != null && b.getArrivalTime() != null) {
                ret = a.getArrivalTime() - b.getArrivalTime();
                if(ret != 0)
                    return (int) ret;
            }

            return a.getTripId().compareTo(b.getTripId());
        }
    }
    
    private static TransitScheduleStopTimeComparator SCHDULE_COMPARATOR = new TransitScheduleStopTimeComparator();
}
