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

import org.opentripplanner.routing.bike_rental.BikeRentalStation;

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

        BikeRentalStation brstation = new BikeRentalStation();
        brstation.id = attributes.get("uid");
        brstation.x = Double.parseDouble(attributes.get("lng"));
        brstation.y = Double.parseDouble(attributes.get("lat"));
        brstation.name = attributes.get("name");
        brstation.bikesAvailable = Integer.parseInt(attributes.get("bikes"));
        brstation.spacesAvailable = Integer.parseInt(attributes.get("bike_racks")) - brstation.bikesAvailable;
        return brstation;
    }
}
