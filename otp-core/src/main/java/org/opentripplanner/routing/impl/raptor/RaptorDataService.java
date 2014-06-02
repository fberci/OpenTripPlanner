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

package org.opentripplanner.routing.impl.raptor;

import lombok.Getter;
import org.onebusaway.gtfs.model.Stop;
import org.opentripplanner.routing.edgetype.TransitBoardAlight;
import org.opentripplanner.routing.edgetype.TripPattern;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.services.TransitIndexService;
import org.opentripplanner.routing.vertextype.TransitStop;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

public class RaptorDataService implements Serializable {
    private static final long serialVersionUID = -5046519968713244930L;

    @Getter
    private RaptorData data;

    public RaptorDataService(RaptorData data) {
        this.data = data;
    }

    public boolean addTripPattern(Graph graph, TripPattern pattern) {
        RaptorRoute raptorRoute = data.getRoute(pattern);
        if(raptorRoute == null) {
            return false;
        }

        if(raptorRoute.containsPattern(pattern)) {
            return true;
        }

        RaptorRoute newRaptorRoute = new RaptorRoute(raptorRoute.getNStops(), raptorRoute.boards.length);
        newRaptorRoute.mode = raptorRoute.mode;
        newRaptorRoute.stops = raptorRoute.stops;

        RaptorData newData = new RaptorData();
        newData.stops = data.stops;
        newData.raptorStopsForStopId = data.raptorStopsForStopId;
        newData.regionData = data.regionData;
        newData.nearbyStops = data.nearbyStops;
        newData.maxTransitRegions = data.maxTransitRegions;

        newData.routes = new HashSet<RaptorRoute>(data.routes);
        newData.routes.remove(raptorRoute);
        newData.routes.add(newRaptorRoute);

        newData.routesForStop = new List[data.routesForStop.length];
        System.arraycopy(data.routesForStop, 0, newData.routesForStop, 0, data.routesForStop.length);

        for(int i = 0; i < newRaptorRoute.stops.length; ++i) {
            RaptorStop raptorStop = newRaptorRoute.stops[i];

            if(i + 1 < newRaptorRoute.stops.length) {
                newRaptorRoute.boards[i] = new TransitBoardAlight[raptorRoute.boards[i].length + 1];
                System.arraycopy(raptorRoute.boards[i], 0, newRaptorRoute.boards[i], 0, raptorRoute.alights[i].length);
                newRaptorRoute.boards[i][raptorRoute.boards[i].length] = transitBoardForStop(pattern, newRaptorRoute.stops[i].stopVertex, graph);
                if(newRaptorRoute.boards[i][raptorRoute.alights[i].length] == null) {
                    return false;
                }
            }

            if(i > 0) {
                newRaptorRoute.alights[i-1] = new TransitBoardAlight[raptorRoute.alights[i-1].length + 1];
                System.arraycopy(raptorRoute.alights[i-1], 0, newRaptorRoute.alights[i-1], 0, raptorRoute.alights[i-1].length);
                newRaptorRoute.alights[i-1][raptorRoute.alights[i-1].length] = transitAlightForStop(pattern, newRaptorRoute.stops[i].stopVertex, graph);
                if(newRaptorRoute.alights[i-1][raptorRoute.alights[i-1].length] == null) {
                    return false;
                }
            }

            newData.routesForStop[raptorStop.index] = new ArrayList<RaptorRoute>(newData.routesForStop[raptorStop.index]);
            for(int j = 0; j < newData.routesForStop[raptorStop.index].size(); ++j) {
                RaptorRoute stopRoute = (RaptorRoute) newData.routesForStop[raptorStop.index].get(j);
                if(stopRoute == raptorRoute) {
                    newData.routesForStop[raptorStop.index].set(j, newRaptorRoute);
                    break;
                }
            }
        }

        this.data = newData;
        return true;
    }

    private TransitBoardAlight transitBoardForStop(TripPattern pattern, TransitStop stopVertex, Graph graph) {
        return transitBoardAlightForStop(pattern, stopVertex.getStop(), true, graph.getService(TransitIndexService.class));
    }

    private TransitBoardAlight transitAlightForStop(TripPattern pattern, TransitStop stopVertex, Graph graph) {
        return transitBoardAlightForStop(pattern, stopVertex.getStop(), false, graph.getService(TransitIndexService.class));
    }

    private TransitBoardAlight transitBoardAlightForStop(TripPattern pattern, Stop stop, boolean isBoarding, TransitIndexService transitIndexService) {
        Collection<Edge> edges;
        if(isBoarding) {
            edges = transitIndexService.getPreBoardEdge(stop.getId()).getToVertex().getOutgoing();
        } else {
            edges = transitIndexService.getPreAlightEdge(stop.getId()).getFromVertex().getIncoming();
        }

        for(Edge e : edges) {
            if(e instanceof TransitBoardAlight && ((TransitBoardAlight) e).getPattern().equals(pattern)) {
                return (TransitBoardAlight) e;
            }
        }

        return null;
    }
}
