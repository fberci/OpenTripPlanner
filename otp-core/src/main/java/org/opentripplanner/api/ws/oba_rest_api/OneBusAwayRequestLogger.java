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

import com.brsanthu.googleanalytics.ExceptionHit;
import com.brsanthu.googleanalytics.GoogleAnalytics;
import com.brsanthu.googleanalytics.GoogleAnalyticsRequest;
import com.brsanthu.googleanalytics.PageViewHit;
import com.brsanthu.googleanalytics.TimingHit;
import com.sun.jersey.api.core.HttpContext;

import java.net.URI;

public class OneBusAwayRequestLogger {

    private static int counter = 1;

	private static GoogleAnalytics ga = new GoogleAnalytics("UA-50283889-5"); //, "OpenTripPlanner-OBA API", MavenVersion.VERSION.toString());
	{
		ga.getConfig().setDeriveSystemParameters(false);
		ga.getConfig().setGatherStats(true);
	}

	public OneBusAwayRequestLogger() {
	}

	public LogRequest startRequest(Object apiMethod, HttpContext req, URI uri, String clientId, String apiKey, boolean internalRequest) {
		if(clientId == null) {
			clientId = "" + ++counter;
		}

		return new LogRequest(apiMethod, req, uri, clientId, apiKey, internalRequest);
	}

	public class LogRequest {

		private final long startTime = System.currentTimeMillis();
		private final PageViewHit pageHit = new PageViewHit();
		private final TimingHit timingHit = new TimingHit();

        private Object apiMethod;
        private String userAgent;
        private String userIp;
        private String clientId;
        private String url;
        private String apiKey;
        private boolean internalRequest;

        protected LogRequest(Object apiMethod, HttpContext req, URI uri, String clientId, String apiKey, boolean internalRequest) {

            this.clientId = clientId;
            this.apiMethod = apiMethod;

            this.apiKey = apiKey;
            this.internalRequest = internalRequest;

            this.url = uri.toString();
            this.userAgent = req.getRequest().getHeaderValue("User-Agent");
            this.userIp = req.getRequest().getHeaderValue("X-Forwarded-For");
            if(this.userIp == null) {
                this.userIp = ""; // req.getRemoteAddr();
            }

            init(pageHit);
            pageHit.documentTitle(this.apiMethod.getClass().getSimpleName());

            init(timingHit);
            timingHit.userTimingCategory("apiRequest");
            timingHit.userTimingLabel(this.apiMethod.getClass().getSimpleName());
        }

        private <T extends GoogleAnalyticsRequest<T>> T init(T gar)  {
            gar.userAgent(userAgent);
            gar.userIp(userIp);
            gar.customMetric(1, "" + (internalRequest ? 1 : 0));
            if(apiKey != null) {
                gar.customDimention(1, apiKey);
            }
            gar.customDimention(2, apiMethod.getClass().getSimpleName());
            gar.clientId(clientId);
            gar.documentUrl(url);

            return gar;
        }

		public void finishRequest() {
			long now = System.currentTimeMillis();
			timingHit.pageLoadTime((int) (now - startTime));
            timingHit.serverResponseTime((int) (now - startTime));

			ga.postAsync(pageHit);
			ga.postAsync(timingHit);
		}

        public void exception(Exception e) {
            exception(e.getClass().getSimpleName(), true);
        }

        public void exception(String description, boolean fatal) {
            ExceptionHit exceptionHit = init(new ExceptionHit());
            exceptionHit.exceptionFatal(fatal);
            exceptionHit.exceptionDescription(description);

            ga.postAsync(pageHit);
            ga.postAsync(exceptionHit);
        }
    }
}
