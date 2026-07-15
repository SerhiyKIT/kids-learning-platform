package ua.kidlearn.users;

/**
 * Mirrors the {@code users.role} CHECK constraint (parent, teacher, admin).
 * Stored lowercase in the DB via {@link RoleConverter}.
 */
public enum Role {
	PARENT,
	TEACHER,
	ADMIN
}
