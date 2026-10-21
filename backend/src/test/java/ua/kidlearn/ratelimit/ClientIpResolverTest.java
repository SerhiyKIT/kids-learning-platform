package ua.kidlearn.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import ua.kidlearn.ratelimit.RateLimitProperties.Limit;

class ClientIpResolverTest {

	private static ClientIpResolver resolver(boolean trust) {
		Limit l = new Limit(5, Duration.ofMinutes(15));
		return new ClientIpResolver(new RateLimitProperties(trust, l, l, l, l, l, l, l, l, l, l, l));
	}

	private static MockHttpServletRequest request(String forwardedFor) {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setRemoteAddr("172.18.0.5");
		if (forwardedFor != null) {
			request.addHeader("X-Forwarded-For", forwardedFor);
		}
		return request;
	}

	@Test
	void behindOurProxyTheLastAddressCountsAndAForgedPrefixChangesNothing() {
		ClientIpResolver resolver = resolver(true);
		assertThat(resolver.resolve(request("203.0.113.7"))).isEqualTo("203.0.113.7");
		assertThat(resolver.resolve(request("1.2.3.4, 203.0.113.7"))).isEqualTo("203.0.113.7");
		assertThat(resolver.resolve(request("9.9.9.9,8.8.8.8 , 203.0.113.7"))).isEqualTo("203.0.113.7");
	}

	@Test
	void withoutAHeaderOrAnEmptyLastEntryTheConnectionAddressIsUsed() {
		ClientIpResolver resolver = resolver(true);
		assertThat(resolver.resolve(request(null))).isEqualTo("172.18.0.5");
		assertThat(resolver.resolve(request("1.2.3.4, "))).isEqualTo("172.18.0.5");
	}

	@Test
	void notTrustedByDefaultTheHeaderIsIgnored() {
		assertThat(resolver(false).resolve(request("203.0.113.7"))).isEqualTo("172.18.0.5");
	}

}
