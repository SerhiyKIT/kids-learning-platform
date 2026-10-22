package ua.kidlearn.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.orm.jpa.support.OpenEntityManagerInViewInterceptor;
import ua.kidlearn.support.WebIntegrationTest;

/**
 * QA1-005: Spring's open-in-view (on by default) keeps a JPA session — and its pooled connection —
 * for the whole request, including the time a slow client takes to read the response.
 */
class OpenInViewTest extends WebIntegrationTest {

	@Autowired
	private ApplicationContext context;

	@Test
	void noSessionIsHeldOpenForTheWholeRequest() {
		assertThat(context.getBeansOfType(OpenEntityManagerInViewInterceptor.class)).isEmpty();
	}

}
