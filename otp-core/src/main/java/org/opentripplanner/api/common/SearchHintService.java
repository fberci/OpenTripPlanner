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

package org.opentripplanner.api.common;

import com.google.common.collect.Sets;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Route;
import org.opentripplanner.graph_builder.services.GraphBuilder;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.services.TransitIndexService;
import org.opentripplanner.util.HttpUtils;
import org.opentripplanner.util.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SearchHintService implements Serializable {
    private static final long serialVersionUID = 1L;

    private Map<AgencyAndId, Set<AgencyAndId>> searchHintsForRoute;
    
    private SearchHintService(Map<AgencyAndId, Set<AgencyAndId>> searchHintsForRoute) {
        this.searchHintsForRoute = searchHintsForRoute;
    }
    
    public Collection<AgencyAndId> getHintsForRoute(AgencyAndId route) {
        if(!searchHintsForRoute.containsKey(route))
            return Collections.emptySet();
        
        return searchHintsForRoute.get(route);
    }

    public static class Builder implements GraphBuilder {
        private static final Logger LOG = LoggerFactory.getLogger(Builder.class);
        
        @Getter @Setter
        private String url = "http://www.bkk.hu/apps/bkkinfo/json.php?jlista=";
        private HttpUtils httpUtils;
        private JSONParser _parser = new JSONParser();
        private TransitIndexService transitIndexService;
        private Map<AgencyAndId, Set<AgencyAndId>> searchHintsForRoute = new HashMap<AgencyAndId, Set<AgencyAndId>>();
        private Map<String, Set<AgencyAndId>> routesForShortName = new HashMap<String, Set<AgencyAndId>>();

        @Override
        public void buildGraph(Graph graph, HashMap<Class<?>, Object> extra) {
            httpUtils = new HttpUtils();
            transitIndexService = graph.getService(TransitIndexService.class);
            
            createRouteShortNameMappings();
            
            for(AgencyAndId routeId : transitIndexService.getAllRouteIds()) {
                createMappingForRoute(routeId);
            }
            
            httpUtils.cleanup();
            graph.putService(SearchHintService.class, new SearchHintService(searchHintsForRoute));
        }
        
        private void createMappingForRoute(AgencyAndId routeId) {
            Set<String> shortNames = getShortNamesForRoute(routeId);
            
            for(String shortName : shortNames) {
                for(AgencyAndId other : getRouteIdsForShortName(shortName)) {
                    MapUtils.addToMapSet(searchHintsForRoute, other, routeId);
                }
            }
        }
        
        private Set<String> getShortNamesForRoute(AgencyAndId routeId) {
            Set<String> ret = Collections.emptySet();
            
            InputStream is = null;
            try {
                is = httpUtils.getData(url + routeId.getId());
                
                InputStreamReader content = new InputStreamReader(is, "UTF-8");
                JSONObject routeDetailsOuter = (JSONObject) _parser.parse(content);
                if(routeDetailsOuter != null) {
                    for(Object key : routeDetailsOuter.keySet()) {
                        JSONObject routeDetails = (JSONObject) routeDetailsOuter.get(key);
                        routeDetails = (JSONObject) routeDetails.get("adatok");
                        if(routeDetails.containsKey("kulcs")) {
                            String kulcs = routeDetails.get("kulcs").toString();
                            String[] routeShortNames = kulcs.split(" ");
                            ret = Sets.newHashSet(routeShortNames);
                        } else {
                            LOG.warn("Search hint not provided for route " + routeId);
                        }
                    }
                } else {
                    LOG.warn("Search hint not found for route " + routeId);
                }
            } catch(Exception e) {
                LOG.warn("Something went wrong...: ", e);
            } finally {
                if(is != null) {
                    try { is.close(); } catch(Exception e) {}
                }
            }
            
            return ret;
        }
        
        private Set<AgencyAndId> getRouteIdsForShortName(String shortName) {
            if(!routesForShortName.containsKey(shortName.toLowerCase()))
                return Collections.emptySet();
            
            return routesForShortName.get(shortName.toLowerCase());
        }
        
        private void createRouteShortNameMappings() {
            for(Route route : transitIndexService.getAllRoutes().values()) {
                if(route.getShortName() != null) {
                    MapUtils.addToMapSet(routesForShortName, route.getShortName().toLowerCase(), route.getId());
                }
            }
        }

        @Override
        public List<String> provides() {
            return Collections.emptyList();
        }

        @Override
        public List<String> getPrerequisites() {
            return Arrays.asList("transitIndex");
        }

        @Override
        public void checkInputs() { }
        
    }
}
