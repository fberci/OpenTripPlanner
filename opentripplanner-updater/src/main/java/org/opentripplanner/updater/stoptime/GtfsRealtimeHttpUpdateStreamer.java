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

import lombok.Setter;

import org.opentripplanner.util.HttpUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.transit.realtime.GtfsRealtime;
import com.google.transit.realtime.GtfsRealtime.FeedMessage;

public class GtfsRealtimeHttpUpdateStreamer extends GtfsRealtimeAbstractUpdateStreamer {
    private static final Logger LOG = LoggerFactory.getLogger(GtfsRealtimeHttpUpdateStreamer.class);

    @Setter
    private String url;

    @Override
    protected FeedMessage getFeedMessage() {
        FeedMessage feed = null;
        try {
            InputStream is = HttpUtils.getData(url);
            feed = GtfsRealtime.FeedMessage.parseFrom(is, extensionRegistry);
        } catch (IOException e) {
            LOG.warn("Failed to parse gtfs-rt feed from " + url + ":", e);
        }
        return feed;
    }

    public String toString() {
        return "GtfsRealtimeHttpUpdateStreamer(" + url + ")";
    }
}