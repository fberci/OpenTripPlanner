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

import java.util.Collection;
import java.util.List;
import lombok.Data;
import org.opentripplanner.routing.patch.TranslatedString;

@Data
public class TransitSituation {
    private String id;
    private Long creationTime;
    private String reasons;
    private String severity;
    private Collection<TransitTimeRange> activeWindows;
    private Collection<TransitTimeRange> publicationWindows;
    private Collection<TransitSituationAffects> allAffects;
    private Collection<TransitSituationConsequences> consequences;
    private TransitNaturalLanguageString url;
    private TransitNaturalLanguageString header;
    private TransitNaturalLanguageString description;
}
