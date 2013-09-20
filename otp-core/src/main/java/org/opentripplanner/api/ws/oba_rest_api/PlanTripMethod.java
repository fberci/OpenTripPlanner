package org.opentripplanner.api.ws.oba_rest_api;

import com.sun.jersey.api.core.InjectParam;
import com.sun.jersey.api.spring.Autowire;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;
import lombok.Setter;
import org.codehaus.jettison.json.JSONException;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Route;
import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.model.Trip;
import org.onebusaway.gtfs.model.calendar.ServiceDate;
import org.opentripplanner.api.common.Message;
import org.opentripplanner.api.common.RoutingResource;
import org.opentripplanner.api.model.Itinerary;
import org.opentripplanner.api.model.Leg;
import org.opentripplanner.api.model.Place;
import org.opentripplanner.api.model.TripPlan;
import org.opentripplanner.api.model.error.PlannerError;
import org.opentripplanner.api.ws.PlanGenerator;
import org.opentripplanner.api.ws.Planner;
import org.opentripplanner.api.ws.Response;
import org.opentripplanner.api.ws.oba_rest_api.beans.TransitResponse;
import org.opentripplanner.api.ws.oba_rest_api.beans.TransitResponseBuilder;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.edgetype.TableTripPattern;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.patch.Alert;
import org.opentripplanner.routing.services.TransitIndexService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements the {@code plan-trip} OneBusAway API method. Is in no way compatible with the original method.
 * 
 * @see http://developer.onebusaway.org/modules/onebusaway-application-modules/current/api/where/methods/agency.html
 */

@Autowire
@Produces({ MediaType.APPLICATION_JSON })
@Path(OneBusAwayApiMethod.API_BASE_PATH + "plan-trip" + OneBusAwayApiMethod.API_CONTENT_TYPE)
public class PlanTripMethod extends RoutingResource {

    private static final Logger LOG = LoggerFactory.getLogger(PlanTripMethod.class);
    
    @Setter @InjectParam 
    public PlanGenerator planGenerator;
    
    // We inject info about the incoming request so we can include the incoming query 
    // parameters in the outgoing response. This is a TriMet requirement.
    // Jersey seems to use @Context to inject internal types and @InjectParam or @Resource for DI objects.
    @Context UriInfo uriInfo;
    
    /** Java is immensely painful. TODO: Guava should cover this. */
    interface OneArgFunc<T,U> {
        public T call(U arg);
    }

    private static final Map<Message, TransitResponse.Status> errorMessageStatus = new EnumMap<Message, TransitResponse.Status>(Message.class);
    static {
        errorMessageStatus.put(Message.BOGUS_PARAMETER, TransitResponse.Status.BOGUS_PARAMETER);
        errorMessageStatus.put(Message.LOCATION_NOT_ACCESSIBLE, TransitResponse.Status.LOCATION_NOT_ACCESSIBLE);
        errorMessageStatus.put(Message.NO_TRANSIT_TIMES, TransitResponse.Status.NO_TRANSIT_TIMES);
        errorMessageStatus.put(Message.OUTSIDE_BOUNDS, TransitResponse.Status.OUTSIDE_BOUNDS);
        errorMessageStatus.put(Message.PATH_NOT_FOUND, TransitResponse.Status.PATH_NOT_FOUND);
        errorMessageStatus.put(Message.PLAN_OK, TransitResponse.Status.OK);
        errorMessageStatus.put(Message.REQUEST_TIMEOUT, TransitResponse.Status.REQUEST_TIMEOUT);
        errorMessageStatus.put(Message.SYSTEM_ERROR, TransitResponse.Status.UNKNOWN_ERROR);
        errorMessageStatus.put(Message.TOO_CLOSE, TransitResponse.Status.TOO_CLOSE);
    }
    
    @PathParam("dialect") private TransitResponseBuilder.Dialect dialect;
    @QueryParam("routerId") private String routerId;
    @QueryParam("includeReferences") @DefaultValue("true") private boolean references;
    
