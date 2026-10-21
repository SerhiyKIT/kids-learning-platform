package ua.kidlearn.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "auth")
/** @param teacherInviteTtl how long an admin-issued teacher invitation link stays valid (TEACHER_INVITE_TTL) */
public record AuthProperties(Duration emailVerificationTtl, Duration passwordResetTtl,
		@DefaultValue("72h") Duration teacherInviteTtl) {
}
