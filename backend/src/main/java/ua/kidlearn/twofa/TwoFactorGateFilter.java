package ua.kidlearn.twofa;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Enforces the mandatory-2FA gate for authenticated ADMIN sessions: until the session reaches
 * {@link TwoFactorStatus#ELEVATED}, only a narrow allowlist of paths is reachable (see
 * {@link TwoFactorStatus}'s javadoc for exactly which, per state). Runs after
 * {@code UsernamePasswordAuthenticationFilter}, so {@link SecurityContextHolder} already holds
 * the authenticated {@link Authentication} for every request it sees. Checks the ROLE_ADMIN
 * {@link org.springframework.security.core.GrantedAuthority} directly rather than the app's
 * {@code AppUserPrincipal} type, deliberately: this package must not depend on
 * {@code ua.kidlearn.auth} (see {@link TwoFactorController}'s javadoc for why). Non-admins —
 * including "not authenticated at all" — pass straight through untouched.
 */
public class TwoFactorGateFilter extends OncePerRequestFilter {

	private static final String ROLE_ADMIN = "ROLE_ADMIN";

	private static final RequestMatcher SETUP = PathPatternRequestMatcher.pathPattern(HttpMethod.POST,
			"/api/admin/2fa/setup");
	private static final RequestMatcher ENABLE = PathPatternRequestMatcher.pathPattern(HttpMethod.POST,
			"/api/admin/2fa/enable");
	private static final RequestMatcher VERIFY = PathPatternRequestMatcher.pathPattern(HttpMethod.POST,
			"/api/admin/2fa/verify");
	private static final RequestMatcher ME = PathPatternRequestMatcher.pathPattern(HttpMethod.GET, "/api/auth/me");
	private static final RequestMatcher LOGOUT = PathPatternRequestMatcher.pathPattern(HttpMethod.POST, "/logout");

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		boolean isAdmin = authentication != null
				&& authentication.getAuthorities().stream().anyMatch(a -> ROLE_ADMIN.equals(a.getAuthority()));
		if (!isAdmin || LOGOUT.matches(request)) {
			filterChain.doFilter(request, response);
			return;
		}

		TwoFactorStatus status = TwoFactorSession.status(request);
		boolean allowed = switch (status) {
			case ELEVATED -> true;
			case SETUP_REQUIRED -> SETUP.matches(request) || ENABLE.matches(request) || ME.matches(request);
			case PENDING_VERIFICATION -> VERIFY.matches(request);
		};

		if (!allowed) {
			writeForbidden(response, status);
			return;
		}
		filterChain.doFilter(request, response);
	}

	private static void writeForbidden(HttpServletResponse response, TwoFactorStatus status) throws IOException {
		String code = status == TwoFactorStatus.SETUP_REQUIRED ? "TWO_FACTOR_SETUP_REQUIRED"
				: "TWO_FACTOR_VERIFICATION_REQUIRED";
		response.setStatus(HttpStatus.FORBIDDEN.value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.getWriter().write("{\"code\":\"" + code + "\"}");
	}

}
