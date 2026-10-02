package eu.mm.software.photofinder.user.interfaces.rest;

import eu.mm.software.photofinder.user.application.query.UserDto;
import eu.mm.software.photofinder.user.application.query.UserQuery;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static eu.mm.software.photofinder.user.interfaces.rest.ApiConst.USER_URI;

@RequestMapping(USER_URI)
@RestController
@Tag(
    name = "User Queries",
    description = "Endpoints to list users or get a user by ID"
)
class UserListController {

    private final UserQuery userQuery;

    @Autowired
    UserListController(UserQuery userQuery) {
        Assert.notNull(userQuery, "userQuery must not be null");
        this.userQuery = userQuery;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "List all users",
        description = "Retrieves a list of all users in the system."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Users successfully retrieved"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public List<UserDto> listUser() {
        return userQuery.findAll();
    }

    @GetMapping(value = "/{userId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Get a user by ID",
        description = "Retrieves a single user by its unique identifier."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "User successfully retrieved"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    public UserDto getUser(
            @Parameter(description = "Unique identifier of the user") @PathVariable("userId") String userId) {
        return userQuery.findById(userId);
    }
}
