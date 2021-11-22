/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
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

