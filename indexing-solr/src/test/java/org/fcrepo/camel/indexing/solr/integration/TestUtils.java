/**
 * Copyright 2015 DuraSpace, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fcrepo.camel.indexing.solr.integration;

import java.io.InputStream;
import java.util.concurrent.Callable;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.camel.Exchange;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

/**
 * Utility functions for the integration tests
 *
 * @author acoburn
 * @since 2015-04-21
 */
public class TestUtils {

    public static InputStream httpGet(final String url) throws Exception {
        final CloseableHttpClient httpClient = HttpClients.createDefault();
        final HttpGet get = new HttpGet(url);
        return httpClient.execute(get).getEntity().getContent();
    }

    public static void httpPost(final String url, final String content, final String mimeType) throws Exception {
        final CloseableHttpClient httpClient = HttpClients.createDefault();
        final HttpPost post = new HttpPost(url);
        post.addHeader(Exchange.CONTENT_TYPE, mimeType);
        post.setEntity(new StringEntity(content));
        httpClient.execute(post);
    }

    public static Callable<Integer> solrCount(final String url) {
        final ObjectMapper mapper = new ObjectMapper();

        return new Callable<Integer>() {
            public Integer call() throws Exception {
                return mapper.readTree(httpGet(url)).get("response").get("numFound").asInt();
            }
        };
    }
 
    private TestUtils() {
        // prevent instantiation
    }
}
