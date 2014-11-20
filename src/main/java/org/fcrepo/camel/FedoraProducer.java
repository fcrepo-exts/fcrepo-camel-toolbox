/**
 * Copyright 2014 DuraSpace, Inc.
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

package org.fcrepo.camel;

import static java.net.URI.create;
import static org.apache.camel.Exchange.ACCEPT_CONTENT_TYPE;
import static org.apache.camel.Exchange.CONTENT_TYPE;
import static org.apache.camel.Exchange.HTTP_METHOD;
import static org.apache.camel.Exchange.HTTP_RESPONSE_CODE;
import static org.apache.camel.component.http4.HttpMethods.DELETE;
import static org.apache.camel.component.http4.HttpMethods.GET;
import static org.apache.camel.component.http4.HttpMethods.POST;
import static org.fcrepo.camel.FedoraEndpoint.DEFAULT_CONTENT_TYPE;
import static org.fcrepo.camel.FedoraEndpoint.FCREPO_IDENTIFIER;
import static org.fcrepo.jms.headers.DefaultMessageFactory.IDENTIFIER_HEADER_NAME;
import static org.slf4j.LoggerFactory.getLogger;

import java.net.URI;
import java.io.IOException;
import java.io.InputStream;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.component.http4.HttpMethods;
import org.apache.camel.component.http4.HttpOperationFailedException;
import org.apache.camel.impl.DefaultProducer;
import org.apache.camel.util.ExchangeHelper;
import org.slf4j.Logger;

/**
 * The Fedora producer.
 *
 * @author Aaron Coburn
 * @since October 20, 2014
 */
public class FedoraProducer extends DefaultProducer {
    private static final Logger LOGGER = getLogger(FedoraProducer.class);

    private FedoraEndpoint endpoint;

    /**
     * Create a FedoraProducer object
     *
     * @param endpoint the FedoraEndpoint corresponding to the exchange.
     */
    public FedoraProducer(final FedoraEndpoint endpoint) {
        super(endpoint);
        this.endpoint = endpoint;
    }

    /**
     * Define how message exchanges are processed.
     *
     * @param exchange the InOut message exchange
     * @throws HttpOperationFailedException
     */
    @Override
    public void process(final Exchange exchange) throws HttpOperationFailedException, IOException {
        final Message in = exchange.getIn();
        final FedoraClient client = new FedoraClient(
                endpoint.getAuthUsername(),
                endpoint.getAuthPassword(),
                endpoint.getAuthHost(),
                endpoint.getThrowExceptionOnFailure());

        final HttpMethods method = getMethod(exchange);
        final String contentType = getContentType(exchange);
        final String accept = getAccept(exchange);
        final String url = getUrl(exchange);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Fcrepo Request [{}] with method [{}]", url, method);
        }

        FedoraResponse response;

