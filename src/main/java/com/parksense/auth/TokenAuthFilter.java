package com.parksense.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * The HTTP edge of authentication: reads {@code Authorization: Bearer ...},
 * resolves the session user and publishes it to {@link RequestRoles}.
 *
 * Open paths: the login endpoint and all static SPA files. Everything under
 * {@code /api} else requires a token. The token service is injected as an
 * {@code ObjectProvider} because servlet filters are created before the
 * regular singleton phase.
 */
@Component
public final class TokenAuthFilter extends OncePerRequestFilter {

    private final ObjectProvider<TokenService> tokens;

    public TokenAuthFilter(ObjectProvider<TokenService> tokens) {
        this.tokens = tokens;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !path.startsWith("/api")
                || path.equals("/api/auth/login")
                || path.equals("/api/system/health");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            var user = tokens.getObject().resolve(header.substring(7));
            if (user.isPresent()) {
                RequestRoles.set(user.get());
                try {
                    chain.doFilter(request, response);
                } finally {
                    RequestRoles.clear();
                }
                return;
            }
        }
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"Authentication required\"}");
    }
}
