package eu.mm.software.photofinder.user.application.query;

import java.util.Set;



public record UserDto(String id,
                      String userName,
                      String userEmail,
                      Boolean active,
                      Set<String> roles) {

}
