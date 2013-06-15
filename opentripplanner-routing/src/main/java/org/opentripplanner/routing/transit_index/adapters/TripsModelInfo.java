/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (props, at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.routing.transit_index.adapters;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import lombok.Getter;

import org.onebusaway.gtfs.model.AgencyAndId;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@XmlRootElement(name = "trip")
public class TripsModelInfo implements Serializable {

    private static final long serialVersionUID = -4853941297409355512L;

    public TripsModelInfo(String headsign, Integer number, String calendarId, AgencyAndId agencyId) {
        this.headsign = headsign;
        this.numberOfTrips = number;
        this.calendarId = calendarId;
        this.id = agencyId.getId();
        this.agency = agencyId.getAgencyId();
    }

    public TripsModelInfo() {
    }

    @Getter
    @XmlAttribute
    @JsonSerialize
    String headsign;

    @Getter
    @XmlAttribute
    @JsonSerialize
    Integer numberOfTrips;

    @Getter
    @XmlAttribute
    @JsonSerialize
    String calendarId;

    @Getter
    @XmlAttribute
    @JsonSerialize
    String id;

    @Getter
    @XmlAttribute
    @JsonSerialize
    String agency;
}
