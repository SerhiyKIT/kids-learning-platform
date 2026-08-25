package ua.kidlearn.twofa;

public record TwoFactorSetupResponse(String otpauthUri, String secretBase32) {
}
