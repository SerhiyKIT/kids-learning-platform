package ua.kidlearn.auth;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import ua.kidlearn.users.User;
import ua.kidlearn.users.UserRepository;

@Service
public class AppUserDetailsService implements UserDetailsService {

	private final UserRepository userRepository;

	public AppUserDetailsService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@Override
	public UserDetails loadUserByUsername(String email) {
		User user = userRepository.findByEmailAndDeletedAtIsNull(email)
				.orElseThrow(() -> new UsernameNotFoundException("No user with email: " + email));
		if (user.getPasswordHash() == null) {
			// External-only account (e.g. future OAuth2 login): no password credentials.
			throw new UsernameNotFoundException("No password credentials for email: " + email);
		}
		return new AppUserPrincipal(user);
	}

}
