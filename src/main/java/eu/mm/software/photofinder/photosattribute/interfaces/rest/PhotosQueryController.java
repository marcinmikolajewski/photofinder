package eu.mm.software.photofinder.photosattribute.interfaces.rest;

import eu.mm.software.photofinder.common.FileUtils;
import eu.mm.software.photofinder.photosattribute.application.query.PhotosAttributesDto;
import eu.mm.software.photofinder.photosattribute.application.query.PhotosAttributesQuery;
import eu.mm.software.photofinder.photosattribute.domain.Status;
import eu.mm.software.photofinder.photosattribute.domain.AiProvider;
import eu.mm.software.photofinder.user.application.query.UserQuery;
import eu.mm.software.photofinder.user.domain.UserNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("rest/api/v1/photos")
@RequiredArgsConstructor
@Tag(
        name = "Photo Queries",
        description = "Endpoints for retrieving, describing, and filtering photo attributes"
)
public class PhotosQueryController {

    private final PhotosAttributesQuery photosAttributesQuery;
    private final UserQuery userQuery;

    @GetMapping(value = "duplicates", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Find duplicate photos",
            description = "Returns a list of IDs representing duplicate photo attributes stored in the database."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Duplicates successfully retrieved"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<List<String>> getDuplicateId() {

        String loggedUserId = Optional.ofNullable(userQuery.findLoggedUserId())
                .orElseThrow(UserNotFoundException::new);

        List<String> duplicatesPhotoAttribute = photosAttributesQuery.getDuplicatesPhotoAttribute(loggedUserId);

        return ResponseEntity.ok()
                .header("X-Total-Count", "" + duplicatesPhotoAttribute.size())
                .body(duplicatesPhotoAttribute);
    }

    @GetMapping(value = "files", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Find missing files",
            description = "Returns photo records that no longer exist on disk but are still present in the database."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Records successfully retrieved")
    })
    public ResponseEntity<List<PhotosAttributesDto>> getFileToDelete() {
        List<PhotosAttributesDto> filesToDelete = photosAttributesQuery.findByUser_Id().parallelStream()
                .filter(FileUtils::fileNotExist)
                .toList();

        return ResponseEntity.ok()
                .header("X-Total-Count", "" + filesToDelete.size())
                .body(filesToDelete);
    }

    @GetMapping(value = "describePhoto", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Describe a local photo",
            description = "Uses a selected provider to generate a description for a photo located on disk."
    )
    public ResponseEntity<String> describePhoto(
            @Parameter(description = "Absolute file path of the photo") @RequestParam("filepath") String filePath,
            @Parameter(description = "Name of the provider used for description") @RequestParam("provider") AiProvider provider) {

        String describe = photosAttributesQuery.describePhoto(filePath, provider);

        return ResponseEntity.ok(describe);
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Get all photo records",
            description = "Retrieves all photo attributes stored in the database."
    )
    public ResponseEntity<List<PhotosAttributesDto>> getAllFiles() {
        List<PhotosAttributesDto> photos = photosAttributesQuery.findByUser_Id();
        return ResponseEntity.ok()
                .header("X-Total-Count", "" + photos.size())
                .body(photos);
    }

    @GetMapping(value = "provider/{provider}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Filter photos by provider",
            description = "Retrieves all photo attributes associated with a specific provider."
    )
    public ResponseEntity<List<PhotosAttributesDto>> getPhotosByProvider(
            @Parameter(description = "Provider name") @PathVariable("provider") String provider) {
        List<PhotosAttributesDto> photos = photosAttributesQuery.findAllByProvider(provider);
        return ResponseEntity.ok()
                .header("X-Total-Count", "" + photos.size())
                .body(photos);
    }

    @GetMapping(value = "status/{status}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Filter photos by status",
            description = "Retrieves all photo attributes that have a specific status.")
    public ResponseEntity<List<PhotosAttributesDto>> getPhotosByStatus(
            @Parameter(description = "Photo status")
            @PathVariable("status") Status status) {
        List<PhotosAttributesDto> photos = photosAttributesQuery.findAllByStatus(status);
        return ResponseEntity.ok()
                .header("X-Total-Count", "" + photos.size())
                .body(photos);
    }

    @GetMapping(value = "status/count", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Count all photos",
            description = "Returns the total number of photo attributes stored in the database."
    )
    public ResponseEntity<Map<String, Long>> countPhotos() {
        Map<String, Long> count = photosAttributesQuery.count();
        return ResponseEntity.ok()
                .header("X-Total-Count", "" + count.values().stream()
                        .reduce(Long::sum)
                        .orElse(0L))
                .body(count);
    }
}
