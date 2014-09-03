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

package org.opentripplanner.updater.alerts;

import com.google.protobuf.ExtensionRegistry;
import com.google.transit.realtime.GtfsRealtime;
import com.google.transit.realtime.GtfsRealtimeBplanner;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.impl.PatchServiceImpl;
import org.opentripplanner.routing.services.PatchService;
import org.opentripplanner.updater.GraphUpdaterManager;
import org.opentripplanner.updater.GraphWriterRunnable;
import org.opentripplanner.updater.PollingGraphUpdater;
import org.opentripplanner.util.HttpUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.prefs.Preferences;

/**
 * GTFS-RT alerts updater
 * 
 * Usage example ('myalert' name is an example) in file 'Graph.properties':
 * 
 * <pre>
 * myalert.type = real-time-alerts
 * myalert.frequencySec = 60
 * myalert.url = http://host.tld/path
 * myalert.earlyStartSec = 3600
 * myalert.defaultAgencyId = TA
 * </pre>
 */
public class GtfsRealtimeAlertsUpdater extends PollingGraphUpdater {
    
    private static final Logger LOG = LoggerFactory.getLogger(GtfsRealtimeAlertsUpdater.class);

	protected ExtensionRegistry extensionRegistry = ExtensionRegistry.newInstance();
	{
		GtfsRealtimeBplanner.registerAllExtensions(extensionRegistry);
	}

    private GraphUpdaterManager updaterManager;

    private Long lastTimestamp = Long.MIN_VALUE;

    private String url;

    private String defaultAgencyId;

    private PatchService patchService;

    private long earlyStart;

    private AlertsUpdateHandler updateHandler = null;
    
    private HttpUtils httpUtils;

    @Override
    public void setGraphUpdaterManager(GraphUpdaterManager updaterManager) {
        this.updaterManager = updaterManager;
    }

    @Override
    protected void configurePolling(Graph graph, Preferences preferences) throws Exception {
        // TODO: add options to choose different patch services
        patchService = graph.getService(PatchService.class);
        if(patchService == null) {
            patchService = new PatchServiceImpl(graph);
            graph.putService(PatchService.class, patchService);
        }
        
        String url = preferences.get("url", null);
        if (url == null)
            throw new IllegalArgumentException("Missing mandatory 'url' parameter");
        this.url = url;
        this.earlyStart = preferences.getInt("earlyStartSec", 0);
        this.defaultAgencyId = preferences.get("defaultAgencyId", null);
        LOG.info("Creating real-time alert updater running every {} seconds : {}",
                getFrequencySec(), url);
    }

    @Override
    public void setup() {
        if (updateHandler == null) {
            updateHandler = new AlertsUpdateHandler();
        }
        updateHandler.setEarlyStart(earlyStart);
        updateHandler.setDefaultAgencyId(defaultAgencyId);
        updateHandler.setPatchService(patchService);
        httpUtils = new HttpUtils();
    }

    @Override
    protected void runPolling() throws Exception {
        InputStream data = null;
        try {
            data = httpUtils.getData(url, lastTimestamp);
            if (data == null) {
                return;
            }

            final GtfsRealtime.FeedMessage feed = GtfsRealtime.FeedMessage.PARSER.parseFrom(data, extensionRegistry);

            long feedTimestamp = feed.getHeader().getTimestamp();
            if (feedTimestamp <= lastTimestamp) {
                LOG.info("Ignoring feed with an old timestamp.");
                return;
            }
            
            // Handle update in graph writer runnable
            updaterManager.execute(new GraphWriterRunnable() {
                @Override
                public void run(Graph graph) {
                    updateHandler.update(feed);
                }
            });

            lastTimestamp = feedTimestamp;
        } catch (Exception e) {
            LOG.error("Eror reading gtfs-realtime feed from " + url, e);
        } finally {
            if(data != null) {
                data.close();
            }
        }
    }

    @Override
    public void teardown() {
        httpUtils.cleanup();
    }

    public String toString() {
        return "GtfsRealtimeAlertsUpdater(" + url + ")";
    }

}
