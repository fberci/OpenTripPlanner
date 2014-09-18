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

package org.opentripplanner.updater.bike_rental;

import org.apache.commons.lang.StringUtils;
import org.opentripplanner.routing.bike_rental.BikeRentalStation;
import org.opentripplanner.routing.bike_rental.BikeRentalStationService;

import java.util.Map;

public class NextBikeBikeRentalDataSource extends GenericXmlBikeRentalDataSource {
    public NextBikeBikeRentalDataSource() {
        super("//place");
    }

    public BikeRentalStation makeStation(Map<String, String> attributes) {
        if(!(attributes.containsKey("uid") && attributes.containsKey("lat") && attributes.containsKey("lng")
            && attributes.containsKey("name") && attributes.containsKey("bikes") && attributes.containsKey("bike_racks")))
        {
            return null;
        }

        BikeRentalStation bikeRentalStation = new BikeRentalStation();
        bikeRentalStation.id = attributes.get("uid");
        bikeRentalStation.x = Double.parseDouble(attributes.get("lng"));
        bikeRentalStation.y = Double.parseDouble(attributes.get("lat"));
		bikeRentalStation.code = attributes.get("name").substring(0, 4);
        bikeRentalStation.name = attributes.get("name").substring(5);
		bikeRentalStation.type = attributes.get("terminal_type");
        bikeRentalStation.bikesAvailable = Integer.parseInt(attributes.get("bikes"));
        bikeRentalStation.spacesAvailable = Math.max(0, Integer.parseInt(attributes.get("bike_racks")) - bikeRentalStation.bikesAvailable);

		if(!StringUtils.isEmpty(bikeRentalStation.code)) {
			BikeRentalStationService bikeRentalStationService = getGraph().getService(BikeRentalStationService.class);
			BikeRentalStation referenceStation = bikeRentalStationService.getReference().get(bikeRentalStation.code);
			if(referenceStation != null) {
				bikeRentalStation.x = referenceStation.x;
				bikeRentalStation.y = referenceStation.y;
			}
		}

        return bikeRentalStation;
    }
}
