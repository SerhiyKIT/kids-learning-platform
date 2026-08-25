package ua.kidlearn.twofa;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Test-only helper: completes mandatory admin 2FA setup+enable for an already-logged-in ADMIN
 * session (see ua.kidlearn.twofa), so other feature tests can get an ELEVATED session and
 * exercise their own admin endpoints without re-testing 2FA itself — that's TwoFactorFlowTest's
 * job. Uses the real system clock (not a fixed test Clock), so it works in any test class without
 * that class needing to inject/override Clock just to log an admin in. Every call uses its own
 * simulated IP (see TwoFactorRateLimitFilter): this helper is called from many unrelated test
 * classes across the suite, and without that they'd all share MockMvc's default remote address
 * and blow through the per-IP /2fa/enable rate limit long before any of them intended to test it.
 */
public final class AdminTwoFactorTestSupport {

	private static final DefaultCodeGenerator CODE_GENERATOR = new DefaultCodeGenerator();

	private AdminTwoFactorTestSupport() {
	}

	public static void completeSetup(MockMvc mockMvc, MockHttpSession session) throws Exception {
		String ip = "ip-" + UUID.randomUUID();
		MvcResult setupResult = mockMvc.perform(post("/api/admin/2fa/setup").with(csrf()).session(session)
						.header("X-Forwarded-For", ip))
				.andExpect(status().isOk())
				.andReturn();
		String secret = JsonPath.read(setupResult.getResponse().getContentAsString(), "$.secretBase32");
		String code = CODE_GENERATOR.generate(secret, Instant.now().getEpochSecond() / 30);
		mockMvc.perform(post("/api/admin/2fa/enable").with(csrf()).session(session)
						.header("X-Forwarded-For", ip)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"code\":\"%s\"}".formatted(code)))
				.andExpect(status().isCreated());
	}

}
