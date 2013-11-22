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

import java.util.concurrent.ConcurrentHashMap;

public final class OneBusAwayApiCache<T1, T2> {
    private final ConcurrentHashMap<T1, T2> cache = new ConcurrentHashMap<T1, T2>();
    
    public final boolean containsKey(T1 key) {
        return cache.containsKey(key);
    }
    
    public final T2 get(T1 key) {
        return cache.get(key);
    }
    
    public final void put(T1 key, T2 value) {
        cache.put(key, value);
    }
}
