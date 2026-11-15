package com.renaissance;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * DomainRedirectFilter
 *
 * Implements an HTTP 301 (Permanent Redirect) from deprecated Render domains
 * (e.g. *.onrender.com) to the new canonical custom domain (e.g. https://renaissanceattendance.site).
 *
 * Executes before Spring Security (Order: Ordered.HIGHEST_PRECEDENCE + 10)
 * to ensure that browser page navigation on old domains is immediately redirected
 * to the canonical domain before authentication or session checks occur.
 *
 * Designed specifically for Render reverse-proxy deployments:
 * - Reads X-Forwarded-Host, Host, and request.getServerName().
 * - Avoids redirect loops by verifying requests are not already on the target domain.
 * - Exempts health checks (/health, /api/public/health, /actuator/**) so Render monitoring is never disrupted.
 * - Exempts API requests (/api/**) and non-GET/HEAD methods (POST, PUT, DELETE, OPTIONS) to preserve
 *   API integrity, CORS preflight, attendance verification, and mobile app compatibility.
 * - Exempts local/test hosts (localhost, 127.0.0.1, etc.).
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class DomainRedirectFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(DomainRedirectFilter.class);

    @Value("${app.redirect.enabled:true}")
    private boolean redirectEnabled = true;

    @Value("${app.redirect.target-domain:renaissanceattendance.site}")
    private String targetDomain = "renaissanceattendance.site";

    @Value("${app.redirect.target-scheme:https}")
    private String targetScheme = "https";

    @Value("${app.redirect.old-domains:attendance-system-ivna.onrender.com,attendance-system-gfr7.onrender.com}")
    private String oldDomainsConfig = "attendance-system-ivna.onrender.com,attendance-system-gfr7.onrender.com";

    private Set<String> configuredOldDomains;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        if (!redirectEnabled) {
            filterChain.doFilter(request, response);
            return;
        }

        String method = request.getMethod();
        // 1. Only redirect idempotent read requests (GET, HEAD). Never redirect POST, PUT, DELETE, PATCH, or OPTIONS (CORS preflight).
        if (!"GET".equalsIgnoreCase(method) && !"HEAD".equalsIgnoreCase(method)) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();
        if (path == null || path.isEmpty()) {
            path = "/";
        }

        // 2. Health check endpoints must NEVER be redirected (Render deployment monitoring)
        if (isHealthCheckEndpoint(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 3. API endpoints and WebSockets must NEVER be redirected
        if (isApiOrWebSocketEndpoint(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 4. Resolve the client's requested hostname behind Render's reverse proxy
        String host = resolveHost(request);

        // 5. Check if the request is arriving on a deprecated old domain
        if (isOldDomain(host)) {
            String cleanTarget = cleanHost(targetDomain);
            String scheme = (targetScheme != null && !targetScheme.trim().isEmpty()) ? targetScheme.trim() : "https";
            String targetBase = scheme + "://" + cleanTarget;

            String queryString = request.getQueryString();
            if ((queryString == null || queryString.trim().isEmpty()) 
                    && request.getParameterMap() != null && !request.getParameterMap().isEmpty()) {
                queryString = request.getParameterMap().entrySet().stream()
                        .flatMap(entry -> Arrays.stream(entry.getValue())
                                .map(val -> java.net.URLEncoder.encode(entry.getKey(), java.nio.charset.StandardCharsets.UTF_8) + "=" 
                                        + java.net.URLEncoder.encode(val, java.nio.charset.StandardCharsets.UTF_8)))
                        .collect(Collectors.joining("&"));
            }
            String redirectUrl = targetBase + path + (queryString != null && !queryString.trim().isEmpty() ? "?" + queryString.trim() : "");

            log.info("Redirecting request from deprecated host [{}] to canonical URL [{}] (301 Moved Permanently)", host, redirectUrl);

            response.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY); // 301
            response.setHeader("Location", redirectUrl);
            response.setHeader("Cache-Control", "public, max-age=86400");
            return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Resolves the original host requested by the client behind Render's reverse proxy.
     */
    public String resolveHost(HttpServletRequest request) {
        // Priority 1: X-Forwarded-Host header injected by reverse proxy / CDN
        String forwardedHost = request.getHeader("X-Forwarded-Host");
        if (forwardedHost != null && !forwardedHost.trim().isEmpty()) {
            String primaryHost = forwardedHost.split(",")[0].trim();
            return cleanHost(primaryHost);
        }

        // Priority 2: Standard Host header
        String hostHeader = request.getHeader("Host");
        if (hostHeader != null && !hostHeader.trim().isEmpty()) {
            String primaryHost = hostHeader.split(",")[0].trim();
            return cleanHost(primaryHost);
        }

        // Priority 3: Servlet container server name
        return cleanHost(request.getServerName());
    }

    /**
     * Determines whether the given host is an old/deprecated domain that requires redirection.
     */
    public boolean isOldDomain(String host) {
        if (host == null || host.isEmpty()) {
            return false;
        }

        String cleanedHost = cleanHost(host);
        String cleanedTarget = cleanHost(targetDomain);

        // A. Prevent redirect loops: if already on target domain or www.target-domain, do not redirect
        if (cleanedHost.equalsIgnoreCase(cleanedTarget) || cleanedHost.equalsIgnoreCase("www." + cleanedTarget)) {
            return false;
        }

        // B. Do not redirect local, development, or internal network hostnames
        if (isLocalOrInternalHost(cleanedHost)) {
            return false;
        }

        // C. Match configured old domains list
        if (getConfiguredOldDomains().contains(cleanedHost)) {
            return true;
        }

        // D. Match any Render deployment subdomain (*.onrender.com)
        if (cleanedHost.endsWith(".onrender.com")) {
            return true;
        }

        return false;
    }

    /**
     * Normalizes a hostname by trimming, lowercasing, and stripping protocols, ports, and trailing slashes.
     */
    public static String cleanHost(String rawHost) {
        if (rawHost == null) {
            return "";
        }
        String host = rawHost.trim().toLowerCase();

        // Strip scheme if present
        if (host.startsWith("https://")) {
            host = host.substring(8);
        } else if (host.startsWith("http://")) {
            host = host.substring(7);
        }

        // Strip port if present
        int colonIdx = host.indexOf(':');
        if (colonIdx != -1) {
            host = host.substring(0, colonIdx);
        }

        // Strip trailing slashes
        while (host.endsWith("/")) {
            host = host.substring(0, host.length() - 1);
        }

        return host.trim();
    }

    private boolean isLocalOrInternalHost(String host) {
        return host.equals("localhost")
                || host.equals("127.0.0.1")
                || host.equals("::1")
                || host.equals("0.0.0.0")
                || host.endsWith(".local")
                || host.endsWith(".internal");
    }

    private boolean isHealthCheckEndpoint(String path) {
        return path.equals("/health")
                || path.equals("/api/public/health")
                || path.startsWith("/actuator");
    }

    private boolean isApiOrWebSocketEndpoint(String path) {
        return path.startsWith("/api/")
                || path.startsWith("/ws");
    }

    private synchronized Set<String> getConfiguredOldDomains() {
        if (configuredOldDomains == null) {
            if (oldDomainsConfig == null || oldDomainsConfig.trim().isEmpty()) {
                configuredOldDomains = new HashSet<>();
            } else {
                configuredOldDomains = Arrays.stream(oldDomainsConfig.split(","))
                        .map(DomainRedirectFilter::cleanHost)
                        .filter(s -> !s.isEmpty())
                        .collect(Collectors.toSet());
            }
        }
        return configuredOldDomains;
    }

    // Getters & Setters for programmatic configuration / testing
    public boolean isRedirectEnabled() {
        return redirectEnabled;
    }

    public void setRedirectEnabled(boolean redirectEnabled) {
        this.redirectEnabled = redirectEnabled;
    }

    public String getTargetDomain() {
        return targetDomain;
    }

    public void setTargetDomain(String targetDomain) {
        this.targetDomain = targetDomain;
    }

    public String getTargetScheme() {
        return targetScheme;
    }

    public void setTargetScheme(String targetScheme) {
        this.targetScheme = targetScheme;
    }

    public String getOldDomainsConfig() {
        return oldDomainsConfig;
    }

    public synchronized void setOldDomainsConfig(String oldDomainsConfig) {
        this.oldDomainsConfig = oldDomainsConfig;
        this.configuredOldDomains = null;
    }
}
