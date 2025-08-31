package com.ecotrack.filter;

import com.ecotrack.model.User;
import com.ecotrack.repository.UserRepository;
import com.ecotrack.security.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserRepository userRepo;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path = request.getServletPath();

        // Skip JWT validation for public endpoints
        if (path.startsWith("/api/auth") || path.startsWith("/api/hello")
                || path.startsWith("/api/aqi") || path.startsWith("/api/health")
                || path.startsWith("/api/test")) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            try {
                String email = jwtUtil.validateTokenAndGetEmail(token);

                // Load user from DB
                User user = userRepo.findByEmail(email)
                        .orElseThrow(() -> new RuntimeException("User not found: " + email));

                UserDetails userDetails = org.springframework.security.core.userdetails.User
                        .withUsername(user.getEmail())
                        .password(user.getPassword())
                        .roles(user.getRole().name())
                        .build();

                // Set authentication if not already set
                if (SecurityContextHolder.getContext().getAuthentication() == null) {
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }

            } catch (Exception e) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired JWT token");
                return;
            }
        } else {
            // No token for protected endpoints → reject
            if (requiresAuthentication(path)) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing Authorization header");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean requiresAuthentication(String path) {
        return path.startsWith("/api/profile") || path.startsWith("/api/route") || path.startsWith("/api/ml");
    }
}
