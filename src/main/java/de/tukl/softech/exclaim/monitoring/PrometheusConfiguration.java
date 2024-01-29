package de.tukl.softech.exclaim.monitoring;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.MetricsServlet;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass(CollectorRegistry.class)
public class PrometheusConfiguration {

    private final CollectorRegistry registry = new CollectorRegistry(true);

    @Bean
    @ConditionalOnMissingBean
    CollectorRegistry metricsRegistry() {
        return registry;
    }

    @Bean
    ServletRegistrationBean registerPrometheusExporterServlet(CollectorRegistry metricsRegistry) {
        return new ServletRegistrationBean(new MetricsServlet(metricsRegistry), "/metrics");
    }
}
