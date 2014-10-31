package org.fcrepo.camel;

import java.net.URI;

public class FedoraResponse {

    private volatile URI url;

    private volatile int statusCode;

    private volatile URI location;
    
    private volatile String body;

    public FedoraResponse(final URI url, final int statusCode,
            final URI location, final String body) {
        this.setUrl(url);
        this.setStatusCode(statusCode);
        this.setLocation(location);
        this.setBody(body);
    }

    public URI getUrl() {
        return url;
    }

    public void setUrl(final URI url) {
        this.url = url;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(final int statusCode) {
        this.statusCode = statusCode;
    }

    public String getBody() {
        return body;
    }

    public void setBody(final String body) {
        this.body = body;
    }

    public URI getLocation() {
        return location;
    }

    public void setLocation(final URI location) {
        this.location = location;
    }
}
