package com.backend.appointment.config;

import com.backend.common.security.UserPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            // If this looks like a JWT, let the OAuth2 Resource Server handle it.
            if (token.chars().filter(ch -> ch == '.').count() >= 2) {
                filterChain.doFilter(request, response);
                return;
            }

            try {
                UserPrincipal principal = extractPrincipalFromToken(token);

                if (principal != null) {
                    var auth = new UsernamePasswordAuthenticationToken(
                            principal,
                            null,
                            principal.roles().stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList()));

                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            } catch (Exception e) {
                // Log token validation failure
                // SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }

    private UserPrincipal extractPrincipalFromToken(String token) {
        // [SKELETON LOGIC]
        // This should decode the JWT, check expiration, and header.
        // Returning a mock principal based on the token string for now.
        // In real app: Claims claims = jwtParser.parse(token);
        String userId = token; // Assuming token is userId for this skeleton/mock phase
        return new UserPrincipal(userId, "user@example.com", "User " + userId, Set.of("ROLE_USER"));
    }
}
