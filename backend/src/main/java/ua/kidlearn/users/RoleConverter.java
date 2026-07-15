package ua.kidlearn.users;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Locale;

/** Converts {@link Role} <-> the lowercase text value used by the DB CHECK constraint. */
@Converter
class RoleConverter implements AttributeConverter<Role, String> {

	@Override
	public String convertToDatabaseColumn(Role role) {
		return role == null ? null : role.name().toLowerCase(Locale.ROOT);
	}

	@Override
	public Role convertToEntityAttribute(String dbValue) {
		return dbValue == null ? null : Role.valueOf(dbValue.toUpperCase(Locale.ROOT));
	}

}
