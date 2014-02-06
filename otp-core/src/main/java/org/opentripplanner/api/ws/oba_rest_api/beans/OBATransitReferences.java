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

import lombok.Data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import java.util.Collection;

@Data
@XmlAccessorType(XmlAccessType.FIELD)
public class OBATransitReferences extends TransitReferences {

	@XmlElementWrapper(name = "agencies")
	@XmlElement(name = "agency")
    private final Collection<TransitAgency> agencies;

	@XmlElementWrapper(name = "routes")
	@XmlElement(name = "route")
    private final Collection<TransitRoute> routes;

	@XmlElementWrapper(name = "stops")
	@XmlElement(name = "stop")
    private final Collection<TransitStop> stops;

	@XmlElementWrapper(name = "trips")
	@XmlElement(name = "trip")
    private final Collection<TransitTrip> trips;

	@XmlElementWrapper(name = "situations")
	@XmlElement(name = "situation")
    private final Collection<TransitSituation> situations;
}
