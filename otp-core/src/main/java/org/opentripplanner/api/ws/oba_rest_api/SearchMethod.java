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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Route;
import org.onebusaway.gtfs.model.Stop;
import org.opentripplanner.api.ws.oba_rest_api.beans.TransitResponse;
import org.opentripplanner.api.ws.oba_rest_api.beans.TransitResponseBuilder;
import org.opentripplanner.routing.services.PatchService;
import org.opentripplanner.routing.services.TransitIndexService;

@Path(OneBusAwayApiMethod.API_BASE_PATH + "search" + OneBusAwayApiMethod.API_CONTENT_TYPE)
public class SearchMethod extends AbstractSearchMethod {

    @QueryParam("query") private String query;
    
    @Override
    protected TransitResponse getResponse() {
        PatchService patchService = graph.getService(PatchService.class);
        
        if(query == null || query.length() == 0) {
            return responseBuilder.getResponseForSearch(query, Collections.EMPTY_LIST, Collections.EMPTY_LIST,
                                                                               Collections.EMPTY_LIST);
        }
        
        String normalizedQuery = normalize(query);
        List<String> stopIds  = getMatchingStops(normalizedQuery);
        List<String> routeIds = getMatchingRoutes(normalizedQuery);
        List<String> alertIds = getMatchingAlerts(patchService, stopIds, routeIds);
        
        return responseBuilder.getResponseForSearch(query, stopIds, routeIds, alertIds);
    }
    
    private List<String> getMatchingRoutes(String query) {
        if(!query.matches("[a-zA-Z0-9-]*"))
            return Collections.emptyList();
        
        List<Route> routes = new LinkedList<Route>();
        for(Route route : transitIndexService.getAllRoutes().values()) {
            if(route.getShortName() != null
                    && route.getShortName().toLowerCase().matches("" + query + "(\\D.*|)$")) {
                routes.add(route);
            }
            else if(route.getShortName() != null
                    && query.toUpperCase().matches("[MHD]")
                    && route.getShortName().toUpperCase().startsWith(query.toUpperCase())) {
                routes.add(route);
            }
            else if(route.getShortName() != null
                    && route.getShortName().toLowerCase().matches(".*-" + query + "(\\D.*|)$")) {
                routes.add(route);
            }
            /*else if(route.getDesc() != null && route.getDesc().contains(query)) {
                routes.add(route);
            }
            else if(route.getLongName() != null && route.getLongName().contains(query)) {
                routes.add(route);
            }*/
        }
        
        Collections.sort(routes, TransitResponseBuilder.ROUTE_COMPARATOR);
        
        List<String> ret = new ArrayList<String>(routes.size());
        for(Route route : routes) {
            responseBuilder.addToReferences(route);
            ret.add(route.getId().toString());
        }
        
        return ret;
    }

    private List<String> getMatchingAlerts(PatchService patchService, List<String> stopIds, List<String> routeIds) {
        
        Set<String> alertIds = new HashSet<String>();
        if(stopIds != null) {
            for(String sStopId : stopIds) {
                AgencyAndId stopid = parseAgencyAndId(sStopId);
                alertIds.addAll(getAlertsForStop(patchService, stopid));
            }
        }
        if(routeIds != null) {
            for(String sRouteId : routeIds) {
                AgencyAndId routeId = parseAgencyAndId(sRouteId);
                alertIds.addAll(getAlertsForRoute(patchService, routeId));
            }
        }
        
        return new ArrayList<String>(alertIds);
    }

