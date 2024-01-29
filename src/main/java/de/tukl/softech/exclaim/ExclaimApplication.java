package de.tukl.softech.exclaim;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.thymeleaf.extras.springsecurity4.dialect.SpringSecurityDialect;
import org.thymeleaf.spring5.SpringTemplateEngine;
import org.thymeleaf.spring5.templateresolver.SpringResourceTemplateResolver;
import org.tuckey.web.filters.urlrewrite.UrlRewriteFilter;

@SpringBootApplication
@Configuration
@EnableScheduling
public class ExclaimApplication {

	public static void main(String[] args) {
		SpringApplication.run(ExclaimApplication.class, args);
	}

	@Bean
	public SpringTemplateEngine templateEngine(SpringResourceTemplateResolver templateResolver) {
		SpringTemplateEngine templateEngine = new SpringTemplateEngine();
		templateEngine.setTemplateResolver(templateResolver);
		templateEngine.addDialect(new SpringSecurityDialect());
		return templateEngine;
	}

	@Bean
	public SessionRegistry sessionRegistry() {
		return new SessionRegistryImpl();
	}

	@Bean
	public FilterRegistrationBean<UrlRewriteFilter> tuckeyRegistrationBean() {
		final FilterRegistrationBean<UrlRewriteFilter> registrationBean = new FilterRegistrationBean<>();
		String fileName = "urlrewrite.xml";
		String prefix = java.lang.ClassLoader.getSystemResourceAsStream(fileName) != null ? "" : "BOOT-INF/classes/";
		registrationBean.addInitParameter("confPath", prefix + fileName);
		registrationBean.setFilter(new UrlRewriteFilter());
		return registrationBean;
	}
}
