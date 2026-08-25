package ua.kidlearn.twofa;

import jakarta.validation.constraints.NotBlank;

public record TwoFactorCodeRequest(@NotBlank String code) {
}
