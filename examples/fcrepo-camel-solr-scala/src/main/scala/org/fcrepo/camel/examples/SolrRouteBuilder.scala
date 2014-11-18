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
package org.fcrepo.camel.examples

import org.apache.camel.Exchange
import org.apache.camel.scala.dsl.builder.RouteBuilder
import org.apache.activemq.camel.component.ActiveMQComponent;

/**
 * A Camel Router using the Scala DSL
 *
 * @author Aaron Coburn
 * @since  Nov 8, 2014
 */
class SolrRouteBuilder extends RouteBuilder {

  getContext.addComponent("activemq", ActiveMQComponent.activeMQComponent("vm://localhost?broker.persistent=false"))

   // a route using Scala blocks
   "activemq:topic:fedora" ==> {
     to("fcrepo:localhost:8080/fcrepo/rest")
     filter(xpath) {
       to("fcrepo:localhost:8080/fcrepo/rest?accept=application/json&transform=default")
       setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
       to("http4:/localhost:8080/solr/core/update")
     }
   }
}
