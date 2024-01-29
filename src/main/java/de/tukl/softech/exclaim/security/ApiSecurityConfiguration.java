package de.tukl.softech.exclaim.security;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

@Configuration
@Order(1)
public class ApiSecurityConfiguration extends WebSecurityConfigurerAdapter {
    private ApiTokenAuthenticationProvider apiTokenAuthenticationProvider;

    public ApiSecurityConfiguration(ApiTokenAuthenticationProvider apiTokenAuthenticationProvider) {
        this.apiTokenAuthenticationProvider = apiTokenAuthenticationProvider;
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.antMatcher("/api/**")
                .authenticationProvider(apiTokenAuthenticationProvider)
                .authorizeRequests()
                .anyRequest().hasAnyRole("SERVICE").and()
                .addFilterBefore(new ApiTokenFilter(), BasicAuthenticationFilter.class);
        http.csrf().disable();
    }
}
