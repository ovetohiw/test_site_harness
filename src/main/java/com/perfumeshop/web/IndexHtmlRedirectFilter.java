package com.perfumeshop.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class IndexHtmlRedirectFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (isDirectIndexHtml(request)) {
            response.setStatus(HttpServletResponse.SC_FOUND);
            response.setHeader("Location", "/");
            return;
        }
        filterChain.doFilter(request, response);
    }

    private static boolean isDirectIndexHtml(HttpServletRequest request) {
        if (!"GET".equalsIgnoreCase(request.getMethod())) {
            return false;
        }
        if (!"/index.html".equals(request.getRequestURI())) {
            return false;
        }
        return request.getAttribute(RequestDispatcher.FORWARD_REQUEST_URI) == null
                && request.getAttribute(RequestDispatcher.INCLUDE_REQUEST_URI) == null;
    }
}
