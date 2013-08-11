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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Data;
import lombok.Getter;

@Data
public class TransitEntryWithReferences<T> {
    @JsonProperty("class")
    private String klass;
    private T entry;
    
    @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    private TransitReferences references;

    public TransitEntryWithReferences(T entry, TransitReferences references) {
        this.klass = getClass().getSimpleName();
        this.entry = entry;
        this.references = references;
    }
}
