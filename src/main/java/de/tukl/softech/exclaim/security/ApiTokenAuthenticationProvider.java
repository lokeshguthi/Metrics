package de.tukl.softech.exclaim.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Component
public class ApiTokenAuthenticationProvider  implements AuthenticationProvider {
    @Value("${exclaim.apiKey}")
    private String apiKey;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String receivedKey = authentication.getCredentials().toString();
        if (apiKey.equals(receivedKey)) {
            return new UsernamePasswordAuthenticationToken(authentication.getPrincipal(), authentication.getCredentials(), Collections.singleton(new SimpleGrantedAuthority("ROLE_SERVICE")));
        } else {
            throw new BadCredentialsException("Received API key is wrong");
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authentication.equals(ApiTokenFilter.ApiKeyAuthenticationToken.class);
    }
}