        switch (method) {
        case PATCH:
            response = client.patch(getMetadataUri(client, url), in.getBody(InputStream.class));
            exchange.getIn().setBody(response.getBody());
            break;
        case PUT:
            response = client.put(create(url), in.getBody(InputStream.class), contentType);
            exchange.getIn().setBody(response.getBody());
            break;
        case POST:
            response = client.post(create(url), in.getBody(InputStream.class), contentType);
            exchange.getIn().setBody(response.getBody());
            break;
        case DELETE:
            response = client.delete(create(url));
            exchange.getIn().setBody(response.getBody());
            break;
        case HEAD:
            response = client.head(create(url));
            exchange.getIn().setBody(null);
            break;
        case GET:
        default:
            response = client.get(endpoint.getMetadata() ? getMetadataUri(client, url) : create(url), accept);
            exchange.getIn().setBody(response.getBody());
        }
        exchange.getIn().setHeader(CONTENT_TYPE, response.getContentType());
        exchange.getIn().setHeader(HTTP_RESPONSE_CODE, response.getStatusCode());
        client.stop();
    }

    /**
     *
     */
    protected URI getMetadataUri(final FedoraClient client, final String url)
            throws HttpOperationFailedException, IOException {
        final FedoraResponse headResponse = client.head(create(url));
        if (headResponse.getLocation() != null) {
            return headResponse.getLocation();
        } else {
            return create(url);
        }
    }

    /**
     * Given an exchange, determine which HTTP method to use. Basically, use GET unless the value of the
     * Exchange.HTTP_METHOD header is defined. Unlike the http4: component, the request does not use POST if there is
     * a message body defined. This is so in order to avoid inadvertant changes to the repository.
     *
     * @param exchange the incoming message exchange
     */
    protected HttpMethods getMethod(final Exchange exchange) {
        HttpMethods method = exchange.getIn().getHeader(HTTP_METHOD, HttpMethods.class);
        if (method == null) {
            method = HttpMethods.GET;
        }
        return method;
    }

    /**
     * Given an exchange, extract the contentType value for use with a Content-Type header. The order of preference is
     * so: 1) a contentType value set on the endpoint 2) a contentType value set on the Exchange.CONTENT_TYPE header
     *
     * @param exchange the incoming message exchange
     */
    protected String getContentType(final Exchange exchange) {
        final String contentTypeString = ExchangeHelper.getContentType(exchange);
        String contentType = null;
        if (endpoint.getContentType() != null) {
            contentType = endpoint.getContentType();
        } else if (contentTypeString != null) {
            contentType = contentTypeString;
        }
        return contentType;
    }

    /**
     * Given an exchange, extract the accept value for use with an Accept header. The order of preference is: 1) an
     * accept value set on the endpoint 2) a value set on the Exchange.ACCEPT_CONTENT_TYPE header 3) a value set on an
     * "Accept" header 4) the endpoint DEFAULT_CONTENT_TYPE (i.e. application/rdf+xml)
     *
     * @param exchange the incoming message exchange
     */
    protected String getAccept(final Exchange exchange) {
        String accept;
        final Message in = exchange.getIn();
        if (endpoint.getAccept() != null) {
            accept = endpoint.getAccept();
        } else if (in.getHeader(ACCEPT_CONTENT_TYPE, String.class) != null) {
            accept = in.getHeader(ACCEPT_CONTENT_TYPE, String.class);
        } else if (in.getHeader("Accept", String.class) != null) {
            accept = in.getHeader("Accept", String.class);
        } else {
            accept = DEFAULT_CONTENT_TYPE;
        }
        return accept;
    }

    /**
     * Given an exchange, extract the fully qualified URL for a fedora resource. By default, this will use the entire
     * path set on the endpoint. If either of the following headers are defined, they will be appended to that path in
     * this order of preference: 1) FCREPO_IDENTIFIER 2) org.fcrepo.jms.identifier
     *
     * @param exchange the incoming message exchange
     */
    protected String getUrl(final Exchange exchange) {
        final Message in = exchange.getIn();
        final HttpMethods method = exchange.getIn().getHeader(HTTP_METHOD, HttpMethods.class);
        final URI baseUri = create(endpoint.getBaseUrl());
        final StringBuilder url = new StringBuilder("http://" + baseUri);
        if (in.getHeader(FCREPO_IDENTIFIER) != null) {
            url.append(in.getHeader(FCREPO_IDENTIFIER, String.class));
        } else if (in.getHeader(IDENTIFIER_HEADER_NAME) != null) {
            url.append(in.getHeader(IDENTIFIER_HEADER_NAME, String.class));
        }
        if (endpoint.getTransform() != null) {
            if (method == POST) {
                url.append("/fcr:transform");
            } else if (method == null || method == GET) {
                url.append("/fcr:transform/" + endpoint.getTransform());
            }
        } else if (method == DELETE && endpoint.getTombstone()) {
            url.append("/fcr:tombstone");
        }
        return url.toString();
    }
}
