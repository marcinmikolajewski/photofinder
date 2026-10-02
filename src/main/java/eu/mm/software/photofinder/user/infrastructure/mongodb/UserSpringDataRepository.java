package eu.mm.software.photofinder.user.infrastructure.mongodb;

import eu.mm.software.photofinder.user.domain.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface UserSpringDataRepository extends MongoRepository<User, String> {

	User findByUserName(String username);

	User findByUserEmail(String userEmail);

}
