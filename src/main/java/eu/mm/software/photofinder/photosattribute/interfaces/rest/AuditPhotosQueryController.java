package eu.mm.software.photofinder.photosattribute.interfaces.rest;

import eu.mm.software.photofinder.photosattribute.application.query.AuditPhotoDto;
import eu.mm.software.photofinder.photosattribute.application.query.AuditPhotoStatsDto;
import eu.mm.software.photofinder.photosattribute.application.query.AuditPhotosQuery;
import eu.mm.software.photofinder.photosattribute.application.query.AuditSummaryDto;
import eu.mm.software.photofinder.photosattribute.domain.AiProvider;
import eu.mm.software.photofinder.user.application.query.UserQuery;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("rest/api/v1/audit")
@RequiredArgsConstructor
@Tag(
        name = "Audit Photos Queries",
        description = "Endpoints to query audit information for photos"
)
public class AuditPhotosQueryController {

    private final AuditPhotosQuery auditPhotosQuery;
    private final UserQuery userQuery;


    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Get all audit photos for logged user",
            description = "Retrieves all audit photo records associated with the currently logged-in user."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Audit photos retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<Set<AuditPhotoDto>> findAll() {
        Set<AuditPhotoDto> auditPhotoDtos = auditPhotosQuery.findByLoggedUser();
        return ResponseEntity.ok()
                .header("X-Total-Count", "" + auditPhotoDtos.size())
                .body(auditPhotoDtos);
    }

    @GetMapping(value = "stats", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Get audit statistics",
            description = "Provides statistics about audit photos, grouped by status or other attributes."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Audit statistics retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<Map<String, AuditPhotoStatsDto>> getStats() {
        Map<String, AuditPhotoStatsDto> stats = auditPhotosQuery.getStats();
        return ResponseEntity.ok().body(stats);
    }

    @GetMapping(value = "empty", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Get IDs of photos with empty description",
            description = "Retrieves IDs of photos that do not have a description in the audit records."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Empty photo IDs retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<Set<String>> getEmptyPhoto() {
        Set<String> ids = auditPhotosQuery.findIdPhotosWithEmptyDescription();
        return ResponseEntity.ok()
                .header("X-Total-Count", "" + ids.size())
                .body(ids);
    }

    @GetMapping(value = "/{provider}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Get audit photos by provider",
            description = "Retrieves audit photo records filtered by the specified provider for the logged-in user."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Audit photos for provider retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<Set<AuditPhotoDto>> findAllByUserAndProvider(
            @Parameter(description = "Provider name to filter audit photos")
            @PathVariable("provider") AiProvider provider) {

        Set<AuditPhotoDto> auditPhotoDtos = auditPhotosQuery.findByUserEmailAndProvider(provider);
        return ResponseEntity.ok()
                .header("X-Total-Count", "" + auditPhotoDtos.size())
                .body(auditPhotoDtos);
    }


    @GetMapping("/photos")
    public ResponseEntity<List<AuditPhotoDto>> findByUserId() {
        return ResponseEntity.ok(auditPhotosQuery.findByUserId(userQuery.findLoggedUserId()));
    }


    @GetMapping("/summary")
    public ResponseEntity<List<AuditSummaryDto>> summarizeByProvider() {
        return ResponseEntity.ok(auditPhotosQuery.summarizeByProvider(userQuery.findLoggedUserId()));
    }
}