    @GET
    public TransitResponse plan() {

        Graph graph = getGraph(routerId);
        if(graph == null) {
            return TransitResponseBuilder.getFailResponse(TransitResponse.Status.ERROR_NO_GRAPH);
        }
        
        TransitIndexService transitIndexService = graph.getService(TransitIndexService.class);
        if (transitIndexService == null) {
            return TransitResponseBuilder.getFailResponse(TransitResponse.Status.ERROR_TRANSIT_INDEX_SERVICE);
        }
        
        try {
            TransitResponseBuilder builder = new TransitResponseBuilder(graph, references, dialect);
            Response plan = getItineraries();
            
            if(plan.getError() != null) {
                PlannerError error = plan.getError();
                TransitResponse.Status status = TransitResponse.Status.UNKNOWN_ERROR;
                if(error.getMessage() != null && errorMessageStatus.containsKey(error.getMessage())) {
                    status = errorMessageStatus.get(error.getMessage());
                }
                return builder.getResponseForErrorTripPlan(status, plan);
            }
            
            TripPlan tripPlan = plan.getPlan();
            if(tripPlan.itinerary != null) {
                for(Itinerary itinerary : tripPlan.itinerary) {
                    if(itinerary.legs == null) continue;

                    for(Leg leg : itinerary.legs) {
                        if(leg.tripId != null) {
                            AgencyAndId tripId = new AgencyAndId(leg.agencyId, leg.tripId);
                            Trip trip = getTrip(transitIndexService, tripId, ServiceDate.parseString(leg.serviceDate));
                            leg.tripId = tripId.toString();
                            builder.addToReferences(trip);
                        }
                        if(leg.routeId != null) {
                            AgencyAndId routeId = new AgencyAndId(leg.agencyId, leg.routeId);
                            Route route = transitIndexService.getAllRoutes().get(routeId);
                            leg.routeId = routeId.toString();
                            builder.addToReferences(route);
                        }
                        if(leg.from.stopId != null) {
                            Stop stop = transitIndexService.getAllStops().get(leg.from.stopId);
                            builder.addToReferences(stop);
                        }
                        if(leg.to.stopId != null) {
                            Stop stop = transitIndexService.getAllStops().get(leg.to.stopId);
                            builder.addToReferences(stop);
                        }
                        if(leg.alerts != null) {
                            for(Alert alert : leg.alerts) {
                                if(alert.alertId != null)
                                    builder.addToReferences(alert);
                            }
                        }
                        if(leg.stop != null) {
                            for(Place place : leg.stop) {
                                Stop stop = transitIndexService.getAllStops().get(place.stopId);
                                builder.addToReferences(stop);
                            }
                        }
                    }
                }
            }

            return builder.getResponseForTripPlan(plan);
        } catch(Exception e) {
            LOG.warn("Trip Planning Exception: ", e);
            return TransitResponseBuilder.getFailResponse(TransitResponse.Status.UNKNOWN_ERROR, "An error occured: " + e.getClass().getName(), e);
        }
    }
    
    private Trip getTrip(TransitIndexService transitIndexService, AgencyAndId tripId, ServiceDate serviceDate) {
        TableTripPattern pattern = transitIndexService.getTripPatternForTrip(tripId);
        int tripIndex = pattern.getTripIndex(tripId);
        Trip trip = pattern.getTrip(tripIndex);
        // TODO: wheelchair accessibility
        return trip;
    }
    
    private Graph getGraph(String routerId) {
        try {
            return graphService.getGraph(routerId);
        } catch(Exception e) {
            return null;
        }
    }
    
    private Response wrapGenerate(OneArgFunc<TripPlan, RoutingRequest> func) {

        /*
         * TODO: add Lang / Locale parameter, and thus get localized content (Messages & more...)
         * TODO: from/to inputs should be converted / geocoded / etc... here, and maybe send coords 
         *       or vertex ids to planner (or error back to user)
         * TODO: org.opentripplanner.routing.impl.PathServiceImpl has COOORD parsing. Abstract that
         *       out so it's used here too...
         */
        
        // create response object, containing a copy of all request parameters
        Response response = new Response(uriInfo);
        RoutingRequest request = null;
        try {
            // fill in request from query parameters via shared superclass method
            request = super.buildRequest();
            TripPlan plan = func.call(request);
            response.setPlan(plan);
        } catch (Exception e) {
            PlannerError error = new PlannerError(e);
            if(!PlannerError.isPlanningError(e.getClass()))
                LOG.warn("Error while planning path: ", e);
            response.setError(error);
        } finally {
            if (request != null) {
                response.debug = request.rctx.debug;
                request.cleanup(); // TODO verify that this is being done on Analyst web services
            }       
        }
        return response;
    }

    public Response getItineraries() throws JSONException {
        return wrapGenerate(new OneArgFunc<TripPlan, RoutingRequest>() {
            @Override
            public TripPlan call(RoutingRequest request) {
                return planGenerator.generate(request);
            }});
    }
}