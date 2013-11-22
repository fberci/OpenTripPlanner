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

import com.vividsolutions.jts.geom.Coordinate;
import org.opentripplanner.api.ws.oba_rest_api.beans.TransitAgencyWithCoverage;
import org.opentripplanner.api.ws.oba_rest_api.beans.TransitListEntryWithReferences;
import org.opentripplanner.api.ws.oba_rest_api.beans.TransitResponse;

import javax.ws.rs.Path;
import java.util.LinkedList;
import java.util.List;

/**
 * Implements the <a href="http://developer.onebusaway.org/modules/onebusaway-application-modules/current/api/where/methods/agencies-with-coverage.html">agencies-with-coverage</a> OneBusAway API method.
 * <br />
 * The returned agencies all have the same coverage area currently.
 */
@Path(OneBusAwayApiMethod.API_BASE_PATH + "agencies-with-coverage" + OneBusAwayApiMethod.API_CONTENT_TYPE)
public class AgenciesWithCoverageMethod extends OneBusAwayApiMethod<TransitListEntryWithReferences<TransitAgencyWithCoverage>> {
    private static final Double DEFAULT_SPAN = 0.7;

    @Override
    protected TransitResponse<TransitListEntryWithReferences<TransitAgencyWithCoverage>> getResponse() {
        List<String> agencies = transitIndexService.getAllAgencies();
        List<TransitAgencyWithCoverage> agenciesWithCoverage = new LinkedList<TransitAgencyWithCoverage>();
        
        Coordinate center = transitIndexService.getCenter();
        for(String agencyId : agencies) {
            TransitAgencyWithCoverage agencyWithCoverage = new TransitAgencyWithCoverage();
            agencyWithCoverage.setAgencyId(agencyId);
            agencyWithCoverage.setLat(center.x);
            agencyWithCoverage.setLon(center.y);
            agencyWithCoverage.setLatSpan(DEFAULT_SPAN);
            agencyWithCoverage.setLonSpan(DEFAULT_SPAN);
            
            agenciesWithCoverage.add(agencyWithCoverage);
	        responseBuilder.addToReferences(transitIndexService.getAgency(agencyId));
        }
        
        return responseBuilder.getResponseForAgenciesWithCoverage(agenciesWithCoverage);
    }
}
