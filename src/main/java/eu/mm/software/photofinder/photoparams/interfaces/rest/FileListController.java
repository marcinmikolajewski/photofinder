package eu.mm.software.photofinder.photoparams.interfaces.rest;

import eu.mm.software.photofinder.photoparams.application.query.FileQuery;
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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("rest/api/v1/file")
@RequiredArgsConstructor
@Tag(
    name = "File Management",
    description = "Endpoints for browsing and querying local and S3 files"
)
public class FileListController {

    private final FileQuery fileQuery;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "List files from local path",
        description = "Retrieves all files from the given path filtered by extensions. "
                    + "Optionally, only files not yet stored in the database can be returned."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "List of files successfully retrieved"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<Set<String>> getAllFiles(
            @Parameter(description = "Path on the local file system to scan") @RequestParam("path") String path,
            @Parameter(description = "List of file extensions to filter by") @RequestParam(value = "extensions") List<String> extensions,
            @Parameter(description = "If true, returns only new files not stored in DB") @RequestParam(value = "new", required = false) boolean onlyNew) {

        Set<String> extensionUpperCase = extensions.parallelStream()
                .map(String::toUpperCase)
                .collect(Collectors.toSet());

        Set<String> fileInDB;
        Set<String> newFile;

        if (onlyNew) {
            fileInDB = fileQuery.findAllByLoggedUser();
        } else {
            fileInDB = new HashSet<>();
        }

        Set<String> allFiles = fileQuery.listAllFilesFromPath(path, extensionUpperCase);

        if (!fileInDB.isEmpty()) {
            newFile = allFiles.parallelStream()
                    .filter(it -> !fileInDB.contains(it))
                    .collect(Collectors.toSet());
        } else {
            newFile = allFiles;
        }

        return ResponseEntity.ok()
                .header("X-Total-Count", "" + newFile.size())
                .body(newFile);
    }
}
