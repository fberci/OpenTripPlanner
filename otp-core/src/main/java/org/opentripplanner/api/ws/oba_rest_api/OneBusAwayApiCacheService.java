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

public final class OneBusAwayApiCacheService {
    @SuppressWarnings("unchecked")
    private ConcurrentHashMap<String, OneBusAwayApiCache> caches = new ConcurrentHashMap<String, OneBusAwayApiCache>();
    
    public final <T1, T2> boolean containsKey(String cacheName, T1 key) {
        if(!caches.containsKey(cacheName)) {
            return false;
        }
        
        @SuppressWarnings("unchecked")
        OneBusAwayApiCache<T1, T2> cache = caches.get(cacheName);
        return cache.containsKey(key);
    }
    
    public final <T1, T2> T2 get(String cacheName, T1 key) {
        if(!caches.containsKey(cacheName)) {
            return null;
        }
        
        @SuppressWarnings("unchecked")
        OneBusAwayApiCache<T1, T2> cache = caches.get(cacheName);
        return cache.get(key);
    }
    
    public final <T1, T2> void put(String cacheName, T1 key, T2 value) {
        if(!caches.containsKey(cacheName)) {
            caches.put(cacheName, new OneBusAwayApiCache<T1, T2>());
        }
        
        @SuppressWarnings("unchecked")
        OneBusAwayApiCache<T1, T2> cache = caches.get(cacheName);
        cache.put(key, value);
    }
}
