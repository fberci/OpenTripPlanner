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

import com.brsanthu.googleanalytics.GoogleAnalytics;
import com.brsanthu.googleanalytics.PageViewHit;
import com.brsanthu.googleanalytics.TimingHit;
import lombok.RequiredArgsConstructor;
import org.opentripplanner.common.MavenVersion;

import java.net.URI;
import java.util.UUID;

public class OneBusAwayRequestLogger {

	private static GoogleAnalytics ga = new GoogleAnalytics("UA-50283889-5", "OpenTripPlanner-OBA API", MavenVersion.VERSION.toString());
	{
		ga.getConfig().setDeriveSystemParameters(false);
		ga.getConfig().setGatherStats(true);
	}

	public OneBusAwayRequestLogger() {
	}

	public LogRequest startRequest(URI uri, String clientId, String apiKey, boolean internalRequest) {
		if(clientId == null) {
			clientId = UUID.randomUUID().toString();
		}

		String query = uri.getQuery();
		String url = uri.getPath() + (query == null ? "" : "?" + query);

		PageViewHit pageViewHit = new PageViewHit();
		pageViewHit.customMetric(0, "" + (internalRequest ? 0 : 1));
		pageViewHit.customMetric(1, "" + apiKey);
		pageViewHit.clientId(clientId);
		pageViewHit.documentUrl(url);

		TimingHit timingHit = new TimingHit();
		timingHit.customMetric(0, "" + internalRequest);
		timingHit.customMetric(1, "" + apiKey);
		timingHit.clientId(clientId);
		timingHit.documentUrl(url);

		return new LogRequest(pageViewHit, timingHit);
	}

	@RequiredArgsConstructor
	public class LogRequest {

		private final long startTime = System.currentTimeMillis();
		private final PageViewHit pageHit;
		private final TimingHit timingHit;

		public void finishRequest() {
			long now = System.currentTimeMillis();
			timingHit.serverResponseTime((int) (now - startTime));

			ga.postAsync(pageHit);
			ga.postAsync(timingHit);
		}
	}
}
