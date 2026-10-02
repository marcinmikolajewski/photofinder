package eu.mm.software.photofinder.photosattribute.interfaces.rest;

import eu.mm.software.photofinder.common.security.service.CurrentUserService;
import eu.mm.software.photofinder.photosattribute.application.command.PhotosApplicationService;
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

@RestController
@RequestMapping("rest/api/v1/photos/")
@RequiredArgsConstructor
@Tag(name = "Photos Commands", description = "Administrative operations for maintaining photo records")
@Slf4j
public class PhotoCommandController {

    private final PhotosApplicationService service;
    private final CurrentUserService currentUserService;

    @Operation(
            summary = "Delete duplicate photos",
            description = "Scans the database and removes duplicate photo records."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "Request accepted, operation in progress"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @DeleteMapping(value = "duplicates", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> deleteDuplicates() {

        String userId = currentUserService.currentUserId();
        log.info("Admin action: delete duplicate photos (userId={})", userId);
        service.deleteDuplicates(userId);

        return ResponseEntity
                .accepted()
                .build();
    }

    @DeleteMapping(value = "files", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Delete database records for missing files",
            description = "Removes photo records from the database when the corresponding files are no longer present on disk."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "Request accepted, operation in progress"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<Void> deleteDescribePhotosFromDBWhenWasDeleteFromDisk() {

        log.info("Admin action: delete DB records for photos missing on disk (userId={})",
                currentUserService.currentUserId());
        service.deleteDescribePhotosFromDBWhenWasDeleteFromDisk();

        return ResponseEntity.accepted()
                .build();
    }

    @DeleteMapping(value = "fix", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Delete photos with error status",
            description = "Cleans up the database by removing photo records that are marked with an error status."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "Request accepted, operation in progress"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<Void> deletePhotosFromDBWhenStatusISError() {

        log.info("Admin action: delete photos with ERROR status (userId={})",
                currentUserService.currentUserId());
        service.deletePhotosFromDBWhenStatusISError();

        return ResponseEntity.accepted()
                .build();
    }
}
