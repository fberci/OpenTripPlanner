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
import org.opentripplanner.routing.patch.TranslatedString;

import java.util.Collection;

@Data
public class TransitAlert {
    private String id;
    private Long start;
    private Long end;
	private Long timestamp;
    private Collection<String> stopIds;
    private Collection<String> routeIds;
    private TranslatedString url;
    private TranslatedString header;
    private TranslatedString description;
}
