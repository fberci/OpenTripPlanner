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

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.opentripplanner.routing.patch.Alert;
import org.opentripplanner.routing.patch.Patch;
import org.opentripplanner.routing.services.PatchService;

public abstract class AbstractSearchMethod extends OneBusAwayApiMethod {
    
    protected String normalize(String html) {
        String egyszeru = html.toLowerCase().replaceAll("(?i)<[^>]*>", " ").replaceAll("\\s+", " ").trim();
        egyszeru = Normalizer.normalize(egyszeru, Normalizer.Form.NFD).replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return egyszeru; 
    }

    protected List<String> getAlertsForStop(PatchService patchService, AgencyAndId stopId) {
        
        Set<String> alertIds = new HashSet<String>();
        
        long currentTime = System.currentTimeMillis() / 1000;
        if(patchService != null) {
            Collection<Patch> patches = patchService.getStopPatches(stopId);
            for(Patch patch : patches) {
                if(patch.activeDuring(null, currentTime, Long.MAX_VALUE)) {
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

    protected List<String> getAlertsForRoute(PatchService patchService, AgencyAndId routeId) {
        Set<String> alertIds = new HashSet<String>();
        
        long currentTime = System.currentTimeMillis() / 1000;
        if(patchService != null) {
            Collection<Patch> patches = patchService.getRoutePatches(routeId);
            for(Patch patch : patches) {
                if(patch.activeDuring(null, currentTime, Long.MAX_VALUE)) {
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
}
