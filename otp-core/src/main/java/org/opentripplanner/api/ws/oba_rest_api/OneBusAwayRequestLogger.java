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
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.opentripplanner.api.ws.oba_rest_api.beans.TransitResponseBuilder;

import java.net.URI;

public class OneBusAwayRequestLogger {

    private static int counter = 1;

	private static GoogleAnalytics ga = new GoogleAnalytics("UA-50283889-5"); //, "OpenTripPlanner-OBA API", MavenVersion.VERSION.toString());
    private static final int TIMEOUT_CONNECTION = 100;
    private static final int TIMEOUT_SOCKET = 100;

    {
		ga.getConfig().setDiscoverRequestParameters(false);
        ga.getConfig().setUserAgent("OTP-API");
        ga.getConfig().setGatherStats(true);

        // Wait at most 100ms for a response, to avoid having too many backlogged requests
        PoolingHttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager();
        connManager.setDefaultMaxPerRoute(1);

        HttpClientBuilder builder = HttpClients.custom().setConnectionManager(connManager);
        builder.setUserAgent(ga.getConfig().getUserAgent());
        builder.setDefaultRequestConfig(
                RequestConfig
                        .custom()
                        .setConnectionRequestTimeout(TIMEOUT_CONNECTION)
                        .setConnectTimeout(TIMEOUT_SOCKET)
                        .setSocketTimeout(TIMEOUT_SOCKET)
                        .build()
        );

        ga.setHttpClient(builder.build());
	}

	public OneBusAwayRequestLogger() {
	}

	public LogRequest startRequest(Object apiMethod, HttpContext req, URI uri, String clientId, String apiKey, boolean internalRequest, TransitResponseBuilder.DialectWrapper dialect) {
		if(clientId == null) {
			clientId = "" + ++counter;
		}

		return new LogRequest(apiMethod, req, uri, clientId, apiKey, internalRequest, dialect.getDialect().name());
	}

	public class LogRequest {

		private final long startTime = System.currentTimeMillis();
		private final PageViewHit pageHit = new PageViewHit();
		private final TimingHit timingHit = new TimingHit();

        private Object apiMethod;
        private String dialect;
        private String userLanguage;
        private String userAgent;
        private String userIp;
        private String clientId;
        private String url;
        private String apiKey;
        private boolean internalRequest;

        protected LogRequest(Object apiMethod, HttpContext req, URI uri, String clientId, String apiKey, boolean internalRequest, String dialect) {

            this.clientId = clientId;
            this.apiMethod = apiMethod;

            this.apiKey = apiKey;
            this.internalRequest = internalRequest;
            this.dialect = dialect;

            this.url = uri.toString();
            this.userLanguage = req.getRequest().getHeaderValue("Accept-Language");
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
            gar.userLanguage(userLanguage);
            gar.customMetric(1, "" + (internalRequest ? 1 : 0));
            gar.customDimention(1, apiKey != null ? apiKey : "");
            gar.customDimention(2, apiMethod.getClass().getSimpleName());
            gar.customDimention(3, dialect);
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
