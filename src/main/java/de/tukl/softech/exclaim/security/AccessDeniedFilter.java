package de.tukl.softech.exclaim.security;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.filter.GenericFilterBean;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Objects;

/**
 * Makes sure that Javascript requests get a correct error code response
 * when not authenticated, while normal requests are redirected to the login page.
 * <p>
 * Adapted from https://stackoverflow.com/a/46515905/303637
 */
public class AccessDeniedFilter extends GenericFilterBean {

    @Override
    public void doFilter(
            ServletRequest request,
            ServletResponse response, FilterChain filterChain) throws IOException, ServletException {

        try {
            filterChain.doFilter(request, response);
        } catch (AccessDeniedException e) {
            handleAccessDenied(request, response);
        }
    }

    private void handleAccessDenied(ServletRequest request, ServletResponse response) throws IOException {
        HttpServletRequest rq = (HttpServletRequest) request;
        HttpServletResponse rs = (HttpServletResponse) response;

        if (isAjax(rq)) {
            rs.sendError(HttpStatus.FORBIDDEN.value());
        } else {
            rs.sendRedirect(rq.getContextPath() + "/login");
        }
    }

    private Boolean isAjax(HttpServletRequest request) {
        return Objects.equals(request.getHeader("X-Requested-With"), "XMLHttpRequest");
    }
}
