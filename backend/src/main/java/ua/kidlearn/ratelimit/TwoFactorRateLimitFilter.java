package ua.kidlearn.ratelimit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import ua.kidlearn.ratelimit.RateLimitProperties.Limit;

/**
 * Per-account + per-IP request counting (every request counts, not just failures — same
 * reasoning as {@link AuthRateLimitFilter}) on the admin 2FA verify/enable endpoints, so a stolen
 * session cookie can't be used to brute-force TOTP/backup codes. Runs after
 * {@code UsernamePasswordAuthenticationFilter}, since it needs the already-authenticated account
 * identity from {@link SecurityContextHolder}, not a request parameter.
 */
public class TwoFactorRateLimitFilter extends OncePerRequestFilter {

	private static final RequestMatcher VERIFY = PathPatternRequestMatcher.pathPattern(HttpMethod.POST,
			"/api/admin/2fa/verify");
	private static final RequestMatcher ENABLE = PathPatternRequestMatcher.pathPattern(HttpMethod.POST,
			"/api/admin/2fa/enable");

	private final RateLimiter rateLimiter;
	private final ClientIpResolver clientIpResolver;
	private final RateLimitProperties properties;

	public TwoFactorRateLimitFilter(RateLimiter rateLimiter, ClientIpResolver clientIpResolver,
			RateLimitProperties properties) {
		this.rateLimiter = rateLimiter;
		this.clientIpResolver = clientIpResolver;
		this.properties = properties;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		String action = VERIFY.matches(request) ? "verify" : ENABLE.matches(request) ? "enable" : null;
		if (action != null) {
			Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
			String account = authentication != null ? authentication.getName() : "anonymous";
			String clientIp = clientIpResolver.resolve(request);
			Limit perAccount = properties.twoFactorPerAccount();
			Limit perIp = properties.twoFactorPerIp();
			String accountKey = "2fa:" + action + ":account:" + account;
			String ipKey = "2fa:" + action + ":ip:" + clientIp;

			if (rateLimiter.isBlocked(accountKey, perAccount.max(), perAccount.window())
					|| rateLimiter.isBlocked(ipKey, perIp.max(), perIp.window())) {
				long retryAfter = Math.max(rateLimiter.retryAfterSeconds(accountKey, perAccount.window()),
						rateLimiter.retryAfterSeconds(ipKey, perIp.window()));
				RateLimitResponses.writeTooManyRequests(response, retryAfter);
				return;
			}
			rateLimiter.tryAcquire(accountKey, perAccount.max(), perAccount.window());
			rateLimiter.tryAcquire(ipKey, perIp.max(), perIp.window());
		}
		filterChain.doFilter(request, response);
	}

}
