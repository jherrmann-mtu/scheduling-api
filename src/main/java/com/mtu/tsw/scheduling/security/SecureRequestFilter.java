package com.mtu.tsw.scheduling.security;

import java.io.IOException;
import java.util.Optional;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.mtu.tsw.scheduling.exceptions.ExceptionMapper.ApiErrorResponse;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
public class SecureRequestFilter extends OncePerRequestFilter {

    private final ObjectMapper mapper = new ObjectMapper();

    private final JwtService jwtService;
    private final UserDetailsService detailsService;
    private String[] publicPaths = {};
    
    public SecureRequestFilter(JwtService jwtService, UserDetailsService detailsService) {
        this.jwtService = jwtService;
        this.detailsService = detailsService;
    }

    public void setPublicPaths(String... paths) {
        if (paths != null) {
            publicPaths = paths;
        }
    }

    private boolean pathIsPublic(String target) {
        for (String path : publicPaths) {
            if (path.equalsIgnoreCase(target))
                return true;
        }
        return false;
    }

    private void writeApiErrorResponse(HttpServletResponse response, int status, String msg) throws JacksonException, IOException {
        response.setStatus(status);
        response.getWriter().write(mapper.writeValueAsString(new ApiErrorResponse(status, msg)));
    }


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        if (pathIsPublic(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }

        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.contains("Bearer ")) {
            writeApiErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "No token present");
            return;
        }

        String token = auth.substring(7);

        if (!jwtService.isTokenUnexpired(token)) {
            writeApiErrorResponse(response, HttpServletResponse.SC_FORBIDDEN, "Expired token");
            return;
        }

        Optional<String> username = jwtService.getUsername(token);
        if (username.isEmpty()) {
            writeApiErrorResponse(response, HttpServletResponse.SC_FORBIDDEN, "Invalid token");
            return;
        }

        UserDetails details = detailsService.loadUserByUsername(username.get());
        if (details == null) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write("Invalid token");
            return;
        }

        // token is valid, store user information and allow request to continue
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities()));
        filterChain.doFilter(request, response);

    }

    
}
