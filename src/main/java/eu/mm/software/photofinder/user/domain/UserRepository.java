package eu.mm.software.photofinder.user.domain;




import java.util.Optional;

public interface UserRepository {

	String save (UserFormAttributes userFormAttributes);

	void update(UserFormAttributes userFormAttributes, String userId);

	void delete (String id);
	
	Optional<User> findByUserName(String name);
}
