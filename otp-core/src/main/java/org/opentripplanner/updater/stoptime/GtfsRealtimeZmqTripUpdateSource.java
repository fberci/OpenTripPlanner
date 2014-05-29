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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.prefs.Preferences;

import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.trippattern.TripUpdateList;
import org.opentripplanner.updater.PreferencesConfigurable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.transit.realtime.GtfsRealtime;
import com.google.transit.realtime.GtfsRealtime.FeedMessage;

/**
 * Supposed to be GTFS-RT ZeroMQ trip update source. For now it just reads the GTFS-RT
 * from a local file. 
 */
public class GtfsRealtimeZmqTripUpdateSource implements TripUpdateSource, PreferencesConfigurable {
    private static final Logger LOG = LoggerFactory.getLogger(GtfsRealtimeZmqTripUpdateSource.class);

    private File file;

    /**
     * Default agency id that is used for the trip id's in the TripUpdateLists
     */
    private String agencyId;
    
    private Graph graph;

    @Override
    public void configure(Graph graph, Preferences preferences) throws Exception {
        this.agencyId = preferences.get("defaultAgencyId", null);
        this.file = new File(preferences.get("file", ""));
        this.graph = graph;
    }

    @Override
    public List<TripUpdateList> getUpdates() throws Exception {
        FeedMessage feed = null;
        List<TripUpdateList> updates = null;
        InputStream is = null;
        try {
            is = new FileInputStream(file);
            feed = GtfsRealtime.FeedMessage.PARSER.parseFrom(is);
            updates = TripUpdateList.decodeFromGtfsRealtime(feed, agencyId, graph.getTimeZone());
        } catch (IOException e) {
            LOG.warn("Failed to parse gtfs-rt feed at " + file + ":", e);
        } finally {
            if(is != null) {
                is.close();
            }
        }
        return updates;
    }

    public String toString() {
        return "GtfsRealtimeZmqTripUpdateSource(" + file + ")";
    }

}