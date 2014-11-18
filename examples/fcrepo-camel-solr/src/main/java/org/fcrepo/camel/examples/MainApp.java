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

import org.apache.camel.main.Main;

/**
 * A Camel Application.
 */
public final class MainApp {

    /**
     *  Keep the default constructor private.
     */
    private MainApp() {
    }

    /**
     * A main() so we can easily run these routing rules in our IDE.
     * @param args The incoming argument list
     */
    public static void main(final String... args) throws Exception {
        final Main main = new Main();
        main.enableHangupSupport();
        main.addRouteBuilder(new SolrRouteBuilder());
        main.run(args);
    }

}

