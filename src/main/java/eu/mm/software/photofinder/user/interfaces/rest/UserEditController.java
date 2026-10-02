package eu.mm.software.photofinder.user.interfaces.rest;

import eu.mm.software.photofinder.user.application.command.UserApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import static eu.mm.software.photofinder.user.interfaces.rest.ApiConst.USER_URI;
import static org.springframework.http.HttpStatus.*;

@RequestMapping(USER_URI)
@RestController
@RequiredArgsConstructor
@Tag(
    name = "User Management",
    description = "Endpoints to create, update, and delete users"
)
class UserEditController {

    private final UserApplicationService userApplicationService;
    private final UserValidator userValidator;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Create a new user",
        description = "Creates a new user in the system. Returns the user ID in the Location header."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "User successfully created"),
        @ApiResponse(responseCode = "400", description = "Validation failed"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<String> create(
            @Parameter(description = "User data to create") @RequestBody @Validated UserEditForm userEditForm,
            final BindingResult bindingResult) throws UserValidationException {

        if (bindingResult.hasErrors()) {
            throw new UserValidationException(bindingResult.getAllErrors().toString());
        }

        String userId = userApplicationService.save(userEditForm);

        return ResponseEntity
                .status(CREATED)
                .header(HttpHeaders.LOCATION, userId)
                .build();
    }

    @PutMapping(value = "{userId}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Update an existing user",
        description = "Updates the attributes of an existing user identified by userId."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "User successfully updated"),
        @ApiResponse(responseCode = "400", description = "Validation failed"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    ResponseEntity<String> updateUser(
            @Parameter(description = "ID of the user to update") @PathVariable("userId") String id,
            @Parameter(description = "Updated user attributes") @RequestBody @Validated UserEditForm userFormAttributes,
            final BindingResult bindingResult) throws UserValidationException {

        if (!bindingResult.hasErrors()) {
            userApplicationService.update(userFormAttributes, id);
            return ResponseEntity.status(OK).build();
        }

        throw new UserValidationException(bindingResult.getAllErrors().toString());
    }

    @DeleteMapping(value = "{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Delete a user",
        description = "Deletes the user identified by the provided ID."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "User successfully deleted"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    ResponseEntity<String> delete(
            @Parameter(description = "ID of the user to delete") @PathVariable("id") String id) {

        userApplicationService.delete(id);
        return ResponseEntity.status(NO_CONTENT).build();
    }

    @InitBinder
    void initBinder(WebDataBinder webDataBinder) {
        webDataBinder.addValidators(userValidator);
    }
}
