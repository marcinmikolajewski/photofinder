package eu.mm.software.photofinder.photosattribute.interfaces.rest;

import eu.mm.software.photofinder.photosattribute.domain.VectorDBRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("rest/api/v1/photos")
@RequiredArgsConstructor
@Tag(
    name = "Vector Commands",
    description = "Endpoints to save photo vectors to the vector database"
)
public class VectorCommandController {

    private final VectorDBRepository vectorDBRepository;

    @PostMapping(value = "save", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Save photo vectors",
        description = "Sends photo embeddings to the vector database. Optionally, overwrite existing vectors."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "202", description = "Vector saving process accepted and running"),
        @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<Void> describePhotosFromPath(
            @Parameter(description = "Whether to overwrite existing vectors", example = "false")
            @RequestParam(value = "overwrite", defaultValue = "false") Boolean overwrite) {

        vectorDBRepository.sendEmbeddedToQueue(overwrite);

        return ResponseEntity.accepted().build();
    }
}