    private List<String> getMatchingStops(String query) {
        if(query.length() < 3)
            return null;
        
        List<StopIndex> matchedStops = new LinkedList<StopIndex>();
        for(Stop stop : transitIndexService.getAllStops().values()) {

            String realStopName = normalize(stop.getName());
            String fullStopName = realStopName.replaceAll(" (?:m\\+h|m|h|p+r)$", "");
            String stopName = fullStopName.replace(" \\(.*$", "");
            String subStopName = fullStopName.replace(".*? \\(", "").replaceAll("\\)$", "");
            String[] words = fullStopName.replaceAll("[()]", "").split(" ");
            
            List<StopIndex> matches = new LinkedList<StopIndex>();
            if(realStopName.contains(query)) {
                matches.add(scoreStopMatch(transitIndexService, realStopName, query, stop));
            }
            if(fullStopName.contains(query)) {
                matches.add(scoreStopMatch(transitIndexService, fullStopName, query, stop));
            }
            if(stopName.contains(query)) {
                matches.add(scoreStopMatch(transitIndexService, stopName, query, stop));
            }
            if(subStopName.contains(query)) {
                matches.add(scoreStopMatch(transitIndexService, subStopName, query, stop));
            }
            if(stop.getCode() != null && stop.getCode().toLowerCase().equals(query)) {
                matches.add(new StopIndex(100, stop, null));
            }
            if(stop.getDesc()!= null && stop.getLocationType() == 0 && stop.getDesc().toLowerCase().equals(query)) {
                matches.add(new StopIndex(100, stop, null));
            }
            if(stop.getCode() != null && stop.getCode().toLowerCase().equals(query)) {
                matches.add(new StopIndex(100, stop, null));
            }
            
            for(String word : words) {
                if(word.contains(query)) {
                    matches.add(scoreStopMatch(transitIndexService, wordMultiplier, word, query, stop));
                }
            }
            
            if(!matches.isEmpty()) {
                Collections.sort(matches, StopIndex.COMPARATOR);
                matchedStops.add(matches.get(0));
            }
        }
        
        Collections.sort(matchedStops, StopIndex.COMPARATOR);

        List<String> ret = new LinkedList<String>();
        for(StopIndex stopIndex : matchedStops) {
            if(stopIndex.getScore() < MIN_SCORE && ret.size() > MIN_RESULTS)
                continue;
            
            responseBuilder.addToReferences(stopIndex.getStop());
            ret.add(stopIndex.getStop().getId().toString());
        }
        
        return ret;
    }
    
    // TODO: nincsenek megalapozva a számok méretei
    private int    MIN_SCORE       = 40;
    private int    MIN_RESULTS     =  5;
    private float  wordMultiplier  = 0.60F,
                   railMultiplier  = 0.10F,
                   tramMultiplier  = 0.07F,
                   otherMultiplier = 0.03F;
    
    private StopIndex scoreStopMatch(TransitIndexService transitIndexService, String name, String query, Stop stop) {
        return scoreStopMatch(transitIndexService, 1.0F, name, query, stop);
    }
    
    private StopIndex scoreStopMatch(TransitIndexService transitIndexService, float multiplier, String name, String query, Stop stop) {
        
        float score = 100F * ((float) query.length() / (float) name.length()); // minél nagyobb részre illeszkedik annál jobb         
       
        LinkedList<Route> routes = new LinkedList<Route>();
        for(AgencyAndId routeId : transitIndexService.getRoutesForStop(stop.getId())) {
            Route route = transitIndexService.getAllRoutes().get(routeId);
            routes.add(route);
            
            if(route.getType() == 1 || route.getType() == 2) {
                multiplier += railMultiplier;
            }
            else if (route.getType() == 0) {
                multiplier += tramMultiplier;
            }
            else {
                multiplier += otherMultiplier;
            }
        }
        
        score *= multiplier;
        
        Collections.sort(routes, TransitResponseBuilder.ROUTE_COMPARATOR);
        return new StopIndex((int) score, stop, routes);
    }
    
    @Data
    @AllArgsConstructor
    static class StopIndex {
        private int score;
        private Stop stop;
        private LinkedList<Route> routes;
        
        static Comparator<StopIndex> COMPARATOR = new Comparator<StopIndex>() {
            @Override
            public int compare(StopIndex a, StopIndex b) {
                int ret = b.getScore() - a.getScore();
                if(ret != 0)
                    return ret;
                
                LinkedList<Route> ra = a.getRoutes();
                LinkedList<Route> rb = b.getRoutes();
                
                if(ra != null && rb != null) {
                    ret = rb.size() - ra.size();
                    if(ret != 0)
                        return ret;
                    
                    if(!ra.equals(rb)) {
                        while(ret == 0 && !(ra.isEmpty() || rb.isEmpty())) {
                            ret = TransitResponseBuilder.ROUTE_COMPARATOR.compare(rb.pop(), ra.pop());
                        }

                        if(ret != 0)
                            return ret;
                    }
                }
                
                return b.getStop().getId().toString().compareTo(a.getStop().getId().toString());
            }
        };
    }

}
