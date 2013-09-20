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

package org.opentripplanner.updater.stoptime;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.prefs.Preferences;

import org.opentripplanner.updater.PreferencesConfigurable;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.trippattern.TripUpdateList;
import org.opentripplanner.util.HttpUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.transit.realtime.GtfsRealtime;

public class GtfsRealtimeHttpTripUpdateSource implements TripUpdateSource, PreferencesConfigurable {
    
    private static final Logger LOG = LoggerFactory.getLogger(GtfsRealtimeHttpTripUpdateSource.class);

    /**
     * Default agency id that is used for the trip id's in the TripUpdateLists
     */
    private String agencyId;

    private String url;
    
    private Graph graph;
    
    private long lastTimestamp = Long.MIN_VALUE;

    @Override
    public void configure(Graph graph, Preferences preferences) throws Exception {
        String url = preferences.get("url", null);
        if (url == null)
            throw new IllegalArgumentException("Missing mandatory 'url' parameter");
        this.graph = graph;
        this.url = url;
        this.agencyId = preferences.get("defaultAgencyId", null);
    }

    @Override
    public List<TripUpdateList> getUpdates() throws Exception {
        GtfsRealtime.FeedMessage feed = null;
        List<TripUpdateList> updates = null;
        
        HttpUtils httpUtils = new HttpUtils();
        InputStream is = null;
        try {
            is = httpUtils.getData(url, lastTimestamp);
            if (is != null) {
                feed = GtfsRealtime.FeedMessage.PARSER.parseFrom(is);

                GtfsRealtime.FeedHeader header = feed.getHeader();
                long feedTimestamp = header.getTimestamp();
        
                if(lastTimestamp < feedTimestamp) {
                    updates = TripUpdateList.decodeFromGtfsRealtime(feed, agencyId, graph.getTimeZone());
                    lastTimestamp = feedTimestamp;
                }
            }
        } catch (Exception e) {
            LOG.warn("Failed to parse gtfs-rt feed from " + url + ":", e);
        } finally {
            if(is != null) {
                is.close();
            }
        }
        httpUtils.cleanup();
        return updates;
    }

    public String toString() {
        return "GtfsRealtimeHttpUpdateStreamer(" + url + ")";
    }
}
