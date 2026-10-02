package eu.mm.software.photofinder.photosattribute.interfaces.rest;

import eu.mm.software.photofinder.photosattribute.application.query.VectorQuery;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("rest/api/v1/photos")
@RequiredArgsConstructor
@Tag(
    name = "Vector Search",
    description = "Endpoints for vector-based photo search using prompts"
)
public class VectorQueryController {

    private final VectorQuery vectorQuery;

    @GetMapping(value = "search", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Search photos by vector prompt",
        description = "Performs a vector search on photos using the given prompt. Optionally, limit the number of results."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Search successfully executed"),
        @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<List<String>> getPath(
            @Parameter(description = "Text prompt for vector search", example = "sunset landscape")
            @RequestParam("prompt") String prompt,
            @Parameter(description = "Maximum number of results to return", example = "25")
            @RequestParam(value = "limit", required = false) Integer limit,
            @RequestParam(value = "provider", required = false) String provider,
            @RequestParam(value = "path", required = false) String path,
            @RequestParam(value = "model", required = false) String model) {

        List<String> search = vectorQuery.search(prompt, provider, model, path, Optional.ofNullable(limit)
                .orElse(25));

        return ResponseEntity.ok()
                .header("X-Total-Count", "" + search.size())
                .body(search);
    }
}
