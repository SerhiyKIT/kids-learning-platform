package ua.kidlearn.users;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.nio.charset.StandardCharsets;

/**
 * A new password must fit BCrypt's input: at most 72 bytes of UTF-8. Longer input makes the
 * encoder throw (a 500 instead of a validation error), and a Cyrillic passphrase reaches the limit
 * at 37 letters. Put next to the minimum-length rule on every field that sets a password.
 */
@Documented
@Constraint(validatedBy = FitsBcrypt.Validator.class)
@Target({ElementType.FIELD, ElementType.RECORD_COMPONENT, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface FitsBcrypt {

	int MAX_BYTES = 72;

	String message() default "password is longer than 72 bytes";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};

	class Validator implements ConstraintValidator<FitsBcrypt, String> {

		@Override
		public boolean isValid(String value, ConstraintValidatorContext context) {
			return value == null || value.getBytes(StandardCharsets.UTF_8).length <= MAX_BYTES;
		}

	}

}
