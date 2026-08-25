package ua.kidlearn.bootstrap;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param adminToken shared secret required to create the first ADMIN account via
 * {@link BootstrapController}. Blank or unset disables the endpoint entirely (see
 * {@link BootstrapService}) — set it at deploy time via the {@code ADMIN_BOOTSTRAP_TOKEN} env
 * var. It can be rotated or removed once the first admin exists: the endpoint stays permanently
 * closed after that regardless of this value.
 */
@ConfigurationProperties(prefix = "app.bootstrap")
public record BootstrapProperties(String adminToken) {
}
