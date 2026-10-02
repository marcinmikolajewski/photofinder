package eu.mm.software.photofinder.common.security.service;

import eu.mm.software.photofinder.user.infrastructure.mongodb.UserSpringDataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.List;
import java.util.Objects;


@Service
public class MongoUserDetailsService implements UserDetailsService {

	private final UserSpringDataRepository repository;

	@Autowired
	public MongoUserDetailsService(UserSpringDataRepository repository) {

		Assert.notNull(repository, "repository must not be null");

		this.repository = repository;
	}

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

		eu.mm.software.photofinder.user.domain.User user = repository.findByUserEmail(username);

		if (Objects.isNull(user)) {
			throw new UsernameNotFoundException("User not found");
		}

		List<SimpleGrantedAuthority> authorities =
				user.getRoles().stream()
						.map(Enum::name)
						.map(SimpleGrantedAuthority::new)
						.toList();

		return new User(user.getUserEmail(), user.getPassword(), authorities);
	}
}
