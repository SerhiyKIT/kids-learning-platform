package ua.kidlearn.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * @param baseUrl the public origin (https in prod) every link in an email is built from
 * @param mailFrom the From address of auth emails; blank = the SMTP server's default
 * @param security cookie hardening
 */
@ConfigurationProperties(prefix = "app")
public record AppProperties(String baseUrl, String mailFrom, @DefaultValue Security security) {

	/** @param secureCookies Secure flag on the session and CSRF cookies (HTTPS-only deployments) */
	public record Security(@DefaultValue("false") boolean secureCookies) {
	}

}
