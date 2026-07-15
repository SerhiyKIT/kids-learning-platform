package ua.kidlearn.auth;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Locale;

/** Converts {@link TokenType} <-> the lowercase text value used by the DB CHECK constraint. */
@Converter
class TokenTypeConverter implements AttributeConverter<TokenType, String> {

	@Override
	public String convertToDatabaseColumn(TokenType type) {
		return type == null ? null : type.name().toLowerCase(Locale.ROOT);
	}

	@Override
	public TokenType convertToEntityAttribute(String dbValue) {
		return dbValue == null ? null : TokenType.valueOf(dbValue.toUpperCase(Locale.ROOT));
	}

}
