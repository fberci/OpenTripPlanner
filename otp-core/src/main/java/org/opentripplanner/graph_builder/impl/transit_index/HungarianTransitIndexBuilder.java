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

package org.opentripplanner.graph_builder.impl.transit_index;

import java.util.HashMap;
import java.util.Map;
import org.onebusaway.gtfs.model.Stop;
import org.opentripplanner.routing.transit_index.RouteVariant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HungarianTransitIndexBuilder extends TransitIndexBuilder {
    private static final Logger LOG = LoggerFactory.getLogger(HungarianTransitIndexBuilder.class);
    
    static private Map<String, String> cardinalDirections = new HashMap<String, String>();
    {
        cardinalDirections.put("N" , "É"  );
        cardinalDirections.put("E" , "K"  );
        cardinalDirections.put("S" , "D"  );
        cardinalDirections.put("W" , "NY" );
        cardinalDirections.put("NE", "ÉK" );
        cardinalDirections.put("NW", "ÉNY");
        cardinalDirections.put("SE", "DK" );
        cardinalDirections.put("SW", "DNY");
    }

    @Override
    protected String getStopName(Stop stop) {
        return stop.getName(); // + " (" + stop.getId().getId() + ")";
    }

    @Override
    protected String getNameForVariantWithExpress(String routeName, String firstStop, String lastStop) {
        return routeNameInHungarian(routeName) + " " + firstStop + "-tól " + lastStop + "-ig";
    }

    @Override
    protected String getNameForVariantWithExpressAndVia(String routeName, String firstStop, String lastStop, String viaStop) {
        return routeNameInHungarian(routeName) + " " + firstStop + "-tól " + lastStop + "-ig " + viaStop + " betéréssel";
    }

    @Override
    protected String getNameForVariantWithFromTo(String routeName, String firstStop, String lastStop) {
        return routeNameInHungarian(routeName) + " " + firstStop + "-tól " + lastStop + "-ig";
    }

    @Override
    protected String getNameForVariantWithVia(String routeName, String viaStop) {
        return routeNameInHungarian(routeName) + " " + viaStop + " betéréssel";
    }

    @Override
    protected String getNameForVariantFallback(String routeName, RouteVariant variant) {
        return routeNameInHungarian(routeName) + " (" + variant.getId() + ")";
    }

    @Override
    protected String getNameForVariantWithTo(String routeName, String lastStop) {
        return routeNameInHungarian(routeName) + " " + lastStop + " felé";
    }

    @Override
    protected String getNameForVariantWithFrom(String routeName, String firstStop) {
        return routeNameInHungarian(routeName) + " " + firstStop + " indulással";
    }

    private String routeNameInHungarian(String rsn /*routeShortName*/) {
        if(rsn.endsWith("3") || rsn.endsWith("8") || rsn.endsWith("20") || rsn.endsWith("30") || rsn.endsWith("60")
			|| rsn.endsWith("80") || rsn.endsWith("100") || rsn.endsWith("200")) {
            return rsn + "-as";
	}
	else if(rsn.endsWith("5")) {
            return rsn + "-ös";
	}
	else if(rsn.endsWith("6")) {
            return rsn + "-os";
	}
	else if(rsn.endsWith("0") || rsn.endsWith("1") || rsn.endsWith("2") || rsn.endsWith("4") || rsn.endsWith("1")
			|| rsn.endsWith("7") || rsn.endsWith("9")) {
            return rsn + "-es";
	}
	else {
            return rsn;
	}
    }
}
