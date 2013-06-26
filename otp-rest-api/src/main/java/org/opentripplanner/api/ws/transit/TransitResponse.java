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

package org.opentripplanner.api.ws.transit;

import lombok.Getter;

public class TransitResponse {

    @Getter private int version;
    @Getter private int code;
    @Getter private String text;
    @Getter private long currentTime = System.currentTimeMillis();
    @Getter private Object data;

    public TransitResponse(int version, int code, String text, Object data) {
        this.version = version;
        this.code = code;
        this.text = text;
        this.data = data;
    }
}