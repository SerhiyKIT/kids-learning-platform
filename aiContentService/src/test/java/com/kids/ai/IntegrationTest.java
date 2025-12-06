package com.kids.ai;

import com.kids.ai.config.AsyncSyncConfiguration;
import com.kids.ai.config.EmbeddedMongo;
import com.kids.ai.config.JacksonConfiguration;
import com.kids.ai.config.TestSecurityConfiguration;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Base composite annotation for integration tests.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest(
    classes = { AiContentServiceApp.class, JacksonConfiguration.class, AsyncSyncConfiguration.class, TestSecurityConfiguration.class }
)
@EmbeddedMongo
public @interface IntegrationTest {
}
