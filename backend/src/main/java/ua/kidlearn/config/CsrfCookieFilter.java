package ua.kidlearn.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Forces the deferred CSRF token to resolve on every request, so the
 * XSRF-TOKEN cookie is written on the very first response instead of only
 * as a side effect of rendering a form (e.g. the login page). Without this,
 * a JS client has no reliable way to obtain its first token. Per Spring
 * Security's recommended SPA CSRF setup.
 */
final class CsrfCookieFilter extends OncePerRequestFilter {

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
		if (csrfToken != null) {
			csrfToken.getToken();
		}
		filterChain.doFilter(request, response);
	}

}
