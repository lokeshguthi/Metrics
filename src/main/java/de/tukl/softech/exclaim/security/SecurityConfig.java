package de.tukl.softech.exclaim.security;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.access.intercept.FilterSecurityInterceptor;

@Configuration
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig extends WebSecurityConfigurerAdapter {
    private ExclaimAuthenticationProvider exclaimAuthenticationProvider;

    public SecurityConfig(ExclaimAuthenticationProvider exclaimAuthenticationProvider) {
        this.exclaimAuthenticationProvider = exclaimAuthenticationProvider;
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.antMatcher("/**")
            .authenticationProvider(exclaimAuthenticationProvider)
            .authorizeRequests()
                .antMatchers("/metrics", "/docs/**", "/webjars/**", "/css/**", "/js/**", "/fonts/**", "/register", "/registered", "/requestPassword", "/resetPassword", "/activate").permitAll()
                .anyRequest().authenticated()
                .and()
            .formLogin()
                .loginPage("/login")
                .permitAll()
                .and()
            .logout()
                .permitAll();
        http.addFilterBefore(new AccessDeniedFilter(), FilterSecurityInterceptor.class);
        http
            .headers().frameOptions().sameOrigin();
    }
}
