package ua.kidlearn.devseed;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * {@link DevSeedController} is {@code @Profile("dev")}, so under any other profile Spring never
 * registers its bean definition at all — the route doesn't exist and requests 404. No database
 * or web layer is needed to prove that: a plain profile-scoped context is enough.
 */
class DevSeedControllerProfileTest {

	@Test
	void controllerIsAbsentOutsideDevProfile() {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
			context.getEnvironment().setActiveProfiles("prod");
			context.register(DevSeedController.class);
			context.refresh();

			assertThrows(NoSuchBeanDefinitionException.class, () -> context.getBean(DevSeedController.class));
		}
	}

}
