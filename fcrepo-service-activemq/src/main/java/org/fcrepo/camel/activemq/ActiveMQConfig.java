/*
 * Licensed to DuraSpace under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership.
 *
 * DuraSpace licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fcrepo.camel.activemq;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.pool.PooledConnectionFactory;
import org.apache.camel.component.activemq.ActiveMQComponent;
import org.apache.camel.component.jms.JmsConfiguration;
import org.fcrepo.camel.common.config.BasePropsConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.jms.ConnectionFactory;

/**
 * @author dbernstein
 */
@Configuration
public class ActiveMQConfig extends BasePropsConfig {
    @Value("${jms.brokerUrl:tcp://localhost:61616}")
    private String jmsBrokerUrl;

    @Value("${jms.username:#{null}}")
    private String jmsUsername;

    @Value("${jms.password:#{null}}")
    private String jmsPasword;

    @Value("${jms.connections:10}")
    private int jmsConnections;

    @Value("${jms.consumers:1}")
    private int jmsConsumers;


    @Bean
    public ActiveMQConnectionFactory connectionFactory() {
        final var connectionFactory = new ActiveMQConnectionFactory();
        connectionFactory.setBrokerURL(jmsBrokerUrl);
        connectionFactory.setUserName(jmsUsername);
        connectionFactory.setPassword(jmsPasword);
        return connectionFactory;
    }

    @Bean
    public ConnectionFactory pooledConnectionFactory(final ActiveMQConnectionFactory connectionFactory) {
        final var pooledConnectionFactory = new PooledConnectionFactory();
        pooledConnectionFactory.setMaxConnections(jmsConnections);
        pooledConnectionFactory.setConnectionFactory(connectionFactory);
        return pooledConnectionFactory;
    }

    @Bean
    public JmsConfiguration jmsConfiguration(final PooledConnectionFactory connectionFactory) {
        final var configuration = new JmsConfiguration();
        configuration.setConcurrentConsumers(jmsConsumers);
        configuration.setConnectionFactory(connectionFactory);
        return configuration;
    }

    @Bean("broker")
    public ActiveMQComponent activeMQComponent(final JmsConfiguration jmsConfiguration) {
        final var component = new ActiveMQComponent();
        component.setConfiguration(jmsConfiguration);
        return component;
    }
}

