package org.fcrepo.camel;

import java.io.InputStream;
import java.util.Properties;
import java.io.IOException;

public class FedoraTestUtils {
    public static String getFcrepoBaseUri() throws IOException {
        Properties props = new Properties();

        InputStream in = FedoraTestUtils.class.getResourceAsStream("/org.fcrepo.properties");
        props.load(in);
        in.close();

        return "http://" + props.getProperty("fcrepo.url").replaceAll("http://", "");

    }
    public static String getFcrepoEndpointUri() throws IOException {
        Properties props = new Properties();

        InputStream in = FedoraTestUtils.class.getResourceAsStream("/org.fcrepo.properties");
        props.load(in);
        in.close();

        return "fcrepo:" + props.getProperty("fcrepo.url").replaceAll("http://", "");
    }

    public static String getTurtleDocument() {
        return "PREFIX dc: <http://purl.org/dc/elements/1.1/>\n\n<> dc:title \"some title\" .";
    }

    public static String getTextDocument() {
        return "Simple plain text document";
    }

    public static String getPatchDocument() {
        return "PREFIX dc: <http://purl.org/dc/elements/1.1/> \n\nINSERT { <> dc:title \"another title\" . } \nWHERE { }";
    }
}


