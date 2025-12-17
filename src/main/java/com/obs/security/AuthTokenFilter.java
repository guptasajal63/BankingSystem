package com.obs.security;

import com.obs.security.services.UserDetailsServiceImpl;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class AuthTokenFilter extends OncePerRequestFilter {
    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    private static final Logger logger = LoggerFactory.getLogger(AuthTokenFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String jwt = parseJwt(request);
            if (jwt != null && jwtUtils.validateJwtToken(jwt)) {
                String username = jwtUtils.getUserNameFromJwtToken(jwt);

                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);

                // Sliding Window Expiration:
                // Check if token is expiring in less than 5 minutes (300000ms). If so, refresh it.
                // Or simply refresh on every request to keep session alive max time.
                // Let's refresh if < 50% of time remains or just generally close to expiry to avoid spamming headers.
                // For simplicity and user requirement "if active do not logout", refreshing if < 10 mins remains seems good for 15 min expiry.

                java.util.Date expiration = jwtUtils.getExpirationDateFromJwtToken(jwt);
                long timeRemaining = expiration.getTime() - System.currentTimeMillis();

                // If time remaining is less than 10 minutes, issue a fresh token (which resets to 15 mins)
                if (timeRemaining < 600000) {
                    String newToken = jwtUtils.generateTokenFromUsername(username);
                    response.setHeader("Token-Refresh", newToken);
                    // Also expose this header so frontend can read it
                    response.setHeader("Access-Control-Expose-Headers", "Token-Refresh");
                }
            }
        } catch (Exception e) {
            logger.error("Cannot set user authentication: {}", e);
        }

        filterChain.doFilter(request, response);
    }

    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");

        if (headerAuth != null && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7);
        }

        return null;
    }
}
