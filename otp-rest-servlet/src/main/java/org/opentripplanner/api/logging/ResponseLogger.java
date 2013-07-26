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

package org.opentripplanner.api.logging;

import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResponseLogger implements Filter {

    private static final Logger LOG = LoggerFactory.getLogger(ResponseLogger.class);
    private String url = null;

    @Override
    public void init(FilterConfig config) throws ServletException {
        url = config.getInitParameter("url");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws ServletException, IOException {
        if (response.getCharacterEncoding() == null) {
            response.setCharacterEncoding("UTF-8"); // Or whatever default. UTF-8 is good for World Domination.
        }

        HttpServletResponseCopier responseCopier = new HttpServletResponseCopier((HttpServletResponse) response);

        try {
            chain.doFilter(request, responseCopier);
            responseCopier.flushBuffer();
        } finally {
            byte[] copy = responseCopier.getCopy();
            log(copy);
        }
    }

    private void log(byte[] copy) {
        try {
            HttpPost post = new HttpPost(url);
            post.addHeader("Content-Type", "application/json; charset=UTF-8");
            post.setEntity(new ByteArrayEntity(copy));

            HttpClient httpclient = new DefaultHttpClient();
            HttpResponse response = httpclient.execute(post);
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                LOG.info("Logging failed: (HTTP " + response.getStatusLine().getStatusCode() + ", " + response.getStatusLine().getReasonPhrase() + ")");
            }
        } catch (Exception e) {
            LOG.info("Logging failed: ", e);
        }
    }

    @Override
    public void destroy() {
        // NOOP.
    }
}
