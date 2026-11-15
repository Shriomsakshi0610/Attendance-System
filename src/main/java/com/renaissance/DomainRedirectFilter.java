package com.renaissance;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class DomainRedirectFilter extends OncePerRequestFilter {

    private static final String OLD_HOST =
            "https://attendance-system-ivna.onrender.com";

    private static final String NEW_DOMAIN =
            "https://relianceattendance.site";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String host = request.getServerName();
        String path = request.getRequestURI();
        String method = request.getMethod();

        boolean isOldDomain = OLD_HOST.equalsIgnoreCase(host);

        boolean isWebPageRequest =
                method.equals("GET") || method.equals("HEAD");

        boolean isApiRequest =
                path.startsWith("/api/") ||
                path.startsWith("/actuator/");

        if (isOldDomain && isWebPageRequest && !isApiRequest) {

            String newUrl = NEW_DOMAIN + path;

            if (request.getQueryString() != null) {
                newUrl += "?" + request.getQueryString();
            }

            response.setStatus(301);
            response.setHeader("Location", newUrl);
            return;
        }

        filterChain.doFilter(request, response);
    }
}