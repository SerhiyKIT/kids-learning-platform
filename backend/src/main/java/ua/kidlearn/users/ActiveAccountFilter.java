package ua.kidlearn.users;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Ends an already-open session whose account has since been soft-deleted (e.g. a deactivated
 * teacher): login itself is refused by AppUserDetailsService, but the session authenticated
 * before the deactivation would otherwise keep working until it expires. One primary-key-style
 * lookup per authenticated request — simpler and more reliable than tracking every session in a
 * registry, and it also catches sessions on other nodes. Looks the user up by the principal's
 * name (the email) rather than the app's principal type, so this package doesn't depend on
 * ua.kidlearn.auth (which depends on it).
 *
 * <p>The same lookup ends sessions authenticated before the account's sessions_valid_after
 * ("sign out everywhere", password change or reset; see SessionAuthentication).
 */
public class ActiveAccountFilter extends OncePerRequestFilter {

	private final UserRepository userRepository;

	public ActiveAccountFilter(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		boolean authenticated = authentication != null && authentication.isAuthenticated()
				&& !(authentication instanceof AnonymousAuthenticationToken);
		if (authenticated && !isStillValid(authentication.getName(), request.getSession(false))) {
			HttpSession session = request.getSession(false);
			if (session != null) {
				session.invalidate();
			}
			SecurityContextHolder.clearContext();
			response.setStatus(HttpStatus.UNAUTHORIZED.value());
			return;
		}
		filterChain.doFilter(request, response);
	}

	private boolean isStillValid(String email, HttpSession session) {
		return userRepository.findActiveByEmail(email)
				.map(user -> session == null || !SessionAuthentication.isStale(session, user.getSessionsValidAfter()))
				.orElse(false);
	}

}
