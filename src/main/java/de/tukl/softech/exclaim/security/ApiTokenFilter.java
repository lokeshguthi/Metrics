package de.tukl.softech.exclaim.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.GenericFilterBean;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;
import java.util.Collections;

public class ApiTokenFilter extends GenericFilterBean {
    private static final Logger logger = LoggerFactory.getLogger(ApiTokenFilter.class);

    public static class ApiKeyAuthenticationToken extends AbstractAuthenticationToken {
        private String apiKey;

        public ApiKeyAuthenticationToken(String apiKey) {
            super(Collections.emptyList());
            this.apiKey = apiKey;
        }

        @Override
        public Object getCredentials() {
            return apiKey;
        }

        @Override
        public Object getPrincipal() {
            return "service";
        }
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        logger.debug("access to API in filter");
        String api_key = request.getParameter("api_key");
        if (api_key != null) {
            logger.debug("received api_key with value: {}", api_key);
            SecurityContextHolder.getContext().setAuthentication(new ApiKeyAuthenticationToken(api_key));
        }
        chain.doFilter(request, response);
    }
}
