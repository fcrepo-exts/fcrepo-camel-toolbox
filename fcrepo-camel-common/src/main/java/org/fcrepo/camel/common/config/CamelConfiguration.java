/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.camel.common.config;

import java.util.Collection;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.spring.SpringCamelContext;
import org.apache.camel.spring.spi.CamelBeanPostProcessor;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Drop-in replacement for the {@code org.apache.camel.spring.javaconfig.CamelConfiguration}
 * base class provided by the {@code camel-spring-javaconfig} module, which was removed in
 * Camel 4. It builds a Spring-managed {@link SpringCamelContext} and registers every
 * {@link RoutesBuilder} bean found in the application context, matching the auto-detection
 * behaviour the toolbox relied on.
 *
 * @author Dan Field
 */
@Configuration
public abstract class CamelConfiguration implements ApplicationContextAware {

    private ApplicationContext applicationContext;

    /**
     * @return a Spring-managed CamelContext with all RoutesBuilder beans registered
     * @throws Exception if a route cannot be added
     */
    @Bean
    public CamelContext camelContext() throws Exception {
        final SpringCamelContext camelContext = new SpringCamelContext(applicationContext);
        final Collection<RoutesBuilder> routesBuilders =
                applicationContext.getBeansOfType(RoutesBuilder.class).values();
        for (final RoutesBuilder routesBuilder : routesBuilders) {
            camelContext.addRoutes(routesBuilder);
        }
        return camelContext;
    }

    /**
     * @param camelContext the Spring-managed CamelContext
     * @return a ProducerTemplate bound to the CamelContext
     */
    @Bean
    public ProducerTemplate producerTemplate(final CamelContext camelContext) {
        return camelContext.createProducerTemplate();
    }

    /**
     * Registers Camel's Spring bean post-processor so that {@code @EndpointInject},
     * {@code @Produce} and {@code @BeanInject} annotations are honoured on Spring beans
     * and on Spring test instances (matching the camel-spring-javaconfig behaviour).
     *
     * <p>The processor is given the {@link ApplicationContext} rather than the
     * {@link CamelContext} so it resolves the context lazily. Injecting the CamelContext
     * here would force it (and every route/config bean) to be created while Spring is
     * still wiring bean post-processors, i.e. before the property-placeholder resolver
     * is active, breaking {@code @Value} resolution.
     *
     * @return the Camel bean post-processor
     */
    @Bean
    public CamelBeanPostProcessor camelBeanPostProcessor() {
        final CamelBeanPostProcessor beanPostProcessor = new CamelBeanPostProcessor();
        beanPostProcessor.setApplicationContext(applicationContext);
        return beanPostProcessor;
    }

    @Override
    public void setApplicationContext(final ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    /**
     * @return the Spring application context
     */
    protected ApplicationContext getApplicationContext() {
        return applicationContext;
    }
}
