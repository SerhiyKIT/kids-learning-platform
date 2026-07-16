package ua.kidlearn.attempts;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

public record CompleteAttemptRequest(@NotBlank String result, BigDecimal score) {
}
