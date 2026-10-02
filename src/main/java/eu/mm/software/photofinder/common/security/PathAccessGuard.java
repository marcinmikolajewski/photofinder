package eu.mm.software.photofinder.common.security;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

/**
 * Centralny strażnik dostępu do systemu plików. Każda ścieżka pochodząca od klienta
 * (parametr "path"/"filepath" w REST API) musi przejść przez {@link #resolveWithinAllowedRoots}
 * zanim trafi do jakiegokolwiek {@code Files}/{@code File} API — inaczej dowolny uwierzytelniony
 * użytkownik może odczytać dowolny plik na serwerze (patrz plans/deep-analysis.md, etap 9.2).
 */
@Slf4j
@Component
public class PathAccessGuard {

    private final List<Path> allowedRoots;

    public PathAccessGuard(@Value("${photofinder.security.allowed-root-dirs:}") String allowedRootDirs) {
        this.allowedRoots = Arrays.stream(allowedRootDirs.split(","))
                .map(String::trim)
                .filter(StringUtils::hasLength)
                .map(it -> Paths.get(it).normalize())
                .toList();
    }

    @PostConstruct
    void logConfiguredRoots() {
        if (allowedRoots.isEmpty()) {
            log.error("photofinder.security.allowed-root-dirs is not configured — "
                    + "all file-path based operations will be rejected until it is set.");
        } else {
            log.info("Allowed root directories for file access: {}", allowedRoots);
        }
    }

    /**
     * Waliduje, że {@code rawPath} mieści się w jednym ze skonfigurowanych katalogów bazowych,
     * i zwraca jego kanoniczną (symlink-rozwiązaną) postać. Odrzuca ścieżkę zarówno gdy próba
     * ucieczki jest widoczna już w samym literale (np. {@code ../../etc}), jak i gdy dopiero
     * rozwiązanie dowiązań symbolicznych wyprowadza ją poza dozwolony katalog.
     *
     * @throws SecurityException gdy ścieżka jest pusta, nieprawidłowa, leży poza dozwolonymi
     *                            katalogami, albo katalogi bazowe w ogóle nie są skonfigurowane
     */
    public Path resolveWithinAllowedRoots(@Nullable String rawPath) {
        if (!StringUtils.hasText(rawPath)) {
            throw new SecurityException("Path must not be blank");
        }
        if (allowedRoots.isEmpty()) {
            throw new SecurityException("File access is disabled: no allowed root directories configured");
        }

        Path normalized;
        try {
            normalized = Paths.get(rawPath).normalize();
        } catch (InvalidPathException e) {
            throw new SecurityException("Invalid path: " + rawPath);
        }

        if (allowedRoots.stream().noneMatch(normalized::startsWith)) {
            log.warn("Rejected path outside allowed roots: {}", rawPath);
            throw new SecurityException("Path is outside of allowed directories");
        }

        // Drugi przebieg po rozwiązaniu dowiązań symbolicznych — broni przed linkiem
        // wewnątrz dozwolonego katalogu, który wskazuje poza niego.
        try {
            Path real = normalized.toRealPath();
            if (allowedRoots.stream().noneMatch(real::startsWith)) {
                log.warn("Rejected path resolving (via symlink) outside allowed roots: {}", rawPath);
                throw new SecurityException("Path is outside of allowed directories");
            }
            return real;
        } catch (IOException e) {
            // Ścieżka jeszcze nie istnieje albo jest nieosiągalna — sam normalized już
            // przeszedł walidację containment, więc zwracamy go bez rozwiązywania symlinków.
            return normalized;
        }
    }
}
