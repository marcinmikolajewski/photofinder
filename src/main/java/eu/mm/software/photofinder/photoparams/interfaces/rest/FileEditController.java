package eu.mm.software.photofinder.photoparams.interfaces.rest;

import eu.mm.software.photofinder.photoparams.application.command.FileApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("rest/api/v1/file")
@RequiredArgsConstructor
@Tag(
    name = "File Description",
    description = "Endpoints to describe files either from local disk or S3 storage"
)
public class FileEditController {

    private final FileApplicationService fileApplicationService;
    private final DescribeFormValidator validator;

    @PostMapping(value = "describe", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Describe photos from local disk",
        description = "Generates descriptions for photos located on the local file system using the specified provider."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "202", description = "Description process accepted and running"),
        @ApiResponse(responseCode = "400", description = "Validation failed or provider not active"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<String> describePhotosFromLocalDisk(
            @Parameter(description = "Form containing files to describe and provider details")
            @RequestBody @Validated DescribeFormDto describeFormDto,
            final BindingResult bindingResult) {

        if (!bindingResult.hasErrors()) {
            fileApplicationService.describePhotos(describeFormDto);
            return ResponseEntity.accepted()
                    .build();
        } else {
            return ResponseEntity.badRequest().body("Provider not active");
        }
    }

    @InitBinder("describeFormDto")
    public void initBinder(WebDataBinder binder) {
        binder.addValidators(validator);
    }
}
