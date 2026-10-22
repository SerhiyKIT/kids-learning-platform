package ua.kidlearn.attempts;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

/** @param score share of scenes right on the first try, 0..1 (QA1-015) */
public record CompleteAttemptRequest(@NotBlank String result, @DecimalMin("0") @DecimalMax("1") BigDecimal score) {
}
