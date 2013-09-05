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

package org.opentripplanner.util;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.cookie.DateUtils;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

public class HttpUtils {
    
    private static final String HEADER_IFMODSINCE = "If-Modified-Since";
    private static final int TIMEOUT_CONNECTION = 5000;
    private static final int TIMEOUT_SOCKET = 5000;

    public static InputStream getData(String url) throws ClientProtocolException, IOException {
        return getData(url, -1);
    }

    public static InputStream getData(String url, long timestamp) throws ClientProtocolException, IOException {
        HttpGet httpget = new HttpGet(url);
        if(timestamp >= 0)
            httpget.addHeader(HEADER_IFMODSINCE, DateUtils.formatDate(new Date(timestamp * 1000)));
        
        HttpClient httpclient = getClient();
        HttpResponse response = httpclient.execute(httpget);
        if(response.getStatusLine().getStatusCode() == HttpStatus.SC_NOT_MODIFIED)
            return null;
        
        if(response.getStatusLine().getStatusCode() != HttpStatus.SC_OK)
            return null;

        HttpEntity entity = response.getEntity();
        if (entity == null) {
            return null;
        }
        InputStream instream = entity.getContent();
        return instream;
    }

    public static void testUrl(String url) throws ClientProtocolException, IOException {
        HttpHead head = new HttpHead(url);
        HttpClient httpclient = getClient();
        HttpResponse response = httpclient.execute(head);

        StatusLine status = response.getStatusLine();
        if (status.getStatusCode() == 404) {
            throw new FileNotFoundException();
        }

        if (status.getStatusCode() != 200) {
            throw new RuntimeException("Could not get URL: " + status.getStatusCode() + ": "
                    + status.getReasonPhrase());
        }
    }
    
    private static HttpClient getClient() {
        HttpParams httpParams = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(httpParams, TIMEOUT_CONNECTION);
        HttpConnectionParams.setSoTimeout(httpParams, TIMEOUT_SOCKET);
        
        DefaultHttpClient httpclient = new DefaultHttpClient();
        httpclient.setParams(httpParams);
        return httpclient;
    }
}
