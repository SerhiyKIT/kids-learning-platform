package ua.kidlearn.ratelimit;

import org.springframework.stereotype.Service;
import ua.kidlearn.ratelimit.RateLimitProperties.Limit;

/**
 * Failure-counting login throttle: tracks failed logins per account and per client IP, and resets
 * an account's counter on a successful login (a legit user who finally gets their password right
 * isn't left locked out). Deliberately does NOT reset the IP counter on success — a shared IP
 * (NAT, office network) with one legitimate login shouldn't erase brute-force signal against
 * other accounts from that same IP.
 */
@Service
public class LoginAttemptService {

	private final RateLimiter rateLimiter;
	private final RateLimitProperties properties;

	public LoginAttemptService(RateLimiter rateLimiter, RateLimitProperties properties) {
		this.rateLimiter = rateLimiter;
		this.properties = properties;
	}

	public boolean isBlocked(String username, String clientIp) {
		Limit perAccount = properties.loginPerAccount();
		Limit perIp = properties.loginPerIp();
		return rateLimiter.isBlocked(accountKey(username), perAccount.max(), perAccount.window())
				|| rateLimiter.isBlocked(ipKey(clientIp), perIp.max(), perIp.window());
	}

	public void recordFailure(String username, String clientIp) {
		Limit perAccount = properties.loginPerAccount();
		Limit perIp = properties.loginPerIp();
		rateLimiter.tryAcquire(accountKey(username), perAccount.max(), perAccount.window());
		rateLimiter.tryAcquire(ipKey(clientIp), perIp.max(), perIp.window());
	}

	public void recordSuccess(String username) {
		rateLimiter.reset(accountKey(username));
	}

	public long retryAfterSeconds(String username, String clientIp) {
		long accountRetry = rateLimiter.retryAfterSeconds(accountKey(username), properties.loginPerAccount().window());
		long ipRetry = rateLimiter.retryAfterSeconds(ipKey(clientIp), properties.loginPerIp().window());
		return Math.max(accountRetry, ipRetry);
	}

	private static String accountKey(String username) {
		return "login:account:" + username;
	}

	private static String ipKey(String clientIp) {
		return "login:ip:" + clientIp;
	}

}
