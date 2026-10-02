package eu.mm.software.photofinder.photoparams.infrastructure;

import eu.mm.software.photofinder.common.security.PathAccessGuard;
import eu.mm.software.photofinder.photoparams.application.query.FileQuery;
import eu.mm.software.photofinder.photosattribute.infrastructure.mongo.PhotoAttributeSpringDataRepository;
import eu.mm.software.photofinder.user.application.query.UserQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
class FileQueryImpl implements FileQuery {

    private final PhotoAttributeSpringDataRepository repository;
    private final UserQuery userQuery;
    private final PathAccessGuard pathAccessGuard;

    @Override
    public Set<String> listAllFilesFromPath(String path, Set<String> extensions) {
        final Set<String> extensionUpperCase = extensions.parallelStream()
                .map(String::toUpperCase)
                .collect(Collectors.toSet());

        Path validatedPath = pathAccessGuard.resolveWithinAllowedRoots(path);

        try (var stream = Files.walk(validatedPath)) {
            return stream.parallel()
                    .filter(Files::isRegularFile)
                    .filter(it -> extensionUpperCase.contains(
                            FilenameUtils.getExtension(it.getFileName().toString().toUpperCase())))
                    .map(Path::toString)
                    .collect(Collectors.toSet());
        } catch (IOException e) {
            log.error("IO exception: {}", path, e);
            return new HashSet<>();
        }
    }

    @Override
    public Set<String> findAllByLoggedUser() {
        return repository.findByUserId(userQuery.findLoggedUserId()).parallelStream()
                .map(it -> it.getPath().endsWith("/") ?
                        it.getPath() + it.getFilename() : it.getPath() + "/" + it.getFilename())
                .collect(Collectors.toSet());
    }
}
