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
package org.fcrepo.camel.examples;

import static org.apache.activemq.camel.component.ActiveMQComponent.activeMQComponent;
import static org.apache.camel.Exchange.CONTENT_TYPE;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.builder.xml.XPathBuilder;

/**
 * A Camel Java DSL Router.
 *
 * @author Aaron Coburn
 *      Nov 8, 2014
 */
public class SolrRouteBuilder extends RouteBuilder {

    /**
     * Let's configure the Camel routing rules using Java code.
     */
    public final void configure() throws Exception {

        final String indexable =
            "http://fedora.info/definitions/v4/repository#Indexable";

        getContext().addComponent("activemq",
            activeMQComponent("vm://localhost:61616?broker.persistent=false"));

        XPathBuilder xpath = new XPathBuilder(
            "/rdf:RDF/rdf:Description/rdf:type[@rdf:resource='" + indexable + "']");
        xpath.namespace("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");

        from("activemq:topic:fedora")
          .to("fcrepo:localhost:8080/fcrepo/rest")
          .filter(xpath)
          .to("fcrepo:localhost:8080/fcrepo/rest?accept=application/json&transform=mytransform")
          .setHeader(CONTENT_TYPE).constant("application/json")
          .to("http4:localhost:8080/solr/core/update");
    }
}
