package eu.mm.software.photofinder.user.application.query;

import java.util.List;


public interface UserQuery {

    List<UserDto> findAll();

    UserDto findById(String userId);

    String findLoggedUserId();

    String findUserNameByUserId(String userId);
}