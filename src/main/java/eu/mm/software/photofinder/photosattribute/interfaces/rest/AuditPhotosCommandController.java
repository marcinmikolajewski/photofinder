package eu.mm.software.photofinder.photosattribute.interfaces.rest;

import eu.mm.software.photofinder.photosattribute.application.command.AuditPhotosApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("rest/api/v1/audit")
@RequiredArgsConstructor
@Tag(
    name = "Audit Photos",
    description = "Endpoints to manage audit and cleanup operations for photos"
)
public class AuditPhotosCommandController {

    private final AuditPhotosApplicationService auditPhotosApplicationService;

    @DeleteMapping(value = "missing", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Remove missing photos from audit",
        description = "Deletes audit records of photos that are missing or unlinked from the system."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "202", description = "Deletion request accepted and in progress"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<Void> describePhotosFromPath() {
        auditPhotosApplicationService.removeUnlinkAuditPhotos();
        return ResponseEntity.accepted().build();
    }
}
