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

import com.fasterxml.jackson.databind.util.ISO8601Utils;
import java.util.Calendar;
import java.util.Date;
import javax.ws.rs.Path;
import org.onebusaway.gtfs.model.calendar.ServiceDate;
import org.opentripplanner.api.ws.oba_rest_api.beans.TransitMetadata;
import org.opentripplanner.api.ws.oba_rest_api.beans.TransitResponse;

/**
 * Implements the {@code current-time} OneBusAway API method. Also returns the validity of the currently
 * used schedules.
 * 
 * @see http://developer.onebusaway.org/modules/onebusaway-application-modules/current/api/where/methods/current-time.html
 */

@Path(OneBusAwayApiMethod.API_BASE_PATH + "{acme : metadata|current-time}" + OneBusAwayApiMethod.API_CONTENT_TYPE)
public class MetadataMethod extends OneBusAwayApiMethod {

    @Override
    protected TransitResponse getResponse() {
        
        Date now = new Date();
        
        Calendar start = Calendar.getInstance(graph.getTimeZone());
        start.setTimeInMillis(graph.getTransitServiceStarts() * 1000);
        
        Calendar end = Calendar.getInstance(graph.getTimeZone());
        end.setTimeInMillis((graph.getTransitServiceEnds() - 1) * 1000);
        
        TransitMetadata metadata = new TransitMetadata();
        metadata.setTime(now.getTime());
        metadata.setReadableTime(ISO8601Utils.format(now));
        metadata.setValidityStart(new ServiceDate(start).getAsString());
        metadata.setValidityEnd(new ServiceDate(end).getAsString());
        
        return responseBuilder.getResponseForMetadata(metadata);
    }
}
