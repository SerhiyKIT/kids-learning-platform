package ua.kidlearn.auth;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import ua.kidlearn.users.Role;
import ua.kidlearn.users.User;

/** Security principal wrapping the persisted {@link User}, exposing id/role to controllers. */
public class AppUserPrincipal implements UserDetails {

	private final UUID id;
	private final String email;
	private final String passwordHash;
	private final Role role;

	public AppUserPrincipal(User user) {
		this.id = user.getId();
		this.email = user.getEmail();
		this.passwordHash = user.getPasswordHash();
		this.role = user.getRole();
	}

	public UUID getId() {
		return id;
	}

	public Role getRole() {
		return role;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
	}

	@Override
	public String getPassword() {
		return passwordHash;
	}

	@Override
	public String getUsername() {
		return email;
	}

}
