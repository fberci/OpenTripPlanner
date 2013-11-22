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

package org.opentripplanner.api.ws.oba_rest_api.methods;

import org.opentripplanner.api.ws.oba_rest_api.beans.TransitEntryWithReferences;
import org.opentripplanner.api.ws.oba_rest_api.beans.TransitResponse;
import org.opentripplanner.api.ws.oba_rest_api.beans.TransitSearch;
import org.opentripplanner.routing.patch.Alert;
import org.opentripplanner.routing.patch.Patch;
import org.opentripplanner.routing.services.PatchService;

import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Path(OneBusAwayApiMethod.API_BASE_PATH + "alert-search" + OneBusAwayApiMethod.API_CONTENT_TYPE)
public class AlertSearchMethod extends AbstractSearchMethod<TransitEntryWithReferences<TransitSearch>> {

    @QueryParam("query") private String query;
    @QueryParam("start") private Long start;
    @QueryParam("end") private Long end;
    
    @Override
    protected TransitResponse<TransitEntryWithReferences<TransitSearch>> getResponse() {
        
        PatchService patchService = graph.getService(PatchService.class);
        String normalizedQuery = query == null ? null : normalize(query);
        List<String> alertIds = getMatchingAlerts(patchService, normalizedQuery, start, end);
        
        return responseBuilder.getResponseForSearch(query, alertIds);
    }

    private List<String> getMatchingAlerts(PatchService patchService, String query, Long start, Long end) {
        
        Set<String> alertIds = new HashSet<String>();
        if(patchService != null) {
            Collection<Patch> patches = patchService.getAllPatches();
            for(Patch patch : patches) {
                Alert alert = patch.getAlert();
                boolean dateMatched = true, queryMatched = true;
                
                if(start != null || end != null) {
                    dateMatched = false;
                    
                    if(start != null && end != null)
                        dateMatched |= patch.activeDuring(null, start, end);
                    else if(start != null)
                        dateMatched |= patch.activeDuring(null, start, Long.MAX_VALUE);
                    else if(end != null)
                        dateMatched |= patch.activeDuring(null, Long.MIN_VALUE, end);
                }
                
                if(query != null && query.length() > 0) {
                    queryMatched = false;
                    
                    if(!queryMatched && alert.alertHeaderText != null) {
                        queryMatched = normalize(alert.alertHeaderText.getSomeTranslation()).contains(query);
                    }

                    if(!queryMatched && alert.alertDescriptionText != null) {
                        queryMatched = normalize(alert.alertDescriptionText.getSomeTranslation()).contains(query);
                    }

                    if(!queryMatched && alert.alertDescriptionText != null && query.length() == 6) {
                        queryMatched = alert.alertDescriptionText.getSomeTranslation().contains(query.toUpperCase());
                    }

                    if(!queryMatched && alert.alertId != null) {
                        queryMatched = alert.alertId.toString().toLowerCase().equals(query);
                    }
                }
                
                if(queryMatched && dateMatched) {
                    responseBuilder.addToReferences(alert);
                    alertIds.add(alert.alertId.toString());
                }
            }
        }
        
        return new ArrayList<String>(alertIds);
    }
}
