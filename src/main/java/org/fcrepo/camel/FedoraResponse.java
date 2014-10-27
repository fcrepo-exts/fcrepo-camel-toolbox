package org.fcrepo.camel;

public class FedoraResponse {

    private volatile String url;

    private volatile int statusCode;

    private volatile String location;
    
    private volatile String body;

    public FedoraResponse(final String url, final int statusCode,
            final String location, final String body) {
        this.setUrl(url);
        this.setStatusCode(statusCode);
        this.setLocation(location);
        this.setBody(body);
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(final String url) {
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

    public String getLocation() {
        return location;
    }

    public void setLocation(final String location) {
        this.location = location;
    }
}
